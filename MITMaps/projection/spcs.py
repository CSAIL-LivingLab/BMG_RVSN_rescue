##
##
## State Plane Coordinate System Class
##
## SYNOPSIS
##   Defines a state plane coordinate system given an
##   ellipsoid, a projection, and other defining parameters.
##
##   Original Author: Yoni Battat <yonib@mit.edu>
##   Creation Date: February 15, 2006
##
##   Last Modified by: Yoni Battat <yonib@mit.edu>
##   Last Modified: February 16, 2006
##
##

class spcs:

    def __init__(self, a, b, c, d, e, f, g, h):

        # All angles assumed to be in radians.
        # All distances assumed to be in meters.

        self.ellipsoid = a
        
        # The Central Meridian, also called the longitude of natural
        # and false origins or the reference longitude.
        self.centralMeridian = b

        # Reference Latitude, also called the latitude of false origin.
        self.referenceLatitude = c

        self.standardParallelOne = d
        self.standardParallelTwo = e

        self.falseEasting = f
        self.falseNorthing = g

        self.projection = h(self)

    def transform(self, latitude, longitude):
        return self.projection.transform(latitude, longitude)
