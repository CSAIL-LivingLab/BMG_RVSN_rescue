/**
 * Note to the reader: this Java class was implemented by Jason Bell
 * in 2002, and substantially edited for cleanness and correctness by
 * Patrick Nichols in 2003. This file is not well-written or
 * well-documented. In fact, it is not well-implemented. I apologize
 * if you have to actually get your hands dirty and edit anything in
 * here. If anyone is to blame. it is the universe for forcing us into
 * such generally horrible life circumstances.
 *
 *  sic transit gloria mundi
 *  "thus does greatness leave the earth"
 *
 */ 
package locationserver;

import java.util.*;
import java.awt.geom.*;

public class Library {

  /**
   * Dijkstra's algorithm
   */ 
    public static Vector findRouteDijkstra(Route request, JavaLocationServer jls,
					   Route r) {
	System.out.println("Calling Djikstra");

	Vector spaces;
	Space s1 = null;
	Space s2 = null;
	PriorityQueue priorityQueue = new PriorityQueue();

	// INITIALIZE SOURCE
	if (request.getSource().indexOf("-") == -1) {
	    
	    // source is a building -- first piece of the path
	    // can be any space that leads out of the building
	    System.out.println("Building source");
	    Building srcBldng = jls.getBuilding(request.getSource());
	    Vector srcSpaces  = srcBldng.getTransitionSpaces();
	    for (int i = 0; i < srcSpaces.size(); i++) {
		Vector firstRoute = new Vector();
		firstRoute.add((Space)srcSpaces.get(i));
		priorityQueue.add(0.0f, firstRoute);
	    }
	    //System.out.println("building routes invalid"); return null;
	} else {
	    // otherwise source is a single room
	    s1 = jls.getSpace(request.getSource());
	    Vector firstRoute = new Vector();
	    firstRoute.add(s1);
	    priorityQueue.add(0.0f, firstRoute);
	}

	// INITIALIZE DESTINATION
	if (request.getDest().indexOf("-") == -1) {
	    // destination is a building
	    System.out.println("Building destination");

	    Dijkstra test = new DijkstraBuilding();
	    spaces = test.findRouteBreadthFirst(priorityQueue, request.getDest(), r);
	} else {
	    // otherwise destination is a single room
	    s2 = jls.getSpace(request.getDest());
	    Dijkstra test = new DijkstraRoom();
	    spaces = test.findRouteBreadthFirst(priorityQueue, s2, r);
	}
	return spaces;
    }
  

  /**
   * Implemented by Emily Whiting (Feb.20.06)
   * It is assumed the vector contains a sequence of spaces connnecting the source
   * and destination of the requested route (returned from findRouteBreadthFirst).
   *
   * This function combines graphs from all individual spaces in the spaces vector.
   * Each graph is simply the link-node representation of each space's triangulation,
   * where triangles are nodes, and links represent adjacent triangles.
   * Graphs are linked by adding edges between corresponding portal nodes.
   */
  private static Graph combineGraphs(Vector spaces) {

      Graph routeGraph = new Graph((Space)spaces.get(0));
	
      // build graph from input spaces only
      for (int i = 1; i < spaces.size(); i++) {
	  Space s = (Space) spaces.get(i);
	  Graph temp = new Graph(s);
	  routeGraph.add(temp,s);
      }
      return routeGraph;
  }
  
  /**
   * findNodesThroughSpaces finds the exact node path (triangle to triangle)
   * from the start space to end space. It returns a vector of positions
   * representing the BFS path.
   */
  public static Vector findNodesThroughSpaces (Space start, Space end, 
					       Route r, Vector spaces) {

      Graph routeGraph = combineGraphs(spaces);
      System.out.println("Space graphs combined: running Dijkstra search");
      PriorityQueue priorityQueue = new PriorityQueue();
      Vector returnVector = null;

      // find possible portals connected to start space for the route origin
      Vector portals = (Vector)start.getPortals();
      for (int i = 0; i < portals.size(); i++) {
	  Graph.Node StartNode = routeGraph.getPortalNode((Portal)portals.get(i));
	  if(StartNode != null) {
	      Vector firstRoute = new Vector();
	      firstRoute.add(StartNode);
	      priorityQueue.add(0.0f, firstRoute);
	  }
      }
      Dijkstra test = new DijkstraNode();
      return test.findRouteBreadthFirst(priorityQueue, end, r);
  }
  
