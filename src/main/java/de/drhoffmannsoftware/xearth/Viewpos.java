package de.drhoffmannsoftware.xearth;

/* Viewpos.java      (c) 2011-2015 by Markus Hoffmann and 
 *                   (c) 1989, 1990, 1993-1995, 1999 by Kirk Lauritz Johnson
 *
 * This file is part of Xearth live Wallpaper for Android 
 * ==================================================================
 * Xearth live Wallpaper for Android is free software and comes with 
 * NO WARRANTY - read the file COPYING/LICENSE for details
 */

public class Viewpos {
	/* types of viewing positions */

	public final static int ViewPosTypeFixed=0;
	public final static int ViewPosTypeSun=1;
	public final static int ViewPosTypeOrbit=2;
	public final static int ViewPosTypeRandom=3;
	public final static int ViewPosTypeMoon=4;
	/* types of viewing rotations */
	public final static int ViewRotNorth=    0;
	public final static int ViewRotGalactic= 1;

	double   view_lon;              /* viewing position longitude  */
	double   view_lat;              /* viewing position latitude   */
	double   view_rot=0;              /* viewing rotation (degrees)  */
	int      rotate_type=ViewRotNorth;           /* type of rotation            */
	int      view_pos_type=ViewPosTypeSun;         /* type of viewing position    */
	double cos_lat, sin_lat;	/* cos/sin of view_lat */
	double cos_lon, sin_lon;	/* cos/sin of view_lon */
	double cos_rot, sin_rot;	/* cos/sin of view_rot */

	public void calc() {
		cos_lat = Math.cos(view_lat * (Math.PI/180));
		sin_lat = Math.sin(view_lat * (Math.PI/180));
		cos_lon = Math.cos(view_lon * (Math.PI/180));
		sin_lon = Math.sin(view_lon * (Math.PI/180));
		cos_rot = Math.cos(view_rot * (Math.PI/180));
		sin_rot = Math.sin(view_rot * (Math.PI/180));
	}
	public void set(final double lat, final double lon) {
		view_lat=lat;
		view_lon=lon;
	}
	void pick_random_position() {
		int    i;
		final double[] pos=new double[3];
		double mag;
		double s_lat, c_lat;
		double s_lon, c_lon;

		/* select a vector at random */
		do {
			mag = 0;
			for (i=0; i<3; i++) {
				pos[i] = ((Math.random()*20001) * 1e-4) - 1;
				mag   += pos[i] * pos[i];
			}
		} while ((mag > 1.0) || (mag < 0.01));

		/* normalize the vector */
		mag = Math.sqrt(mag);
		for (i=0; i<3; i++)
			pos[i] /= mag;

		/* convert to (lat, lon) */
		s_lat = pos[1];
		c_lat = Math.sqrt(1 - s_lat*s_lat);
		s_lon = pos[0] / c_lat;
		c_lon = pos[2] / c_lat;

		view_lat = Math.atan2(s_lat, c_lat) * (180/Math.PI);
		view_lon = Math.atan2(s_lon, c_lon) * (180/Math.PI);
	}

	final static double TWOPI=2*Math.PI;
	static double DegsToRads(final double x) {
		return ((x)*(TWOPI/360));
	}
	final static double SideralMonth=(27.3217);
	final static double MoonMeanLongitude=DegsToRads(318.351648);
	final static double MoonMeanLongitudePerigee=DegsToRads( 36.340410);
	final static double MoonMeanLongitudeNode=DegsToRads(318.510107);
	final static double MoonInclination=DegsToRads(  5.145396);

	private static double  Normalize(double x) {
		while(x<-Math.PI) x+=TWOPI;
		while(x>Math.PI) x-=TWOPI;
		return x;
	}

