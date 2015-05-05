package locationserver;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * This class defines a Space, which represents a room or similar on
 * MIT's campus.  Spaces have a notion of a boundary, a name, and a
 * set of portals which connects this space to other spaces. Each
 * space maintains its triangulated state and the paths inside the
 * space between all pairs of portals. Spaces are defined by a
 * polyline of 3d points, but are assumed to be axis-aligned.
 */
public class Space implements Serializable {
  public int count = 0;
  private String name;           // unique space name
  private String buildingname;   // building name
  private String type;
  private Polyline boundaries;   // vector of points of boundary
  private double[] boundingBox;  // bounding box of the space
  private boolean convex;        // true iff convex
  private String tag;            // tag for the space
  private double height;         // height (z)

  // note that some member variables are marked transient so that
  // serialization doesn't cause memory loops
  private transient HashMap portalPaths; // set of Portal.Portal->Polyline
  private transient HashMap bestPortals; // set of Space.Space->portal[2]
  private transient Vector neighbors; // vector of Spaces
  private Graph graph; // stores paths connecting all portal pairs
  private Vector neighborNames; // vector of Strings
  private Vector adjPortals; // Portals connected to this space
  
  // cache a copy of the space's triangulation when instantiated
  private double[] triangles;      // double[] (convenience)
    //  private Vector triangles;      // double[] (convenience)

  // some static space types
  public static String TYPE_GRASS = "GRASS";
  public static String TYPE_CNSTR = "CNSTR";
  public static String TYPE_UKNWN = "UKNWN";
  public static String TYPE_SDWLK = "SDWLK";
  public static String TYPE_HGWAY = "HGWAY";
  public static String TYPE_BLDNG = "BLDNG";
  public static String TYPE_SLOPE = "SLOPE";

  private static final long serialVersionUID = 7021344565675999703L;

  /*
    get methods for the transient fields
  */
  public HashMap getPortalPaths() {
    return portalPaths;
  }

  public HashMap getBestPortals() {
    return bestPortals;
  }

  public Vector getNeighbors() {
    return neighbors;
  }

  /*
    set methods for the transient fields
  */
  public void setPortalPaths(HashMap hm) {
    this.portalPaths = hm;
  }

  public void setBestPortals(HashMap hm) {
    this.bestPortals = hm;
  }

  public void setNeighbors(Vector v) {
    this.neighbors = v;
  }




  /**
   * No-args constructor for serialized version
   */ 
  public Space() { }

  public Space(String name,
	       String type,
	       Polyline boundaries, 
	       double[] boundingBox,
	       double[] triangles,
	       //Vector triangles,
	       double height) {
    this.name          = name;
    this.type          = type;
    this.boundaries    = boundaries;
    this.boundingBox   = boundingBox;
    this.triangles     = triangles;
    this.adjPortals    = new Vector();
    this.portalPaths   = new HashMap();
    this.bestPortals   = new HashMap();
    this.neighbors = null; // must be explicity computed!
    this.height = height;
    this.graph  = null;

    // compute convexity
    this.convex = this.computeConvexity();
    this.buildingname = this.computeBuildingName();
  }
  
  /**
   * Return the triangulated representation as a vector of triangle objects
   */ 
  public double[] getTriangles() {
      //  public Vector getTriangles() {
    return this.triangles;
  }

  /**
   * Return the triangulated representation as a flat array of doubles
   */ 
  public double[] getTrianglesList() { 
      
      /* if storing triangle objects (see Triangle.java), convert
	 to flat array of doubles
      double[] answer = new double[this.triangles.size()*6];
      for(int i = 0; i < this.triangles.size(); i++) {
	  answer[i*6] = ((Triangle)this.triangles.get(i)).p1.x;
	  answer[i*6+1] = ((Triangle)this.triangles.get(i)).p1.y;
	  answer[i*6+2] = ((Triangle)this.triangles.get(i)).p2.x;
	  answer[i*6+3] = ((Triangle)this.triangles.get(i)).p2.y;
	  answer[i*6+4] = ((Triangle)this.triangles.get(i)).p3.x;
	  answer[i*6+5] = ((Triangle)this.triangles.get(i)).p3.y;
      }
      return answer; */
      return this.triangles;
  }

  /**
   * Return the globally unique name of the space
   */ 
  public String getName() {
    return this.name;
  }

  /**
   * Return the building name of this building.
   */ 
  public String getBuildingName() {
    return this.buildingname;
  }
  