  /** 
   * given three spaces that are members of a path, returns the start
   * and finish portals that minimize the total distance through the
   * space.  order prevPortals*nextPortals. If no such pair of portals
   * can be found, returns null.
  */
  public static Portal[] getBestPortals(Space prevSpace, 
					Space thisSpace, 
					Space nextSpace) {
    Collection portalsPrev = getPortalsConnecting(prevSpace, thisSpace);
    Collection portalsNext = getPortalsConnecting(thisSpace, nextSpace);
    Portal startPortal = null;
    Portal finishPortal = null;
    
    double minLength = Double.MAX_VALUE;
    for (Iterator j = portalsPrev.iterator(); j.hasNext();) {
      Portal prevPortal = (Portal) j.next();
      for(Iterator k = portalsNext.iterator(); k.hasNext();) {
	Portal nextPortal = (Portal) k.next();
	Collection route = 
	    getShortestRoute(prevPortal.getApproximatePosition(),
			     nextPortal.getApproximatePosition(),
			     thisSpace);

	/* Removed by Emily Whiting (02.16.06):
	 * Forego iterative shortening. Path returned from Library::getShortestRoute()
	 * approximates medial axis. Shortening doesn't improve 'natural' appearance.
	 *
	Collection shortenedRoute = iterativelyShorten(route, thisSpace);
	double nextLength = getRouteLength(shortenedRoute);
	*/
	double nextLength = getRouteLength(route);
	if (nextLength < minLength) {
	  minLength = nextLength;
	  startPortal = prevPortal;
	  finishPortal = nextPortal;
	}
      }
    }
    if (startPortal == null || finishPortal == null) {
      return null;
    } else {
      Portal[] returnArray = new Portal[2];
      returnArray[0] = startPortal;
      returnArray[1] = finishPortal;
      return returnArray;
    }
  }

  /** 
   * Implemented by Emily Whiting (Feb 24, 2006)
   * given three spaces that are members of a path, returns the average
   * distance through the space. If no pair of portals can be found to
   * connect the three spaces, returns null.
  */
  public static double getAvgRouteLength(Space prevSpace, 
					 Space thisSpace, 
					 Space nextSpace) {
    
    Collection portalsPrev = getPortalsConnecting(prevSpace, thisSpace);
    Collection portalsNext = getPortalsConnecting(thisSpace, nextSpace);
    
    double sumLength = 0.0;
    int numPairs = 0;
    for (Iterator j = portalsPrev.iterator(); j.hasNext();) {
	Portal prevPortal = (Portal) j.next();
	for(Iterator k = portalsNext.iterator(); k.hasNext();) {
	    Portal nextPortal = (Portal) k.next();
	    Collection route = 
		getShortestRoute(prevPortal.getApproximatePosition(),
				 nextPortal.getApproximatePosition(),
				 thisSpace);
	    
	    sumLength += getRouteLength(route);
	    numPairs++;
	}
    }
    if (numPairs == 0) {
	return Double.MAX_VALUE;
    } else {
	return (sumLength/(double)numPairs);
    }
  }
  
  /** 
   * returns a collection of positions representing the shortest route
   * from Portal start and Portal finish through Space space.
   * Positions are guaranteed to form a series of line segments that
   * don't cross space boundaries.
   *
   * Heavily edited by Emily Whiting (18.02.06) to search a graph
   * representation of the space (see Graph.java).
  */
  public static Collection getShortestRoute(Position start, 
					    Position finish, 
					    Space space) {

    // figure out which node to start with
    Position startPosition = start;
    Position finishPosition = finish;

    Graph spacegraph = space.getGraph();
    Graph.Node StartNode = spacegraph.getPortalNode(start);
    Graph.Node FinishNode = spacegraph.getPortalNode(finish);
    if(StartNode == null) System.err.println("Start Node not found");
    if(FinishNode == null) System.err.println("Finish Node not found");

    // now, do djikstra's algo to find shortest route
    PriorityQueue priorityQueue = new PriorityQueue();
    Vector firstRoute = new Vector();
    firstRoute.add(StartNode);
    priorityQueue.add(0.0f, firstRoute);
    Vector returnVector = getShortestNodePath(priorityQueue, FinishNode);

    // We have a list of Graph nodes. Now extract a list of positions.
    Vector returnVal = new Vector();
    if (returnVector == null) {
	returnVal.add(startPosition);
	returnVal.add(finishPosition);
	return returnVal;
    } else {
	for (Iterator i = returnVector.iterator(); i.hasNext();) {
	    returnVal.add(((Graph.Node)i.next()).pos());  //add node position
	}
    }
    return returnVal;
  }

