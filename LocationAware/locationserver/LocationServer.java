package locationserver;

import java.awt.*;
import java.util.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**  This interface defines how a LocationServer is accessed.  These
 *  methods give an interface that can be used with RMI or without.
 *  The network protocol functionality mirrors the functionality
 *  expressed in this class and in the rest of the core API.

*/

public interface LocationServer extends Remote {

  public Space getSpace(Location loc) throws RemoteException;
  public Space getSpace(String spaceName) throws RemoteException;
  public Portal getPortal(String portalName) throws RemoteException;
  public Route getRoute(Route r) throws RemoteException;
  public Vector getSpacesWithConstraints(String blg, String flr) 
    throws RemoteException;
  public String getRandomSpace() throws RemoteException;
  public String[] getSuperNodes() throws RemoteException;
  public Vector getSpacesThatClip(Rectangle r) throws RemoteException;

}
