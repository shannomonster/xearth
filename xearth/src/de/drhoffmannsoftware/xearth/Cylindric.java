package de.drhoffmannsoftware.xearth;

/* Cylindric.java    (c) 2011 by Markus Hoffmann and 
 *                   (c) 1989, 1990, 1993-1995, 1999 by Kirk Lauritz Johnson
 *
 * This file is part of Xearth live Wallpaper for Android 
 * ==================================================================
 * Xearth live Wallpaper for Android is free software and comes with 
 * NO WARRANTY - read the file COPYING/LICENSE for details
 */

public class Cylindric {
	final private static double BigNumber=1e6;
	/* cylindrical projection (xyz->xy)
	 */
	static double x(final double x, final double z) {
		return (Math.atan2((x), (z)));
	}
	static double y(final double y) {
		return (((y) >= 0.9999999999) ? (BigNumber) 
				: (((y) <= -0.9999999999) ? (-BigNumber) 
						: (Math.tan(Math.asin(y)))));
	}
	static double inv_y(final double y) {
		return Math.sin(Math.atan(y));
	}
}
