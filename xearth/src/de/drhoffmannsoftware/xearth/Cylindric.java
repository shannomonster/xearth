package de.drhoffmannsoftware.xearth;

/* This file is part of xearth, the xearth android live background 
 * ============================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
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