  // priorityQueue initialized with possible start nodes for path
  public static Vector getShortestNodePath(PriorityQueue priorityQueue, Graph.Node end) {

      Vector returnVector = null;

      while (!priorityQueue.isEmpty()) {
	  Object[] nextObject = priorityQueue.removeShortestPath();
	  if(nextObject == null) break;

	  Double shortestPathLength = (Double) nextObject[0];
	  Vector shortestPath = (Vector) nextObject[1];
	  Graph.Node head = (Graph.Node)shortestPath.lastElement();

	  // stopping condition: the head of the search path queue is
	  // close to the triangle nearest the path destination
	  if (head.equals(end)) {
	      returnVector = shortestPath;
	      break;
	  }

	  // find the triangles adjacent to this one
	  Vector nextNodes = head.getAdjNodesWithWeights();

	  for (int i = 0; i < nextNodes.size(); i++) {
	      Object[] nextNodeObj = (Object[])nextNodes.get(i);
	      Graph.Node nextNode = (Graph.Node)nextNodeObj[1];

	      if (!shortestPath.contains(nextNode)) {
		  Double nextWeight = (Double)nextNodeObj[0];
		  Vector extendPath = (Vector) shortestPath.clone();
		  extendPath.add(nextNode);
		  double newWeight = shortestPathLength.doubleValue() + nextWeight.doubleValue();
		  priorityQueue.add(newWeight, extendPath);
	      }
	  }
      }  return returnVector;
  }
  /**
   * Method that returns the length of route.  Input collection is
   * expected to be a collection of Positions that specify a route
   * step by step
  */
  public static double getRouteLength(Collection route) {
    double returnLength = 0.0f;
    Iterator i = route.iterator();
    Position prevPosition = (Position) i.next();
    Position nextPosition = null;
    for (;i.hasNext();) {
      nextPosition = prevPosition;
      nextPosition = (Position) i.next();
      returnLength += 
	Math.sqrt(((nextPosition.x - prevPosition.x)*
		   (nextPosition.x - prevPosition.x)) + 
		  ((nextPosition.y - prevPosition.y)*
		   (nextPosition.y - prevPosition.y)) +
		  ((nextPosition.z - prevPosition.z)*
		   (nextPosition.z - prevPosition.z)));
    }
    return returnLength;
  }
  
  public static Polyline shortenedPoly(Vector portals) {
      Polyline answer = new Polyline();
      for(int i = 0; i < portals.size()-1; i++) {
	  Space currSpace = ((Portal)portals.get(i)).spaces[1];

	  Vector initpositions =
	      (Vector)getShortestRoute(((Portal)portals.get(i)).getApproximatePosition(),
				       ((Portal)portals.get(i+1)).getApproximatePosition(),
				       currSpace);
	      
	  Collection positions = iterativelyShorten(initpositions, currSpace);
	  for(Iterator j = positions.iterator(); j.hasNext();) {
	      answer.addPoint((Position)j.next());
	  }
      } return answer;
  }

  /* Implemented by Emily Whiting (March 5, 2006)
   */
  public static Collection iterativelyShorten(Vector positions, Space space) {
      Vector returnVector = new Vector();

      Polyline pline = space.getBoundaries();
      Position[] plinePoints = pline.getPoints();
    
      Position prevP = (Position)positions.get(0);
      Position anchor = prevP;  //the anchor is the last included point.
      returnVector.add(prevP);  //the first Position is automatically included

      for (int i = 1; i < positions.size(); i++) {
	  Position nextP = (Position) positions.get(i);
	  if(i == 1) {
	      // adds epsilon shift to avoid self intersection
	      anchor = new Position(prevP.x + 0.1*(nextP.x - prevP.x),
				    prevP.y + 0.1*(nextP.y - prevP.y), prevP.z);
	  }
	  else if(i == positions.size()-1) {
	      nextP = new Position(prevP.x + 0.9*(nextP.x - prevP.x),
				   prevP.y + 0.9*(nextP.y - prevP.y), prevP.z);
	  }
	  //now, we check intersection of the new line segment with every
	  //space polyline segment
	  Position boundaryP1 = null;
	  Position boundaryP2 = plinePoints[0];
	  boolean doesIntersect = false;
	  for (int j = 1; j < plinePoints.length; j++) {
	      boundaryP1 = boundaryP2;
	      boundaryP2 = plinePoints[j];

	      if(Line2D.linesIntersect(anchor.x, anchor.y, nextP.x, nextP.y,
				       boundaryP1.x, boundaryP1.y, boundaryP2.x, boundaryP2.y)) {
		  //prevP is the latest Position that _doesn't_ intersect
		  returnVector.add(prevP);
		  anchor = prevP;  //update the anchor
		  break;
	      }
	  }
	  //else, skip prevP - it doesn't get included
	  prevP = nextP; //...and continue
      }
      returnVector.add(positions.lastElement());
      return returnVector;
  }

