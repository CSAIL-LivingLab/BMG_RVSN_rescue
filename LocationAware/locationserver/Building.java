package locationserver;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * This class defines a Building, which represents a container for 
 * a set of spaces on MIT's campus.  Building objects have a name,
 * and a list of transitional spaces that connect this building to
 * the basemap. Future implementations could include additional
 * properties such as the building contour and bounding box.
 */
public class Building implements Serializable {

  // note that some member variables are marked transient so that
  // serialization doesn't cause memory loops

  // vector of Spaces that connect to the basemap
  private transient Vector transitionSpaces; 
  private String name;           // unique space name

  /*
    get methods for the transient fields
  */
  public Vector getTransitionSpaces() {
    return transitionSpaces;
  }

  /*
    set methods for the transient fields
  */
  public void addTransitionSpace(Space s) {
    this.transitionSpaces.add(s);
  }

  /**
   * No-args constructor for serialized version
   */ 
  public Building() { }

  public Building(String name) {
      this.name = name;
      this.transitionSpaces = new Vector();
  }

  /**
   * Return the globally unique name of the space
   */ 
  public String getName() {
    return this.name;
  }
  
  public boolean equals(String s) {
    return this.name.equals(s);
  }

  public boolean equals(Building b) {
    return this.name.equals(b.getName());
  }
}
