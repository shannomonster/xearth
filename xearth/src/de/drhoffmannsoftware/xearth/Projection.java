package de.drhoffmannsoftware.xearth;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
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
