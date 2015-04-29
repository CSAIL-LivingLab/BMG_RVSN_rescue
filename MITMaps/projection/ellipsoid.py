##
##
## Reference Ellipsoid Class
##
## SYNOPSIS
##   Defines an Ellipsoid given ellipsoidal parameters.
##
##   Original Author: Yoni Battat <yonib@mit.edu>
##   Creation Date: February 15, 2006
##
##   Last Modified by: Yoni Battat <yonib@mit.edu>
##   Last Modified: February 16, 2006
##
##
from math import *

class ellipsoid:

    # a   -> Semi-Major Axis in meters
    # b   -> Semi-Minor Axis in meters
    # f   -> Flattening: (a-b)/a
    # e^2 -> First Eccentricity Squared: 2f-f^2

    def __init__(self, a, f):
        self.a = a
        self.f = f
        self.computeE()

    def setA(self, a):
        self.a = a

    def setB(self, b):
        self.b = b

    def setF(self, f):
        self.f = f

    def setE(self, e):
        self.e = e

    def computeF(self):
        if self.a and self.b:
            self.f = ( self.a - self.b ) / self.a

    def computeE(self):
        if self.f:
            self.e = sqrt( (2*self.f) - pow(self.f, 2) )
