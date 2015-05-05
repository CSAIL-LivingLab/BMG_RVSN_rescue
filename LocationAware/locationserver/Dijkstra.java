package locationserver;

import java.util.*;
import java.awt.geom.*;

/**
* This class implements Dijkstra's shortest path graph search algorithm.
* Child classes overide the FoundDest function to account for Space, Building,
* or Graph.Node destination types.
*/

public abstract class Dijkstra {
    
    public abstract boolean FoundDest(Object head, Object end);
    
    public Collection getNeighbors(Vector shortestPath, Object head, Route r) {
	return Library.getSpacesExcludingWithWeights(shortestPath, (Space)head, r);
    }
    
    public double getWeight(Object head, Object nextObj,
			    Double nextWeight, Double pathLength) {
	    
	double centroidDist = ((Space)head).getCenter().distance(((Space)nextObj).getCenter());
	double weight = pathLength.doubleValue() + nextWeight.doubleValue() + centroidDist;
	return weight;
    }

    /**
     * Dijkstra's search from start to end, returning a vector of spaces
     * representing the shortest centroid-to-centroid path between the two.
     */
    public Vector findRouteBreadthFirst (PriorityQueue priorityQueue, 
					 Object end, Route r) {
	
	// continue to search paths until there are no more partial paths
	// to expand
	while (!priorityQueue.isEmpty()) {
	    Object[] nextObject = priorityQueue.removeShortestPath();
	    if(nextObject == null) break;
	    
	    Double shortestPathLength = (Double) nextObject[0];
	    Vector shortestPath = (Vector) nextObject[1];
	    Object head = shortestPath.lastElement();
      
	    // check to see if we're done with the search
	    if (FoundDest(head, end)) {
		return shortestPath;
	    }
	    
	    // see if there are any adjacencies to follow for this space
	    Collection nextObjects = getNeighbors(shortestPath, head, r);
	    if (nextObjects == null) {
		continue;
	    }
      
	    // iterate over adjacent spaces
	    for (Iterator i = nextObjects.iterator(); i.hasNext();) {
		Object[] item = (Object[]) i.next();
		Object nextObj = item[1];

		if (!shortestPath.contains(nextObj)) {
		    Double nextWeight = (Double) item[0];
		
		    // append the next Spaces onto the path.  Add the new paths to
		    // the priority queue by making a shallow copy of the shortest path.
		    Vector extendPath = (Vector) shortestPath.clone();
		    extendPath.add(nextObj);
		
		    // add the existing path length, the weight imposed by the portal,
		    // and the distance between the two adjacent nodes.		
		    double newWeight = getWeight(head, nextObj, shortestPathLength, nextWeight);
		    priorityQueue.add(newWeight, extendPath);
		}
	    } // end iterative path
	}
	
	// base case - we've run out of partial paths to expand
	System.out.println("no path found!");
	return null;
    }
}

/** Destination is of type Space. */
class DijkstraRoom extends Dijkstra {

    public DijkstraRoom() {}

    public boolean FoundDest(Object head, Object end) {
	// check to see if we're done with the search
	if (((Space)head).equals((Space)end)) {
	    return true;
	} else
	    return false;
    }
}

/** Destination is of type Building. */
class DijkstraBuilding extends Dijkstra {

    public DijkstraBuilding() {}

    public boolean FoundDest(Object head, Object end) {
	// check to see if we're done with the search

	// Terminates when the search finds a space that belongs
	// to the destination building.
	if (((Space)head).getBuildingName().equals((String)end)) {
	    return true;
	} else
	    return false;
    }
}

/** Destination is of type Graph.Node. Used for intra-space path finding. */
class DijkstraNode extends Dijkstra {

    public DijkstraNode() {}

    public boolean FoundDest(Object h, Object end) {
	// stopping condition: the head of the search path queue is
	// is a portal into the path destination
	Graph.Node head = (Graph.Node)h;

	if (head.isPortal() && 
	    (head.getPortal().spaces[0].equals(end) ||
	     head.getPortal().spaces[1].equals(end))) {
	    return true;
	} else
	    return false;
    }

    public Collection getNeighbors(Vector shortestPath, Object head, Route r) {
	return ((Graph.Node)head).getAdjNodesWithWeights();
    }

    public double getWeight(Object head, Object nextObj,
			    Double nextWeight, Double pathLength) {
	return pathLength.doubleValue() + nextWeight.doubleValue();
    }

}