  private static boolean linesegIntersect(Position p1, Position p2,
					  Position q1, Position q2) {       
      // test if lines intersect
      double intersectX, intersectY;

      Position Pdir = Position.normalize(Position.diff(p1,p2));
      Position Qdir = Position.normalize(Position.diff(q1,q2));

      Position P1 = p1;
      Position Q1 = q1;
      Position P2 = new Position(P1.x + Pdir.x, P1.y + Pdir.y, P1.z);
      Position Q2 = new Position(Q1.x + Qdir.x, Q1.y + Qdir.y, Q1.z);
	  
      double M = ((Q1.x-P1.x)*(Q1.y-Q2.y) - (Q1.x-Q2.x)*(Q1.y-P1.y));
      double D = ((P2.x-P1.x)*(Q1.y-Q2.y) - (Q1.x-Q2.x)*(P2.y-P1.y));

      if(Math.abs(D) < 0.0001) return false; // parallel;
      else {
	  intersectX = P1.x + Pdir.x*M/D;
	  intersectY = P1.y + Pdir.y*M/D;
	    
	  // now see if intersection point is on line segments
	  if(onLineSeg(p1, p2, intersectX, intersectY) &&
	     onLineSeg(q1, q2, intersectX, intersectY))
	      return true;
      }
      return false;
  }

  private static boolean onLineSeg(Position p1, Position p2,
				   double intersectX, double intersectY) {
      if(((p1.x - intersectX)*(intersectX - p2.x) >= 0) &&
	 ((p1.y - intersectY)*(intersectY - p2.y) >= 0))
	  return true;
      else return false;
  }
    
  /** 
   * returns a Collection of Object[2]'s, where Object[0] is a Double,
   * and Object[1] is a Space.  Object[0] is the weight imposed by a
   * Portal and Object[1] is the Space adjacent to the argument
   * 'space'. Constraints are implemented using the route r, by a call
   * to the isLegal() function! If no legal portals are found, returns
   * null.
  */
  public static Collection getSpacesExcludingWithWeights(Collection spaces, 
							 Space space, 
							 Route r) {
    Vector returnVector = new Vector();
    
    Collection portals = space.getPortals();
    for (Iterator i = portals.iterator(); i.hasNext();) {
      Object[] addArray = new Object[2];
      Portal nextPortal = (Portal) i.next();
      
      // first, see if this portal is valid for our route!
      if (r.isLegal(nextPortal)) {
    
	// check if the portal's spaces are excluded before adding them
	// in be careful not to add nulls
	if (!spaces.contains(nextPortal.spaces[0]) &&
	    (nextPortal.spaces[0] != null)) {
	  addArray[0] = new Double(nextPortal.getWeight(r));
	  addArray[1] = nextPortal.spaces[0];
	  returnVector.add(addArray);
	}
	if (!spaces.contains(nextPortal.spaces[1]) &&
	    (nextPortal.spaces[1] != null)) {
	  addArray[0] = new Double(nextPortal.getWeight(r));
	  addArray[1] = nextPortal.spaces[1];
	  returnVector.add(addArray);
	}
      }
    }
    
    if (returnVector.size() > 0) {
      return returnVector;
    } else {
      return null;
    }
  }

  /** 
   * returns a Collection of Spaces adjacent to the argument 'space'.
   * Constraints are implemented using the route r, by a call
   * to the isLegal() function. If no legal portals are found, returns
   * an empty vector.
  */
  public static Collection getSpacesExcluding(Collection spaces, 
					      Space space, Route r) {
    Vector returnVector = new Vector();
    Collection portals  = space.getPortals();

    for (Iterator i = portals.iterator(); i.hasNext();) {
	Portal nextPortal = (Portal) i.next();
	
	// first, see if this portal is valid for our route!
	if (r.isLegal(nextPortal)) {
    
	    // check if the portal's spaces are excluded before adding them
	    // in be careful not to add nulls
	    if (!spaces.contains(nextPortal.spaces[0]) &&
		(nextPortal.spaces[0] != null)) {
		returnVector.add(nextPortal.spaces[0]);
	    }
	    if (!spaces.contains(nextPortal.spaces[1]) &&
		(nextPortal.spaces[1] != null)) {
		returnVector.add(nextPortal.spaces[1]);
	    }
	}
    } return returnVector;
  }
  