	void pick_moon_position(final long ssue) {
		double lambda,beta;
		double L, Mm, N, Ev, Ae, Ec;


		final double D= Sun.DaysSinceEpoch(ssue);
		lambda = Sun.sun_ecliptic_longitude(ssue);
		final double Ms = Sun.mean_sun(D);

		L  = Normalize((D/SideralMonth % 1.0)*TWOPI + MoonMeanLongitude);
		Mm = Normalize(L - DegsToRads(0.1114041*D) - MoonMeanLongitudePerigee);
		N  = Normalize(MoonMeanLongitudeNode - DegsToRads(0.0529539*D));
		Ev  = DegsToRads(1.2739) * Math.sin(2.0*(L-lambda)-Mm);
		Ae  = DegsToRads(0.1858) * Math.sin(Ms);
		Mm += Ev - Ae - DegsToRads(0.37)*Math.sin(Ms);
		Ec  = DegsToRads(6.2886) * Math.sin(Mm);
		L  += Ev + Ec - Ae + DegsToRads(0.214) * Math.sin(2.0*Mm);
		L  += DegsToRads(0.6583) * Math.sin(2.0*(L-lambda));
		N  -= DegsToRads(0.16) * Math.sin(Ms);

		L -= N;
		lambda =(Math.abs(Math.cos(L)) < 1e-12) ?
				(N + Math.sin(L) * Math.cos(MoonInclination) * Math.PI/2) :
					(N + Math.atan2(Math.sin(L) * Math.cos(MoonInclination), Math.cos(L)));
				lambda=Normalize(lambda);
				beta = Math.asin(Math.sin(L) * Math.sin(MoonInclination));
				Sun.ecliptic_to_equatorial(lambda, beta);
				Sun.alpha -= (TWOPI/24)*Sun.GST(ssue);
				Sun.alpha=Normalize(Sun.alpha);
				view_lon = Sun.alpha * (360/TWOPI);
				view_lat = Sun.delta * (360/TWOPI);
	}
	final static int EarthPeriod=86400;

	double   orbit_period=EarthPeriod;          /* orbit period (seconds)      */
	double   orbit_inclin;          /* orbit inclination (degrees) */

	public void pick_simple_orbit(final long ssue) {
		double x, y, z;
		double a, c, s;
		double t1, t2;

		/* start at 0 N 0 E */
		x = 0;
		y = 0;
		z = 1;

		/* rotate in about y axis (from z towards x) according to the number
		 * of orbits we've completed
		 */
		a  = ((double) ssue / orbit_period) * (2*Math.PI);
		c  = Math.cos(a);
		s  = Math.sin(a);
		t1 = c*z - s*x;
		t2 = s*z + c*x;
		z  = t1;
		x  = t2;

		/* rotate about z axis (from x towards y) according to the
		 * inclination of the orbit
		 */
		a  = orbit_inclin * (Math.PI/180);
		c  = Math.cos(a);
		s  = Math.sin(a);
		t1 = c*x - s*y;
		t2 = s*x + c*y;
		x  = t1;
		y  = t2;

		/* rotate about y axis (from x towards z) according to the number of
		 * rotations the earth has made
		 */
		a  = ((double) ssue / EarthPeriod) * (2*Math.PI);
		c  = Math.cos(a);
		s  = Math.sin(a);
		t1 = c*x - s*z;
		t2 = s*x + c*z;
		x  = t1;
		z  = t2;

		view_lat = (Math.asin(y) * (180/Math.PI));
		view_lon = (Math.atan2(x, z) * (180/Math.PI));
	}

	public double[] XFORM_ROTATE(double[] p) {
		double _p0_, _p1_, _p2_;              
		double _c_, _s_, _t_;                 
		_p0_ = p[0];                          
		_p1_ = p[1];                          
		_p2_ = p[2];                          
		_c_  = cos_lon;                   
		_s_  = cos_lon;                   
		_s_  = sin_lon;                   
		_t_  = (_c_ * _p0_) - (_s_ * _p2_);   
		_p2_ = (_s_ * _p0_) + (_c_ * _p2_);   
		_p0_ = _t_;                           
		_c_  = cos_lat;                   
		_s_  = sin_lat;                   
		_t_  = (_c_ * _p1_) - (_s_ * _p2_);   
		_p2_ = (_s_ * _p1_) + (_c_ * _p2_);   
		_p1_ = _t_;                           
		_c_  = cos_rot;                   
		_s_  = sin_rot;                   
		_t_  = (_c_ * _p0_) - (_s_ * _p1_);   
		_p1_ = (_s_ * _p0_) + (_c_ * _p1_);   
		_p0_ = _t_;                           
		p[0] = _p0_;                          
		p[1] = _p1_;                          
		p[2] = _p2_;                          
		return p;
	}
}
