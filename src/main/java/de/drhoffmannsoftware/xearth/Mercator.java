package de.drhoffmannsoftware.xearth;

/* Mercator.java     (c) 2011 by Markus Hoffmann and 
 *                   (c) 1989, 1990, 1993-1995, 1999 by Kirk Lauritz Johnson
 *
 * This file is part of Xearth live Wallpaper for Android 
 * ==================================================================
 * Xearth live Wallpaper for Android is free software and comes with 
 * NO WARRANTY - read the file COPYING/LICENSE for details
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
