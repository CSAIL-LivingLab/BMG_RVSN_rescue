package locationserver;

/** this class defines a Portal - a connection between two spaces */
import java.io.*;
import org.w3c.dom.Element;

public class Portal implements Serializable {
  public transient Space[] spaces;
  public String[] spacenames; // non-transient storage of source/dest spaces
  public Polyline contour;
  public String type;
  public String name;
  public String toString;
  
  // some static portal types
  public static String TYPE_OUTDOOR     = "OUTDOOR";  
  public static String TYPE_DOOR        = "DOOR";
  public static String TYPE_IMPLICIT    = "IMPLICIT"; 
  public static String TYPE_ELEV_DOWN   = "ELEV_DOWN";
  public static String TYPE_ELEV_UP     = "ELEV_UP";  
  public static String TYPE_STAIRS_UP   = "STAIRS_UP";
  public static String TYPE_STAIRS_DOWN = "STAIRS_DOWN";
  public static String TYPE_TRANSITION  = "TRANSITION";

  /** 
   * Note the portal contour varies between types:
   * For vertical portals (stairs and elevators), the portal contour is
   * the same as its source space. For implicit portals, the contour is
   * a single line, incident on a shared edge between the source and
   * destinations spaces. 
    */
  
  public Portal() { }

  /**
   * Constructor takes a type, two spaces to connect, and coordinates
   * for the portal's physical location. Upon creation, the portal
   * registers itself with its two spaces. The portal is only valid
   * from A to B.
   */
  public Portal(String name, String type, 
		Space space0, String space1Name, Polyline contour) {
    this.name = name;
    this.type = type;
    this.contour = contour; 
    this.spaces = new Space[2];
    this.spaces[0] = space0;
    //this.spaces[1] = space1; // initialized later, since space1 may not exist yet
    this.spacenames = new String[2];
    this.spacenames[0] = space0.getName();
    this.spacenames[1] = space1Name;
    this.toString = this.spacenames[0] + " :: " 
      + spacenames[1] + " :: " + this.name;
  }
  
  /** 
   * Return a 3D point representing the location of the portal
   */
  public Position getApproximatePosition() {
    Position[] positions = this.contour.getPoints();
    double xsum, ysum, zsum;
    double numpts = positions.length;

    xsum = ysum = zsum = 0.0;
    for (int i = 0; i < numpts; i++) {
	xsum += positions[i].x;
	ysum += positions[i].y;
	zsum += positions[i].z;
    }
    Position center = new Position(xsum/numpts,
				   ysum/numpts,
				   zsum/numpts);
    return center;
  }
  
  public Polyline getContour() {
    return this.contour;
  }

  /**
   * toString method for the portal
   */ 
  public String toString() {
    return this.toString;
  }

  /**
   * .equals comparator for portals, uses the toString method
   */ 
  public boolean equals(Portal p) {
    if (this.toString().equals(p.toString())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Method which returns the weight of a portal with respect to a
   * given route. It doesn't really make sense to say a portal has a
   * fixed weight without specifying the context of it's cost. This is
   * used to implement route preferences (i.e. keep me inside or avoid
   * stairs) but not as hard constraints.
   */
  public double getWeight(Route r) {

    // assume a floor height of 10ft
    if(this.type.equals(TYPE_ELEV_UP) || this.type.equals(TYPE_STAIRS_UP) || 
       this.type.equals(TYPE_ELEV_DOWN) || this.type.equals(TYPE_STAIRS_DOWN))

	return 10.0;
    else
	return 1.0;
  }

  public String getSourceName() {
      return spacenames[0];
  }

  public String getDestName() {
      return spacenames[1];
  }

  public void setDestSpace(Space s) {
      this.spaces[1] = s;
  }

  static public String determineType(Element portal) {
      String ptype  = portal.getAttribute("type");
      String pclass = portal.getAttribute("class");
      
      if (pclass.equalsIgnoreCase("VERTICAL")) {
	  String dir = portal.getAttribute("direction");

	  if (dir.equalsIgnoreCase("DOWN")) {
	      if (ptype.equalsIgnoreCase("ELEV"))
		  return TYPE_ELEV_DOWN;
	      else if (ptype.equalsIgnoreCase("STAIR"))
		  return TYPE_STAIRS_DOWN;

	  } else if (dir.equalsIgnoreCase("UP")) {
	      if (ptype.equalsIgnoreCase("ELEV"))
		  return TYPE_ELEV_UP;
	      else if (ptype.equalsIgnoreCase("STAIR"))
		  return TYPE_STAIRS_UP;
	  }
      } else if (pclass.equalsIgnoreCase("HORIZONTAL")) {
	  if (ptype.equalsIgnoreCase("OUTDOOR"))
	      return TYPE_OUTDOOR;
	  else if (ptype.equalsIgnoreCase("EXPLICIT"))
	      return TYPE_DOOR;
	  else if (ptype.equalsIgnoreCase("IMPLICIT"))
	      return TYPE_IMPLICIT;
	  else if (ptype.equalsIgnoreCase("TRANSITION"))
	      return TYPE_TRANSITION;

      } else {
	  System.out.println("Undefined portal class: " + pclass);
	  return "UNDEFINED";
      }
      System.out.println("Undefined portal type: " + ptype);
      return "UNDEFINED";
  }
}
