package de.drhoffmannsoftware.xearth;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
 */

/* Calculates the position of the sun */

public class Sun {	
	final private static double TWOPI=2*Math.PI;
	static double DegsToRads(final double x) {
		return ((x)*(TWOPI/360));
	}
	static double alpha,delta;

	static void ecliptic_to_equatorial(final double lambda, final double beta) {
		final double sin_e = Math.sin(MeanObliquity);
		final double cos_e = Math.cos(MeanObliquity);

		alpha = (Math.atan2(Math.sin(lambda)*cos_e - Math.tan(beta)*sin_e, Math.cos(lambda)));
		delta = (Math.asin(Math.sin(beta)*cos_e + Math.cos(beta)*sin_e*Math.sin(lambda)));
	}
	private static double  Normalize(double x) {
		while(x<-Math.PI) x+=TWOPI;
		while(x>Math.PI) x-=TWOPI;
		return x;
	}
	
	double   lon;               /* sun position longitude      */
	double   lat;               /* sun position latitude       */
	final static long  EpochStart=631065600;
	static double DaysSinceEpoch(long secs) {
		return (double)((secs-EpochStart)*(1.0/(24*3600)));
	}
	final static double RadsPerDay=(TWOPI/365.242191);
	final static double Epsilon_g=(DegsToRads(279.403303));
	final static double OmegaBar_g=(DegsToRads(282.768422));
	final static double Eccentricity=(0.016713);
	final static double MeanObliquity=(23.440592*(TWOPI/360));


	public double[] vector(final Viewpos v) {
		final double[] rslt=new double[3];
		rslt[0] = Math.sin(lon * (Math.PI/180)) * Math.cos(lat * (Math.PI/180));
		rslt[1] = Math.sin(lat * (Math.PI/180));
		rslt[2] = Math.cos(lon * (Math.PI/180)) * Math.cos(lat * (Math.PI/180));
		return v.XFORM_ROTATE(rslt);
	}	
	void sun_position(final long ssue) {
		// Log.d("TAG","Time: "+ssue);
		final double lambda = sun_ecliptic_longitude(ssue);
		ecliptic_to_equatorial(lambda, 0.0);
		final double tmp=Normalize(alpha-(TWOPI/24.0)*GST(ssue));
		lon=(tmp*(360.0/TWOPI));
		lat=(delta*(360.0/TWOPI));
		//Log.d("TAG","SUN: lambda="+lambda+" tmp="+tmp+" lon="+sun_lon+", lat="+sun_lat+" alpha="+alpha+", delta="+delta);
		//Log.d("TAG"," alpha="+alpha+", delta="+delta);
	}
	static double sun_ecliptic_longitude(final long ssue) {
		final double E = solve_keplers_equation(mean_sun(DaysSinceEpoch(ssue)));
		final double v = 2 * Math.atan(Math.sqrt((1+Eccentricity)/(1-Eccentricity)) * Math.tan(E/2));
		return (v + OmegaBar_g);
	}
	static double mean_sun (final double D) {
		double N = RadsPerDay * D;
		N = (N % TWOPI);
		if (N < 0) N += TWOPI;
		double M = N + Epsilon_g - OmegaBar_g;
		if (M < 0) M += TWOPI;
		return M;
	}
	static double solve_keplers_equation(final double M) {
		double E=M;
		double delta;
		while (true) {
			delta = E - Eccentricity*Math.sin(E) - M;
			if (Math.abs(delta) <= 1e-10) break;
			E -= delta / (1 - Eccentricity*Math.cos(E));
		}
		return E;
	}
	static double julian_date(int y, int m, final int d) {
		if((m==1) || (m==2)) {
			y--;
			m+=12;
		}
		final int A=y/100;
		final int B=2-A+(A/4);
		final int C=(int)(365.25*(double)y);
		final int D = (int) (30.6001 * ((double)m + 1.0));
		final double JD = (double)B + (double)C + (double)D + (double)d + 1720994.5;
		return JD;
	}
	private static double julian_date(final long timestamp) {
		return ((double)((int)( timestamp / 86400.0 )) + 2440587.5);
	}

	static double GST(final long ssue) {
		double     T0;
		double     UT;
//		Calendar cal= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
//		cal.setTimeInMillis(ssue*1000);
//		final double JD = julian_date(cal.getTime().getYear()+1900, cal.getTime().getMonth()+1, cal.getTime().getDate());
		final double JD = julian_date(ssue);
		final double T  = (JD - 2451545) / 36525;
		T0 = ((T + 2.5862e-5) * T + 2400.051336) * T + 6.697374558;
		T0 = (T0 % 24.0);
		if (T0 < 0) T0 += 24;
//		UT = (double)cal.getTime().getHours() + (double)((double)cal.getTime().getMinutes() + cal.getTime().getSeconds() / 60.0) / 60.0;
		UT=(double)(ssue%86400)/3600.0;
		T0 += UT * 1.002737909;
		T0 = (T0 % 24.0);
		if (T0 < 0) T0 += 24;
		return T0;
	}
}