  /**
   * Nulled out version of getSpacesExcludingWithWeights, for use
   * where no route is generally present.
   */ 
  public static Collection getSpacesExcluding(Collection spaces, 
					      Space space) {
    Route dummy = new Route(null, null, Route.ROUTE_ALWAYS_LEGAL);
    Collection returnVector = new Vector();
    Collection tmp = getSpacesExcludingWithWeights(spaces, space, dummy);
    if (tmp == null) { return null; }
    Iterator i = tmp.iterator();
    while(i.hasNext()) {
      Object[] o = (Object[]) i.next();
      returnVector.add(o[1]);
    }
    return returnVector;
  }


  /** returns null if no direct connection */
  public static Collection getPortalsConnecting(Space space1, Space space2) {
    if (space1 == null || space2 == null)
      return null;
    Iterator i = space1.getPortals().iterator();
    Vector returnPortals = new Vector();
    while(i.hasNext()) {
      Portal nextPortal = (Portal) i.next();
      if (nextPortal.spaces[0] == space1 &&
	  nextPortal.spaces[1] == space2) {
	returnPortals.add(nextPortal);
      }
    }
    return returnPortals; 
  }
  
  /** is a Portal vertical ? */
  // pnichols asks: what the hell does this method actually do?
  public static boolean isVert(Portal portal) {
    return (portal.type.equals(Portal.TYPE_ELEV_DOWN)  ||
	    portal.type.equals(Portal.TYPE_ELEV_UP)    ||
	    portal.type.equals(Portal.TYPE_STAIRS_DOWN) ||
	    portal.type.equals(Portal.TYPE_STAIRS_UP)   );
  }
}

/** 
 * helper class - Priority Queue.  Should be general enough to use
 * with any object type.  always returns (and deletes) the shortest
 * path in the queue.  Insertions only succeed if they are the
 * shortest path with a particular head.
 */

class PriorityQueue {
  private SortedMap sortedMap;

  /* keyed on Object, members are Object[2], where Object[0] is
   * Double, Object[1] is Vector */
    private Vector heads;

  public PriorityQueue() {
    this.sortedMap = new TreeMap();
    //this.heads = new HashMap();
    this.heads = new Vector();
  }

  /**
   * Try to add a new path to the priority queue, with specified
   * length and path. Path is a vector, with the head (end) of the
   * path as the last element of the vector.
   */
  public void add(double length, Vector path) {
    // first, check to see if the new path is shorter than the
    // shortest path with the same head
    //
    // NOTE: "heads" stores all graph nodes that have their shortest
    // path determined.
    Object head = path.lastElement();
    if (!this.heads.contains(head)) {
    
	// put the new path in the queue.
	Double doubleKey = new Double(length);
	if (sortedMap.containsKey(doubleKey)) {
	    Vector getVector = (Vector) sortedMap.get(doubleKey);
	    getVector.add(path);
	}
	else {
	    Vector newVector = new Vector();
	    newVector.add(path);
	    sortedMap.put(doubleKey, newVector);
	}
    }
  }
  
  /** 
   * removes one of the lowest paths in the Queue.  The first element
   * of the returned Object[2] is a Double representing length, the
   * second element is a Vector of the Path
  */
  public Object[] removeShortestPath() {

    Vector returnPath = null;
    Object[] returnObject = new Object[2];

    do {
	if (sortedMap.isEmpty()) {
	    return null;
	}
	Double lowestLength = (Double) sortedMap.firstKey();
	Vector lowestPaths = (Vector) sortedMap.remove(lowestLength);
	//just get one of the lowest paths:
	returnPath = (Vector) lowestPaths.remove(0);
	if (!lowestPaths.isEmpty()) {
	    //put it back into the map, minus the path we're removing
	    sortedMap.put(lowestLength, lowestPaths);
	}
	returnObject[0] = lowestLength;
	returnObject[1] = returnPath;
    } while(this.heads.contains(returnPath.lastElement()));

    // put this shortest path in the heads list
    Object head = returnPath.lastElement();
    this.heads.add(head);

    return returnObject;
  }
  public boolean isEmpty() {
    return this.sortedMap.isEmpty();
  }
  public int size() {
    return this.sortedMap.size();
  }
}