  /**
   * Return the height of this space -- assumed to be uniform for the
   * polyline of the space's outline. This is implemented as the
   * height of the first point in the polyline.
   */ 
  public double getZ() {
    Position [] pts = this.boundaries.getPoints();
    return pts[0].z;
  }

  /**
   * Return a human-readable version of the space's name
   */
  public String getPrettyName() {
    int lastAt = this.name.lastIndexOf('#');
    String lastPart = "";
    try {
      String substring = this.name.substring(lastAt + 1);
      if (!substring.equals(""))
	lastPart = "-" + substring;
    }
    catch (IndexOutOfBoundsException e) {
      lastPart = ""; lastAt = this.name.length();
    }
    int nextToLastAt = this.name.lastIndexOf('#', lastAt - 1);
    String secondPart = this.name.substring(nextToLastAt + 1, lastAt);
    int firstAt = this.name.indexOf('#');
    
    //some names are still sort of broken, having only one '@' symbol.
    String firstPart;
    if (firstAt > nextToLastAt)
      firstPart = "";
    else
      firstPart = this.name.substring(firstAt + 1, nextToLastAt);
    return firstPart + secondPart + lastPart;
  }
  
  /**
   * Associates a portal with this space -- called only after the
   * Space is constructed, after the portal has been created also
   */ 
  public void addPortal(Portal p) {
    this.adjPortals.add(p);
  }

  /**
   * Return the floor number of the space. Buildings follow the
   * convention: BUILDINGNAME#FLOOR#ROOM#TYPE, so we tokenize.
   */ 
  public int getFloorNumber() {
    StringTokenizer st = new StringTokenizer(this.name, "#");
    st.nextToken();        // discard building name
    return Integer.parseInt(st.nextToken()); // convert to an int???
  }

  public String getFloorName() {
    StringTokenizer st = new StringTokenizer(this.name, "#");
    st.nextToken();        // discard building name
    return st.nextToken();
  }

  public String getRoomName() {
    StringTokenizer st = new StringTokenizer(this.name, "#");
    st.nextToken();        // discard building name
    st.nextToken();        // discard floor name
    return st.nextToken();
  }

  /**
   * Return the building name of this building. Buildings follow the
   * convention: BUILDINGNAME-ROOM, so we tokenize.
   */ 
  public String computeBuildingName() {
    StringTokenizer st = new StringTokenizer(this.name, "-");
    return st.nextToken();
  }

  /**
   * Return the type of the room (i.e. OFF, GRASS, etc)
   */
  public String getType() {
    return this.type; // type
  }
  

  /**
   * figure out if a certain point lies inside a room's polyline.
   * Ignore z values
   */
  public boolean isInside2D(Position position) {
      return this.inside(position, this.getBoundaries());
  }

  /** helper function for intersect and isInside() */
  private static int turn(Position v1, Position v2, Position v3) {
    if (v1.x * (v2.y - v3.y) + 
	v2.x * (v3.y - v1.y) + 
	v3.x * (v1.y - v2.y)
	> 0)
      return 1;
    else
      return -1;
  }

  /** detect the intersection of two edges.  Two parallel edges don't
      intersect.  edges sharing vertices don't intersect*/
  public static boolean intersect(Position a1, Position a2, 
				  Position b1, Position b2) {
    if (a1.equals(b1) || a1.equals(b2) || a2.equals(b1) || a2.equals(b2))
      return false;
    else if (a1.x - a2.x == 0 && b1.x - b2.x == 0)
      return false;
    else if (a1.y - a2.y == 0 && b1.y - b2.y == 0)
      return false;
    else if ((a1.y - a2.y)/(a1.x - a2.x) == (b1.y - b2.y)/(b1.x - b2.x))
      return false;
    else return (turn (a1, a2, b1) == -turn (a1, a2, b2) && 
		 turn(b1, b2, a1) == -turn (b1, b2, a2)); 
  }

  
  /**this is not robust.  you should check to make sure you don't 
     intersect with vertices _on_ the polyline. */
  public static boolean inside(Position vertex, Polyline wire) {
    /* find a point outside the wire */
    int numPoints = wire.numPoints();
    double maxX = -Double.MAX_VALUE;
    Position[] positionArray = wire.getPoints();
    for (int j = 0; j < numPoints; j++) {
      Position nextPosition = positionArray[j];
      if (nextPosition.x > maxX)
	maxX = nextPosition.x;
    }
    // it doesn't matter what the y coordinate is, since the X
    // coordinate is guaranteed outside the polyline
    Position outsidePosition = new Position(maxX, 0.0,0.0);
    
    // count intersecting room edges
    int intersectionCount = 0;
    for (int k = 0; k < wire.numPoints(); k++) {
	Position firstPos = positionArray[k];
	Position secondPos = positionArray[(k+1)%wire.numPoints()];
	if (intersect(vertex, outsidePosition, firstPos, secondPos))
	    intersectionCount++;
    }

    //even or odd?
    if ((intersectionCount%2) == 1)
      return true;
    else
      return false;
  }
  
