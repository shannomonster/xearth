package de.drhoffmannsoftware.xearth;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
 */

public class Mercator {
	final private static double BigNumber=1e6;
	
	/* mercator projection (xyz->xy)
	 * [the argument to MERCATOR_Y() is thresholded against 0.9999999999
	 * and -0.9999999999 instead of 1.0 and -1.0 to avoid numerical
	 * difficulties that can arise when the argument of tan() gets close
	 * to PI/2; thanks to Bill Leonard for helping debug this.]
	 */
	public static double x(final double x, final double z) { return Math.atan2((x), (z)); }
	public static double y(final double y) {
		return (((y) >= 0.9999999999) ? (BigNumber)    
				: (((y) <= -0.9999999999) ? (-BigNumber) 
						: Math.log(Math.tan((Math.asin(y)/2) + (Math.PI/4)))));
	}
	public static double inv_y(final double y) {
		return (Math.sin(2 * (Math.atan(Math.exp(y)) - (Math.PI/4))));
	}
}
