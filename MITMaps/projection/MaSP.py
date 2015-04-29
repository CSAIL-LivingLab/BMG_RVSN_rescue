##
##
## Definition of the Massachusetts State Plane Coordinate System
## With Accompanying Ellipsoid GRS 80
##
## SYNOPSIS
##   Defines the Massachusetts State Plane Coordinate System, Mainland Zone
##   (Fipszone 2001) as described by: http://www.mass.gov/mgis/dd-over.htm
##
##   The website states the following:
##
##    The Massachusetts State Plane Coordinate System, Mainland Zone
##    meters is defined as follows:
##
##    Projection:  Lambert Conformal Conic
##    Spheroid:  GRS 80
##    Central Meridian:  -71.5
##    Reference Latitude:  41
##    Standard Parallel 1:  41.71666666667
##    Standard Parallel 2:  42.68333333333
##    False Easting:  200000
##    False Northing:  750000
##
##   Wikipedia defines GRS 80 as follows:
##
##    GRS 80, or Geodetic Reference System 1980
##
##    Semi-major axis = Equatorial Radius = a = 6,378,137.00000 m;
##    Flattening = f = 0.003352810681225;
##
##   Original Author: Yoni Battat <yonib@mit.edu>
##   Creation Date: February 15, 2006
##
##   Last Modified by: Yoni Battat <yonib@mit.edu>
##   Last Modified: February 16, 2006
##
##
from math import *

from ellipsoid import *
from spcs import *
from lcc import *

##################################################################

class GRS80(ellipsoid):

    def __init__(self):
        # initialize inherited base class
        ellipsoid.__init__(self,
                           6378137.0,
                           0.003352810681225)

##################################################################

class MaSP(spcs):

    feetPerMeter = 3.2808399

    falseEastingOffset = 56300.406613422674  # in feet
    falseNorthingOffset = 2460676.7994120447 # in feet

    def __init__(self):

        ellipsoid = GRS80()

        # initialize inherited base class
        spcs.__init__(self,
                      ellipsoid,
                      radians(-71.5),
                      radians(41),
                      radians(41.71666666667),
                      radians(42.68333333333),
                      200000,
                      750000,
                      lcc)

    def transformToFeet(self, latitude, longitude):

        (x, y) = self.transform(latitude, longitude)

        x = x*self.feetPerMeter - self.falseEastingOffset
        y = y*self.feetPerMeter - self.falseNorthingOffset

        return (x, y)

##################################################################
