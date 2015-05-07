import java.io.*;
import java.util.*;
import java.rmi.*;
import locationserver.*;

public class MITquest2D {
    
    protected LocationServer locationServer;
    
    public static void main(String[] args) throws IOException {
        
        MITquest2D inst = new MITquest2D();

        System.out.println("<?xml version=\"1.0\"?>");
        System.out.println("<MITquest>");

        Boolean debug = false;
        
        try {
            if (args[2].equals("debug")) debug = true;
        } catch (Exception e) {}

        System.err.println("* debug mode is " + (debug ? "ON" : "OFF") + " *");
        
        try {

            inst.getRoute(args[0], args[1], debug);
            
        } catch (Exception e) {
            System.err.println("Usage: java MITquest2D <start space> <end space> [debug]");
        }

        System.out.println("</MITquest>");
        
    }
    
    void getRoute(String from, String to, Boolean debug) {
        
        try {

            String name = "//localhost/LocationServer";

            System.err.println("* Attempting to connect to server *");
            locationServer = (LocationServer) Naming.lookup(name);
            System.err.println("* Location Server Connected *");

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {

            System.err.println("* Requesting route from " + from + " to " + to + " *");
            Route r = new Route(from, to, Route.ROUTE_ROLLING);
            r = locationServer.getRoute(r);

            Polyline path = r.getPath();
            Position[] points = path.getPoints();
            System.err.println("* Route acquired with " + Integer.toString(points.length) + " nodes *");

            System.out.println("\t<route from=\"" + from + "\" to=\"" + to + "\" type=\"walking\">");
            for (int i = 0; i < points.length; i++) {
                System.out.println("\t\t<point projection=\"MaSP\" " +
                                   "x=\"" + Double.toString(points[i].x) + "\" " +
                                   "y=\"" + Double.toString(points[i].y) + "\" " +
                                   "/>");
            }
            System.out.println("\t</route>");

            if (debug) {
                
                // spaces with contour, centroid, and Delaney triangulation - courtesy of ewhiting
                Vector<String> spaceNames = r.getRoute();
                System.err.println("* Route acquired with " + Integer.toString(spaceNames.size()) + " spaces *");
                
                for (int i = 0; i < spaceNames.size(); i++) {
                    Space s = locationServer.getSpace(spaceNames.get(i));
                    System.out.println("\t<space name=\"" + s.getName() + "\">");
                    
                    Position centroid = s.getCenter();
                    System.out.println("\t\t<centroid projection=\"MaSP\" " +
                                       "x=\"" + Double.toString(centroid.x) + "\" " +
                                       "y=\"" + Double.toString(centroid.y) + "\" " +
                                       "/>");


                    System.out.println("\t\t<triangulation>");
                    double[] triangulation = s.getTrianglesList();
                    for (int j = 0; j < triangulation.length; j++) {
                        System.out.println("\t\t\t<point projection=\"MaSP\" " +
                                           "x=\"" + Double.toString(triangulation[j]) + "\" " +
                                           "y=\"" + Double.toString(triangulation[++j]) + "\" " +
                                           "/>");
                    }
                    System.out.println("\t\t</triangulation>");

                    
                    System.out.println("\t\t<contour>");
                    Polyline p = s.getBoundaries();
                    Position[] contour = p.getPoints();
                    for (int j = 0; j < contour.length; j++) {
                        System.out.println("\t\t\t<point projection=\"MaSP\" " +
                                           "x=\"" + Double.toString(contour[j].x) + "\" " +
                                           "y=\"" + Double.toString(contour[j].y) + "\" " +
                                           "/>");
                    }
                    System.out.println("\t\t</contour>");
                    System.out.println("\t</space>");
                }

	    }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
