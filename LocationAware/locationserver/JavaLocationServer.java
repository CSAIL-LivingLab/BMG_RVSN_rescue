package locationserver;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;

// for reading xml data
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.*;


// ======================================================================

/**
 * Edited by Emily Whiting (april'06) to read xml data
 * This class is a simple LocationServer: it takes its information
 * from xml files containing space data and their corresponding portals.
 * It then serves the information with the LocationServer API.
 */
public class JavaLocationServer extends UnicastRemoteObject
  implements LocationServer, Serializable {
  
  // internal rep
  private HashMap roomHashMap;
  private HashMap portalHashMap;
  private HashMap routeCache; // cache full routes
  private HashMap buildingHashMap;

  // flag variables (mostly for perf testing)
  private boolean MEMOIZE    = true;
  private boolean NOISY      = false;
  private boolean PERFDATA   = true;
  private boolean PRECOMPUTE = true;

  /** since this class is serializable, we need an empty constructor */
  public JavaLocationServer() throws RemoteException {
    super();
  }
    
  /**  
   * Constructor takes a parameter, then a list of files.
   * if:
   * args[0] = QUICK_START
   *  then args[1] will be a serialized file to load of everything
   * if:
   * args[0] = BATCH_OUT
   *  then args[1] = file to output
   *  args[2 ... n] = buildings to load
   */
  public JavaLocationServer(String[] filenames) throws RemoteException {
    super();
    
    if (filenames[0].equals("QUICK_START")) {
      this.loadSerializedData(filenames[1]);
      System.out.println(this.portalHashMap.values().size());
    } else if (filenames[0].equals("BATCH_OUT")) {
      this.roomHashMap = new HashMap();
      this.portalHashMap = new HashMap();
      this.routeCache = new HashMap();
      this.buildingHashMap = new HashMap();
      // make sure we have a set of maps to load
      if (filenames == null) {
	throw new NullPointerException();
      }
      
      // READ SPACES & PORTALS: buildings contain one floor per file, basemap files
      // have 500 spaces per file. Portals are listed as elements of source space.
      System.out.println("*READING SPACES & PORTALS*");
      String directory = filenames[2];

      // READ FLOOR FILE NAMES FROM XML DOCUMENT
      Document document  = readDocument(directory + filenames[3]);
      NodeList buildings = document.getElementsByTagName("building");
      for (int i = 0; i < buildings.getLength(); i++) {
	  Element building = (Element)buildings.item(i);
	  String bldngname = building.getAttribute("id");
	  NodeList floors  = building.getElementsByTagName("floor");

	  Building bldngObj = new Building(bldngname);
	  buildingHashMap.put(bldngname, bldngObj);

	  for (int j = 0; j < floors.getLength(); j++) {
	      Element floor = (Element)floors.item(j);
	      String floorname = floor.getAttribute("id");
	      readSpaces(directory + bldngname + "-" + floorname + ".xml", bldngObj);
	  }
      }
      
      // CONNECT PORTALS TO SPACES
      System.out.println("*LINKING PORTALS TO SPACES*");
      linkPortals();
      
      // PRE-COMPUTE SPACES
      if (this.PRECOMPUTE) {
	System.out.println("*PRECOMPUTING SPACE DATA*");
	precomputeSpaces();
      }
      this.serializeOutput(filenames[1]);
    }
  }

  /**
   * returns the full, globally unique name of a room given the filename and
   * the room contained in the file. The current naming convention is: 
   * <Bldng>-<Floor><Room> (e.g. 1-078 for Bldng 1, Floor 0, Room 78)
  */
  private String getFullName(Element space, String filename) {
      String spaceName = space.getAttribute("name");
      String spaceType = space.getAttribute("type");
      
      // e.g. filename: inputDir/1-1.xml
      int indexStart = filename.lastIndexOf('/');
      int indexEnd   = filename.lastIndexOf(".xml");
      String bldg_floor = filename.substring(indexStart+1, indexEnd);

      int bmapIndex = bldg_floor.indexOf("BMAP");
      if(bmapIndex > -1)
	  return "BMAP-" + spaceName;
      else {
	  return /*bldg_floor + */spaceName;
      }
  }

  /**
   * Private method that takes in a file name, parses the file,
   * and then constructs the xml document.
   */
  private Document readDocument(String filename) {

      Document document = null;
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

      try {
	  DocumentBuilder builder = factory.newDocumentBuilder();
	  document = builder.parse( new File(filename) );
      } catch (SAXException sxe) {
	  // Error generated during parsing
	  Exception  x = sxe;
	  if (sxe.getException() != null)
	      x = sxe.getException();
	  x.printStackTrace();
      } catch (ParserConfigurationException pce) {
	  // Parser with specified options can't be built
	  pce.printStackTrace();
      } catch (IOException ioe) {
	  // I/O error
	  ioe.printStackTrace();
      }
      return document;
  }

  /**
   * Private method that takes in a file name for a set of spaces for
   * a given building, parses the file, and then constructs the Space
   * objects for each listed space. Roughly, spaces correspond to
   * rooms in a building. This method reads in a file name, constructs
   * the spaces, and updates an adjacency structure with the portals
   * associated with the space
   */ 
    private void readSpaces(String filename, Building building) {

      // get list of all spaces in this xml document
      Document document = readDocument(filename);
      if (document != null) {
	  NodeList spaces = document.getElementsByTagName("space");
    
	  for (int i = 0; i < spaces.getLength(); i++) {
	      Element space   = (Element)spaces.item(i);
	      Element contour = (Element)space.getElementsByTagName("contour").item(0);
	      NodeList points = contour.getElementsByTagName("point");
	      String roomName = getFullName(space, filename);
	      String roomType = Space.determineType(space.getAttribute("type"));

	      System.out.println(space.getAttribute("name"));
	      
	      /* set up the bounding box calculation */
	      double[] boundingBox = new double[4];
	      boundingBox[0] = Double.MAX_VALUE; // xmin
	      boundingBox[1] = Double.MAX_VALUE; // ymin
	      boundingBox[2] = Double.MIN_VALUE; // xmax
	      boundingBox[3] = Double.MIN_VALUE; // ymax
      
	      /* now, construct the Polyline */
	      Polyline polyline = new Polyline();
	      for (int j = 0; j < points.getLength(); j++) {
		  Element point = (Element)points.item(j);
	      
		  String xval = point.getAttribute("x");
		  String yval = point.getAttribute("y");
		  //String zval = point.getAttribute("z");
		  
		  double x = Double.valueOf(xval).doubleValue();
		  double y = Double.valueOf(yval).doubleValue();
		  double z = 0.0;//Double.valueOf(zval).doubleValue();
		  
		  /* sieve - figure out bounding box */
		  if (x < boundingBox[0]) { boundingBox[0] = x; }
		  if (y < boundingBox[1]) { boundingBox[1] = y; }
		  if (x > boundingBox[2]) { boundingBox[2] = x; }
		  if (y > boundingBox[3]) { boundingBox[3] = y; }
		  
		  /* finished - add point to polyline */
		  polyline.addPoint(new Position(x, y, z));
	      }
      
	      // Second, grab the triangles from the space element
	      // 2 pts (x,y) for each triangle vertex -- 6*N doubles (only in 2d for now)

	      NodeList triNodes = space.getElementsByTagName("triangle");
	      double[] triangles2D = new double[triNodes.getLength() * 6];
	      //Vector triangles2D = new Vector();

	      for (int j = 0; j < triNodes.getLength(); j++) {
		  Element triangle = (Element)triNodes.item(j);
		  Element[] trVerts = new Element[3];
		  //double[] coords = new double[6];

		  // integer value indicates index in list of contour points
		  trVerts[0] = (Element)points.item(Integer.parseInt(triangle.getAttribute("v0")));
		  trVerts[1] = (Element)points.item(Integer.parseInt(triangle.getAttribute("v1")));
		  trVerts[2] = (Element)points.item(Integer.parseInt(triangle.getAttribute("v2")));
		  /*
		  for(int k = 0; k < 3; k++) {
		      coords[2*k]   = Double.valueOf(trVerts[k].getAttribute("x")).doubleValue();
		      coords[2*k+1] = Double.valueOf(trVerts[k].getAttribute("y")).doubleValue();
		  }
		  Triangle nextTriangle = new Triangle(coords[0], coords[1],
						       coords[2], coords[3],
						       coords[4], coords[5], 0.0);
		  triangles2D.add(nextTriangle);
		  */
		  for (int k = 0; k < 3; k++) {
		      triangles2D[6*j + 2*k]   = Double.valueOf(trVerts[k].getAttribute("x")).doubleValue();
		      triangles2D[6*j + 2*k+1] = Double.valueOf(trVerts[k].getAttribute("y")).doubleValue();
		  }

	      }

	      // make the space
	      Space spaceObj = new Space(roomName, roomType, polyline, boundingBox, triangles2D, 12.0f);
	      roomHashMap.put(roomName, spaceObj);
	      readPortals(space, roomName);
	  
	  } /** end do rooms */
      }
  }
  

  /**
   * This method reads in all portals originating from the space element
   * and constructs the portals (roughly, a door or similar connector between
   * two spaces), giving each portal a reference to its named spaces.
   */ 
  private int readPortals(Element space, String spaceName0) {
      NodeList portals = space.getElementsByTagName("portal");
    
      for (int i = 0; i < portals.getLength(); i++) {
	  Element portal       = (Element)portals.item(i);
	  Polyline contour     = new Polyline();
 
	  // get the name of the spaces the portal connects
	  String spaceName1 = portal.getAttribute("target");
	  Space space0 = null;
	  if (spaceName0 != "0") { space0 = this.getSpaceByName(spaceName0); } // get space1 later

	  // get the portal type (e.g. stair, elevator, door)
	  String portalType = Portal.determineType(portal);
	      
	  // get the unique portal name
	  String portalName = portalType + Integer.toString(i);
	  portalName = portalName + spaceName0 + spaceName1;

	  // make sure that the portal actually connects two buildings!
	  if (space0 != null && spaceName1 != null) {

	      // PORTAL LOCATION -- campus coordinates
	      if(portal.getAttribute("class").equalsIgnoreCase("HORIZONTAL")) {
		  // get points on space contour:
		  // horizontal portals are incident to a single edge of the space contour
		  double t1, t2;
		  Element edge         = (Element)portal.getElementsByTagName("edge").item(0);
		  Element spacecontour = (Element)space.getElementsByTagName("contour").item(0);
		  NodeList points      = spacecontour.getElementsByTagName("point");

		  if(edge == null) {
		      System.out.println("Missing an edge: " + space.getAttribute("name") +
					 "->" + portal.getAttribute("target")); }
		  int edgeIndex = (Integer.parseInt(edge.getAttribute("index"))) %
		      (points.getLength()-1);
		  
		  if(edge.hasAttribute("minparam")) {
		      if(edge.getAttribute("minparam").equalsIgnoreCase("false"))
			  t1 = 0.0; // bug in xml output
		      else
			  t1 = Double.parseDouble(edge.getAttribute("minparam"));
		      if(edge.getAttribute("maxparam").equalsIgnoreCase("false"))
			  t2 = 0.0; // bug in xml output
		      else
			  t2 = Double.parseDouble(edge.getAttribute("maxparam"));
		  } else {
		      double tmid;
		      if(edge.getAttribute("param").equalsIgnoreCase("false"))
			  tmid = 0.0; // bug in xml output
		      else
			  tmid = Double.parseDouble(edge.getAttribute("param"));
		      t1 = tmid - 0.1;
		      t2 = tmid + 0.1;
		  }
		  Element p1 = (Element)points.item(edgeIndex);
		  Element p2 = (Element)points.item(edgeIndex+1);
	      
		  String xval, yval;
		  double x1, y1;
		  double x2, y2;
     
		  // 2D -- all spaces on same floor have same height
		  xval = p1.getAttribute("x"); x1 = Double.valueOf(xval).doubleValue();
		  yval = p1.getAttribute("y"); y1 = Double.valueOf(yval).doubleValue();
	      
		  xval = p2.getAttribute("x"); x2 = Double.valueOf(xval).doubleValue();
		  yval = p2.getAttribute("y"); y2 = Double.valueOf(yval).doubleValue();
	      
		  // end points determined by interpolating along incident contour edge
		  contour.addPoint(new Position(x1 + t1*(x2-x1), y1 + t1*(y2-y1), 0));
		  contour.addPoint(new Position(x1 + t2*(x2-x1), y1 + t2*(y2-y1), 0));
	      }
	      else if(portal.getAttribute("class").equalsIgnoreCase("VERTICAL")) {
		  // vertical portals have same contour as their source space
		  contour = space0.getBoundaries();
	      }
	      // instantiate our portal, and add it to an internal map
	      Portal nextPortal = new Portal(portalName, portalType, space0, spaceName1, contour);
	      portalHashMap.put(portalName, nextPortal);
	  } else {
	      System.out.println("** Portal " + portalName + " has bogus entries!");
	  }
	  
      }  /** end do portals */  
      return portals.getLength();
  } 

  /**
   * Private function to link portals to their spaces. Iterates through all
   * portals, initializes the destination space, and links the portal to its
   * source space.
   */
  private void linkPortals() {
      Iterator portals = this.portalHashMap.values().iterator();
      int portallink = 0;
      int reallyLinked = 0;
      while (portals.hasNext()) {
	  portallink++;
	  if (this.PERFDATA && portallink % 10000 == 0) {
	      System.out.println("  * "+portallink+" Portals Linked");
	  }
	  Portal p = (Portal) portals.next();
	  Space src  = p.spaces[0];
	  Space dest = null;

	  /* Check for dangling portals: lead outside building, identified with
	   * target="<Bldg>-<Floor>FLOORCONTOUR". These will be linked once all regular
	   * portals are initialized.
	   */
	  if(p.spacenames[1].indexOf("FLOORCONTOUR") < 0) {
	      dest = this.getSpaceByName(p.spacenames[1]);
	      p.setDestSpace(dest);
	  } else {
	      // do something
	      System.out.println("Dangling portal: " + p.spacenames[0] + " -> " + p.spacenames[1]);
	  }

	  // note that portals are not symmetric A->B ONLY
	  if (src != null && dest != null) {
	      src.addPortal(p);
	      reallyLinked++;

	      // check if transitional portal
	      if (p.type.equals(Portal.TYPE_TRANSITION)) {
		  String bname = src.getBuildingName();
		  Building b = this.getBuilding(bname);
		  b.addTransitionSpace(src);
	      }

	  } else {
	      System.out.println("Error linking: " + p.spacenames[0] + " " + p.spacenames[1]);
	  }
      }
      System.out.println("Total Portals: "+this.portalHashMap.values().size());
      System.out.println("Total Portals Linked: "+reallyLinked);
  }

  /**
   * Private method that iterates through all spaces stored in the roomhashmap.
   * For each space, precomputes the internal graph, and the portal pair giving the
   * shortest route through the space.
   */
  private void precomputeSpaces() {
      Iterator rooms = this.roomHashMap.values().iterator();
      int precomp = 0;
      System.out.println("JavaLocationServer::precomputeSpaces");
      while (rooms.hasNext()) {
	  precomp++;
	  if (this.PERFDATA && precomp % 100 == 0) {
	      System.out.println("  * "+precomp+" Spaces Precomputed");
	  }
	  Space s = (Space) rooms.next();
	  // build graph of space triangulation that connects all adjacent portals
	  System.out.println("Space: " + s.getName() + " -- build graph");
	  s.buildGraph();
	  s.computeBestPortalsForSpaces();
      }
  }

  public String getSpaceName(Location location) {
    return this.getSpace(location).getName();
  }

  /* use the polyline algorithm to figure out if a Location is inside
     a Space.
     if location == null, return any Space
  */
  
  public Space getSpace(Location location) {
    Iterator i = this.roomHashMap.values().iterator();
    for (;i.hasNext();) {
      Space nextSpace = (Space) i.next();
      if (location == null)
	return nextSpace;
      if (nextSpace.isInside2D(location.getPosition()))
	return nextSpace;
    }
    return null;
  }
  
  /**
   * Return a space given a location in 3d space, a building name, and
   * a floor
   */ 
  public Space getSpace(Location location, String buildingName,
			int floor) {
    Iterator i = this.roomHashMap.values().iterator();
    for (;i.hasNext();) {
      Space nextSpace = (Space) i.next();
      if (location == null)
	return nextSpace;
      if (nextSpace.isInside2D(location.getPosition()) &&
	  nextSpace.getBuildingName().equals(buildingName) &&
	  nextSpace.getFloorNumber() == floor)
	return nextSpace;
    }
    return null;
  }
  
  /**
   * getSpace returns the named space, if it is in the collection of
   * spaces held by this server. Is the named space is not held, the
   * best-guess for that space will be returned. If nothing for the
   * named building, floor, and room can be found, return null.
   */ 
  public Space getSpace(String name) {
      System.out.println("Requested space: " + name);
    Space s = (Space) this.roomHashMap.get(name);
    
    if (s == null)
	s = new Space(); // empty space
    System.out.println(s);
    return s;
  }

  public Building getBuilding(String name) {
      Building b = (Building) this.buildingHashMap.get(name);
      return b;
  }

  private Space getSpaceByName(String name) {
    if (this.roomHashMap.get(name) == null) {
      return null; 
    } else {
      return (Space) roomHashMap.get(name);
    }
  }

  public Portal getPortal(String portalName) {
    return (Portal) this.portalHashMap.get(portalName);
  }
  
  /**
   * Method which converts /path/to/file/BUILDING to just BUILDING
   * (i.e. /scratch/NE43 -> NE43).
   */ 
  private String getBuildingName(String filename) {
    File file = new File(filename);
    return file.getName();
  }
  
  private int getFloorNumber(String filename) {
    File file = new File(filename);
    String filenameString = file.getName();	
    int dotIndex = filenameString.indexOf('.');
    return Integer.parseInt(filenameString.substring(dotIndex + 1, filenameString.length()));
  }

  /**
   * This method takes a route object which contains a wrapped set of
   * constraints, a source, and a destination and generates both a
   * polyline path and a route, mutating the given route and returning
   * it.
   *
   * Edited by Emily Whiting (19.02.06) to search space graphs. Final route
   * polyline no longer uses best portal pairs. NOTE: temporarily took out
   * return of memoized route.
   *
   */
  public Route getRoute(Route request) {

    Vector spaces;
    String type = Route.ROUTE_PAVED;  // ignores client type request
    // String type = request.getType();
    // make a copy of the route to avoid memory cycles
    Route r = new Route(request.getSource(), request.getDest(), type);

    System.out.println("Calling getRoute() on: " + r);
    spaces = Library.findRouteDijkstra(request, this, r);

    // FIND EXACT NODE PATH THROUGH SPACES
    if (spaces != null) {
	System.err.println("Space sequence found: searching for node path");
	Vector nodes = Library.findNodesThroughSpaces((Space)spaces.firstElement(),
						      (Space)spaces.lastElement(),
						      r, spaces);
	if (nodes != null) {
	    r = this.setRoute(r, nodes);
	    this.routeCache.put(r.toString(), r);    // cache the route

	    // print spaces returned
	    System.out.println("Returning new route: " + r);
	    System.out.println("Spaces path: ");
	    for (int i = 0; i < spaces.size(); i++) {
		System.err.println("Space " + i + " = " + ((Space)spaces.get(i)).getName());
	    }
	    return r;
	} else {
	    System.err.println("Nodes null: exact route not found");
	}
    } else {
 	System.err.println("Spaces null: no route found");
    }
    return null;
  }

  /**
   * Populate route object with traversed path points, portal names, and
   * space names.
   */
  private Route setRoute(Route r, Vector nodes) {
      // we want to return a vector of space NAMES
      Vector names = new Vector();
      names.add(r.getSource());
	    
      // extract polyline and traversed portals from vector of graph nodes
      Polyline path = new Polyline();
      Vector portalNames = new Vector();
      Vector portals = new Vector();
      for (int i = 0; i < nodes.size(); i++) {
	  Graph.Node node = (Graph.Node)nodes.get(i);
	  if(node.isPortal()) {
	      portals.add(node.getPortal());
	      portalNames.add(node.getPortal().name);
	      names.add(node.getPortal().getDestName());
	      i++; // portals come in pairs
	  }
      }
      path = Library.shortenedPoly(portals);
      
      System.out.println("Returning route with " + path.numPoints() + " points");
      r.setRoute(names);
      r.setPath(path);
      r.setPortals(portalNames);
      return r;
  }

  /**
   * This method returns a vector of spaces that fit the constraints
   * of being in in a named building on a named floor.
   */ 
  public Vector getSpacesWithConstraints(String blg, String flr) {
    Vector returnVector = new Vector();
    Iterator rooms = this.roomHashMap.values().iterator();
    while (rooms.hasNext()) {
      Space s = (Space) rooms.next();
      if (s.getBuildingName().equals(blg) 
	  && s.getFloorNumber() == Integer.parseInt(flr)) {
	returnVector.add(s);
      }
    }
    return returnVector;
  }

  /**
   * Used for perf shizzle
   */ 
  public String getRandomSpace() {
    Object[] names = this.roomHashMap.keySet().toArray();
    int index = (int) (Math.random() * names.length);
    String name = (String) names[index];
    return name;
  }

  public String[] getSuperNodes() {
    String[] nodes = new String[200];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = this.getRandomSpace();
    }
    return nodes;
  }

  public Vector getSpacesThatClip(Rectangle r) {
    Vector v = new Vector();
    Iterator spaces = this.roomHashMap.values().iterator();
    while (spaces.hasNext()) {
      Space s = (Space) spaces.next();
      double[] sbox = s.getBoundingBox();
      Rectangle r1 = new Rectangle((int) sbox[0], (int) sbox[1],
				   (int) (sbox[2] - sbox[0]),
				   (int) (sbox[3] - sbox[1]));
      if (r.intersects(r1) && v.size() < 1000) {
	v.add(s);
      }
    }
    return v;
  }

  private void loadSerializedData(String filename) {
    this.roomHashMap = new HashMap();
    this.portalHashMap = new HashMap();
    this.routeCache = new HashMap();

    try {
      System.out.println("Loading from file!");
      FileInputStream fis = new FileInputStream(filename);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Object o = ois.readObject();
      while (o != null) {
	if (o instanceof Space) {
	  Space s = (Space) o;
	  s.setPortalPaths((HashMap) ois.readObject());
	  s.setBestPortals((HashMap) ois.readObject());
	  s.setNeighbors((Vector) ois.readObject());
	  this.roomHashMap.put(s.getName(), s);
	}
	if (o instanceof Portal) {
	  Portal p = (Portal) o;
	  p.spaces = (Space[]) ois.readObject();
	  this.portalHashMap.put(p.name, p);
	}
	o = ois.readObject();
      }
    } catch (Exception e) {
      e.printStackTrace();      
    }
  }

  private void serializeOutput(String filename) {
    try {
      System.out.println("Serializing to: " + filename);
      FileOutputStream fos = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      Space space;
      Portal portal;

      // first, write all spaces
      Iterator spaces = this.roomHashMap.values().iterator();
      while (spaces.hasNext()) {
	space = (Space) spaces.next();
	// write the space
	oos.writeObject(space);
	// write the space key
	// write the space's transient fields
	oos.writeObject(space.getPortalPaths());
	oos.writeObject(space.getBestPortals());
	oos.writeObject(space.getNeighbors());
      }

      Iterator portals = this.portalHashMap.values().iterator();
      while (portals.hasNext()) {
	portal = (Portal) portals.next();
	// write the portal
	oos.writeObject(portal);
	// write the portal's transient fields
	oos.writeObject(portal.spaces);
      }
      oos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
}