  /**
   * Return the set of portal names that are associated with this Space
   */ 
  public Collection getPortals() {
    return this.adjPortals;
  }

  /**
   * Return the Polyline boundary of this space
   */ 
  public Polyline getBoundaries() {
    return this.boundaries;
  }

  /**
   * Representation of this space as a string
   */ 
  public String toString() {
    String returnString = "Space - Name = " + this.name;
    return returnString;
  }

  /**
   * returns min_x, min_y, max_x, max_y, respectively, of the polyline
   * defining the space's interior, where:
   *  double[0] = min_x
   *  double[1] = min_y
   *  double[2] = max_x
   *  double[3] = max_y
   */
  public double[] getBoundingBox() {
    return this.boundingBox;
  }

  /**
   * Compute the convexity of this space. This is computed by
   * iterating over all polylines and for any set of 3 points in the
   * polyline, computing the turn of the line segments A->B and
   * B->C. If the turn is positive for all points, the shape is
   * convex. The turn is computed using by finding the cross product
   * rule.
   */
  private boolean computeConvexity() {
    Position[] positions = this.boundaries.getPoints(); 
    for (int i = 2; i < positions.length; i++) {
      Position p1 = positions[i-2];
      Position p2 = positions[i-1];
      Position p3 = positions[i-0];
      double turn = 
	p1.x*p2.y + p3.x*p1.y + p2.x*p3.y - p1.x*p3.y - p2.x*p1.y - p3.x*p2.y;
      if (turn <= 0) {
	return false;
      }
    }
    return true;
  }
  
  public boolean getConvexity() {
    return this.convex;
  }
  
  public void setTag(String s) {
    this.tag = s;
  }

  public String getTag() {
    return this.tag;
  }

  public double minX() { double[] d = this.getBoundingBox(); return d[0]; }
  public double minY() { double[] d = this.getBoundingBox(); return d[1]; }
  public double maxX() { double[] d = this.getBoundingBox(); return d[2]; }
  public double maxY() { double[] d = this.getBoundingBox(); return d[3]; }

  /**
   * Method for determining the approximate center of a
   * space. Actually returns the centroid of the set of positions in
   * the space's boundary. This is MUCH more accurate than the center
   * of the space's bounding box.
   */ 
  public Position getCenter() {
    // iterate over the positions in this space, and find a common position
    Position[] pts = this.getBoundaries().getPoints();
    double x = 0; double y = 0; double z = 0; 
    for (int i = 0; i < pts.length; i++) {
      x = x + pts[i].x;
      y = y + pts[i].y;
      z = z + pts[i].z;
    }
    x = x / pts.length;
    y = y / pts.length;
    z = z / pts.length;
    Position center = new Position(x, y, z);
    return center;
  }

  /**
   * Method returns a portal[] of size two, which contain the portals
   * representing the shortest distance path between the two spaces
   * through this space. Returns null if either of the argument spaces
   * are not, in fact, connected to this space.
   */ 
  public Portal[] lookupBestPortals(Space s1, Space s2) {
    if (this.isNeighbor(s1) && this.isNeighbor(s2)) {
      // case: both spaces are adjacent!
      String lookup = s1.getName() + s2.getName();
      Portal[] portals = (Portal[]) this.bestPortals.get(lookup);
      return portals;
    } else {
      // case: one or both of the args are not adjacent!
      return null;
    }
  }

