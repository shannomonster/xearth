package de.drhoffmannsoftware.xearth;

/* Projection.java   (c) 2011-2015 by Markus Hoffmann and 
 *                   (c) 1989, 1990, 1993-1995, 1999 by Kirk Lauritz Johnson
 *
 * This file is part of Xearth live Wallpaper for Android 
 * ==================================================================
 * Xearth live Wallpaper for Android is free software and comes with 
 * NO WARRANTY - read the file COPYING/LICENSE for details
 */

/* Handles screen-projection ...*/

public class Projection {
	double proj_scale;
	double proj_xofs;
	double proj_yofs;
	double inv_proj_scale;

	public void setoffset(final double x,final double y) {
		proj_xofs=x;
		proj_yofs=y;
	}	
	/* xy->screen projections */
	double x(final double x) {
		return ((proj_scale*(x))+proj_xofs);
	}
	double y(final double y) {
		return (proj_yofs-proj_scale*y);
	}
	double inv_x(final double x) {
		return ((x-proj_xofs)*inv_proj_scale);
	}
	double inv_y(final double y) {
		return ((proj_yofs-y)*inv_proj_scale);
	}
}
