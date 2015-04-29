##
##
## The Lambert Conformal Conic Projection
##
## SYNOPSIS
##   Computes the transformation of ellipsoidal coordinates
##   (i.e. latitude and longitude) to the plane via the Lambert
##   conformal conic projection given a state plane definition.
##
##   References:
##    Map Projections - A Working Manual, USGS Professional Paper 1395 by John P. Snyder
##
##   Original Author: Yoni Battat <yonib@mit.edu>
##   Creation Date: February 15, 2006
##
##   Last Modified by: Yoni Battat <yonib@mit.edu>
##   Last Modified: February 16, 2006
##
##
from math import *

def cot(x):
    return 1/tan(x)

def sec(x):
    return 1/cos(x)

class lcc:

    def __init__(self, spcs):

        self.spcs = spcs

        self.n = (log(self.m(self.spcs.standardParallelOne)) - log(self.m(self.spcs.standardParallelTwo))) / (log(self.t(self.spcs.standardParallelOne)) - log(self.t(self.spcs.standardParallelTwo)))
        self.F = self.m(self.spcs.standardParallelOne) / (self.n * pow(self.t(self.spcs.standardParallelOne), self.n))
        self.r0 = self.r(self.spcs.referenceLatitude)

    def m(self, theta):
        return cos(theta) / sqrt(1 - pow(self.spcs.ellipsoid.e, 2) * pow(sin(theta), 2))

    def t(self, theta):
        return tan(pi/4 - theta/2) / pow(((1 - self.spcs.ellipsoid.e*sin(theta)) / (1 + self.spcs.ellipsoid.e*sin(theta))), self.spcs.ellipsoid.e/2)

    def r(self, theta):
        return self.spcs.ellipsoid.a * self.F * pow(self.t(theta), self.n)

    def transform(self, latitude, longitude):

        theta = self.n * (longitude - self.spcs.centralMeridian)
        r = self.r(latitude)

        easting = self.spcs.falseEasting + r * sin(theta)
        northing = self.spcs.falseNorthing + self.r0 - r * cos(theta)

        return (easting, northing)
