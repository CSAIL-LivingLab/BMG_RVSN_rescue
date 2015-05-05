package locationserver;

import java.rmi.*;
import java.rmi.server.*;

/** this class is a singleton.  The java.lang.Object methods are only
 * appropriate for a singleton object.  We have to implement the
 * java.lang.Object methods because we choose not to extend
 * java.rmi.server.UnicastRemoteObject, which is a convenience
 * class.
 */

public class JavaLocationServerRMI extends JavaLocationServer {
  
  private JavaLocationServerRMI singletonReference;
  
  public static void main(String[] args) {
    // create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
    /**we're not going to throw RMI exceptions here, since we're
     * on the local machine */
    try {
      JavaLocationServerRMI ls = new JavaLocationServerRMI(args);
      System.out.println("Beginning binding process");
      Naming.rebind("//localhost/LocationServer", ls);
      System.out.println("JavaLocationServerRMI bound in registry");
    }
    catch (java.lang.Exception e) {
      e.printStackTrace();
    }
  }
  
  private JavaLocationServerRMI() throws RemoteException {
    super();
  }
  
  private JavaLocationServerRMI(String[] filenames) throws RemoteException { 
    super(filenames);
    this.singletonReference = this;
  }

  /** always fail here;  cloning is impossible */
  public Object clone() {
    throw new RuntimeException
      ("JavaLocationServer.java does not support the clone(0 method");
  }

  public String toString() {
    return "JavaLocationServerRMI.java";
  }

  public boolean equals(Object obj) {
    if (obj instanceof JavaLocationServerRMI)
      return true;
    else
      return false;
  }
  
}
