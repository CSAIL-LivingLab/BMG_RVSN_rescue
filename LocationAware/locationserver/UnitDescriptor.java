package locationserver;

/**  Holds description of units (meters?  inches?), whether the
     coordinates are global or local, and the transform to convert to
     the other type.
*/

public class UnitDescriptor implements java.io.Serializable {
  private String description;
  private String localOrGlobal;
    private double[] transform;   // x,y,z,rotation
    public final Type type;
  /** dummy implementation */
  public UnitDescriptor(UnitDescriptor.Type type, double x,
			double y, double z, double rotation) {
    this.description = "Meters";
    this.type = type;
    this.transform = new double[4];
    this.transform[0] = x;
    this.transform[1] = y;
    this.transform[2] = z;
    this.transform[3] = rotation;
  }
  
  String returnDescription() {
    return this.description;
  }

    double xTranslation() {
	return this.transform[0];
    }    
    double yTranslation() {
	return this.transform[1];
    }    
    double zTranslation() {
	return this.transform[2];
    }    
    double rotation() {
	return this.transform[3];
    }    
  public static class Type implements java.io.Serializable {
      private final String type;
      private Type(String type) {this.type = type;}
      public static final Type GLOBAL =
	  new Type("GLOBAL");
      public static final Type LOCAL =
	  new Type("LOCAL");	
  }
}