  /**
   * Method which initiates precomputation of best portal pairs for
   * each set of spaces adjacent to this space. This method should
   * only be called once, externally, by the JavaLocationServer.
   */ 
  public void computeBestPortalsForSpaces() {
    // first, build a collection of all of our spaces
    if (this.neighbors == null) {
      this.neighbors = new Vector();
      Iterator i = this.adjPortals.iterator();
      while (i.hasNext()) {
	Portal p = (Portal) i.next();
	//System.out.println(p);
	if (!this.neighbors.contains(p.spaces[0]) && 
	    !p.spaces[0].equals(this)) { 
	  this.neighbors.add(p.spaces[0]); 
	}
	if (!this.neighbors.contains(p.spaces[1]) && 
	    !p.spaces[1].equals(this)) { 
	  this.neighbors.add(p.spaces[1]); 
	}
      }
    }
    /* Note: in pnichols' version, weights from space S1 to S3 via S2
       was estimated as the shortest possible path through S2 (precomputed below).
       This has been changed to simply use centroid to centroid distances
       between adjacent spaces.

    // second, iterate over all combinations of spaces and find the
    // best set of portals! then we key the space[2] to the portal[2] 
    // we also key the portal[2] to the polyline path!
    for (int i = 0; i < this.neighbors.size(); i++) {
      for (int j = 0; j < this.neighbors.size(); j++) {
	if (i != j) {
	  Space s1 = (Space) this.neighbors.get(i);
	  Space s2 = (Space) this.neighbors.get(j);
	  Portal[] portals = Library.getBestPortals(s1, this, s2);
	  if (portals != null) {
	    String hash = s1.getName() + s2.getName();
	    this.bestPortals.put(hash, portals);
	    this.computePortalPath(portals[0], portals[1]);
	  }

	}
      }
      }
    */
    // finally, set up neighbornames
    this.neighborNames = new Vector();
    for (int i = 0; i < this.neighbors.size(); i++) {
      Space s = (Space) this.neighbors.get(i);
      this.neighborNames.add(s.getName());
    }
  }

  /**
   * Method returns a boolean expressing whether this space is a
   * neighbor of the named space.
   */ 
  public boolean isNeighbor(Space candidate) {
    if (this.neighbors == null) {
      // something bad has happened!
      return false;
    } else {
      return this.neighbors.contains(candidate);
    }
  }

  /**
   * Return the path between two of the portals in the space. Returns
   * null if one of the arguments is not present.
   */ 
  public Polyline getPathBetweenPortals(Portal start, Portal finish) {
    String lookup = start.name + finish.name;
    Polyline path = (Polyline) this.portalPaths.get(lookup);
    return path;
  }

  /**
   * Compute the path from one portal to another. Makes use of the
   * portalPaths data structure, which is a hashmap of {Start_Portal,
   * Hashmap}, where the nested Hashmap is {End_Portal, Path}, or
   * {Start_Portal, {End_Portal, Path}}. This data structure is
   * proportional in size to the number of pairs computed.
   */ 
  private void computePortalPath(Portal start, Portal finish) {
    // see if we have computed any pairs for this start portal 
    String lookup = start.name + finish.name;
    Polyline path = generatePortalPath(start, finish);
    this.portalPaths.put(lookup, path);
  }
  
  /**
   * Generate the path from one portal to another and return the
   * polyline representation of that set of iteratively shortened
   * points.
   */ 
  private Polyline generatePortalPath(Portal start, Portal finish) {
    Polyline path = new Polyline();
    if (this.convex) {
      path.addPoint(start.getApproximatePosition());
      path.addPoint(finish.getApproximatePosition());
    } else {
      Collection route = 
	Library.getShortestRoute(start.getApproximatePosition(), 
				 finish.getApproximatePosition(), this);

      Iterator i = route.iterator();
      while (i.hasNext()) {
	  path.addPoint((Position) i.next());
      }
    }
    return path;
  }

  public Vector getNeighborNames() {
    return this.neighborNames;
  }
  
  public double getHeight() {
    return this.height;
  }
  
  public boolean equals(Space s) {
    return this.name.equals(s.getName());
  }

  public void buildGraph() {
    this.graph = new Graph(this);
  }

  public Graph getGraph() {
    return this.graph;
  }

  static public String determineType(String attribute) {
      if (attribute.equals(TYPE_GRASS)) {
	  return TYPE_GRASS;
      } else if (attribute.equals(TYPE_CNSTR)) {
	  return TYPE_CNSTR;
      } else if (attribute.equals(TYPE_SDWLK)) {
	  return TYPE_SDWLK;
      } else if (attribute.equals(TYPE_HGWAY)) {
	  return TYPE_HGWAY;
      } else if (attribute.equals(TYPE_BLDNG)) {
	  return TYPE_BLDNG;
      } else if (attribute.equals(TYPE_SLOPE)) {
	  return TYPE_SLOPE;
      } else {
	  System.out.println("Unknown space type: " + attribute);
	  return TYPE_UKNWN;
      }
  }
}
