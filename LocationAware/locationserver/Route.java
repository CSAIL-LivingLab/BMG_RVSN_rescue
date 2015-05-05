package locationserver;

import java.util.*;
import java.io.*;

/**
 * A Route tries to encapsulate a constrainted path between two
 * locations using the Space and Portal representation used in the
 * Location Server. Routes contain many different layers of meaning --
 * sets of constraints on a path, the set of spaces a path traverses,
 * and the actual set of world space positions the path traverses.
 */
public class Route implements Serializable {

  // note to the wary: passing spaces around in route objects semse
  // to create pointer cyles, out of memory errors, an a general
  // collapse of the fabric of space-time.
  private String source;
  private String dest;
  private String type;
  private Polyline path = null; // polyline of positions in world space
  private Vector route  = null; // vector of space names
  private Vector portals = null; // vector of portals on route
  
  // static strings to represent routes
  public static String ROUTE_ROLLING = "ROUTE_ROLLING";
  public static String ROUTE_WALKING = "ROUTE_WALKING";
  public static String ROUTE_PAVED   = "ROUTE_PAVED"; // no grass or construction
  public static String ROUTE_AVOID_INDOOR  = "ROUTE_AVOID_INDOOR";
  public static String ROUTE_AVOID_OUTDOOR = "ROUTE_AVOID_OUTDOOR";
  public static String ROUTE_ALWAYS_LEGAL  = "ROUTE_ALWAYS_LEGAL";
  
  public Route(String source, String dest, String type) {
    this.source = source;
    this.dest = dest;
    this.type = type;
  }
  
  public String getSource() {
    return this.source;
  }

  public String getDest() {
    return this.dest;
  }
  
  public Vector getRoute() {
    return this.route;
  }
  
  public Polyline getPath() {
    return this.path;
  }
  
  public String getType() {
    return this.type;
  }

  public Vector getPortals() {
    return this.portals;
  }

  public void setRoute(Vector v) {
    this.route = v;
  }
  
  public void setPath(Polyline p) {
    this.path = p;
  }

  public void setPortals(Vector v) {
    this.portals = v;
  }

  public boolean equals(Route r) {
    return (r.getSource().equals(this.source) &&
	    r.getDest().equals(this.dest) &&
	    r.getType().equals(this.type));
  }

  public String toString() {
    return this.source + " ->" + this.dest + " | " + this.type;
  }
 
  
  /**
   * Is the named portal legal in this route? This method is used to
   * generate constrained routes, i.e. by not allowing stairs in
   * rolling routes. This method does NOT express route preferences
   * (i.e. keep me inside as much as possible).
   */ 
  public boolean isLegal(Portal p) {
      // ALWAYS LEGAL type -- handy for graph building
      if (this.type.equals(Route.ROUTE_ALWAYS_LEGAL)) {
	  return true;
      }

      if (p.spaces[1].getBuildingName().equals("BMAP") &&
	  p.spaces[1].getType().equals(Space.TYPE_BLDNG)) {
	  return false;
      }

      // ROLLING ROUTES -> can't use stairs
      if (this.type.equals(Route.ROUTE_ROLLING)) {
	  // check for interior verticale adjacencies
	  if (p.type.equals(Portal.TYPE_STAIRS_UP) ||
	      p.type.equals(Portal.TYPE_STAIRS_DOWN)) {
	      return false;
	  } else {
	      // check basemap 
	      if (p.type.equals(Portal.TYPE_OUTDOOR)) {
		  // can't pass through grass, construction, etc
		  if (p.spaces[1].getBuildingName().equals("BMAP") &&
		      (p.spaces[1].getType().equals(Space.TYPE_GRASS) ||
		       p.spaces[1].getType().equals(Space.TYPE_CNSTR) || 
		       p.spaces[1].getType().equals(Space.TYPE_UKNWN))) {
		      return false;
		  }
	      }
	      return true;
	  }
      }
      // WALKING ROUTES -> everything is OK
      if (this.type.equals(Route.ROUTE_WALKING)) {
	  if (p.type.equals(Portal.TYPE_OUTDOOR)) {
	      // can't pass through construction, etc
	      if (p.spaces[1].getBuildingName().equals("BMAP") &&
		  (p.spaces[1].getType().equals(Space.TYPE_CNSTR) || 
		   p.spaces[1].getType().equals(Space.TYPE_UKNWN))) {
		  return false;
	      }
	  }
	  return true;
      }
      // PAVED ROUTE -> when outside same as rolling, when inside same as walking
      if (this.type.equals(Route.ROUTE_PAVED)) {
	  // check basemap 
	  if (p.type.equals(Portal.TYPE_OUTDOOR) ||
	      p.type.equals(Portal.TYPE_TRANSITION)) {
	      // can't pass through grass, construction, etc
	      if (p.spaces[1].getBuildingName().equals("BMAP") &&
		  (p.spaces[1].getType().equals(Space.TYPE_GRASS) ||
		   p.spaces[1].getType().equals(Space.TYPE_CNSTR) || 
		   p.spaces[1].getType().equals(Space.TYPE_UKNWN))) {
		  return false;
	      }
	  }
	  return true;
      }
      
      // if we get here, no known route type -- so just way OK
      return true;
  }

  public boolean contains(Space s) {
    // defensive check
    if (s == null) { return false; }
    Vector names = this.getRoute();
    for (int i = 0; i < names.size(); i++) {
      String name = (String) names.get(i);
      if (name.equals(s.getName())) {
	return true;
      }
    }
    return false;
  }

}
