package de.drhoffmannsoftware.xearth;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
 */

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

/*
 * Xearth animated wallpaper.
 */
public class XearthWallpaper extends WallpaperService {
	final static String TAG="xearth";
	/* Konstanten */
	// public final static String HomePageURL="http://www.cs.colorado.edu/~tuna/xearth/index.html";
	public final static String HomePageURL="http://hewgill.com/xearth/original/";

	/* types of projections */
	final static int ProjTypeOrthographic=0;
	final static int ProjTypeMercator    =1;
	final static int ProjTypeCylindrical =2;

	/* types of dots*/
	final static int    DotTypeStar=0;
	final static int    DotTypeGrid=1;
	/* types of crossings*/
	final static int    XingTypeEntry=0;
	final static int    XingTypeExit=1;
	final static double MAP_DATA_SCALE=30000;
	final static double BigNumber=1e6;
	
	private final Handler    mHandler=  new Handler();
	private final Paint      mPaint=    new Paint();
	private final Viewpos    viewpos=   new Viewpos();
	private final Projection projection=new Projection();

	private static Map     map=null;
	private static Overlay ovl=null;
	private static BitMap  bmp=null;

	private static String bitMapFile="0";
	static double[][] scanbuf;
	static int[] scanbufi;
	class ScanDot {
		short  x;
		short  y;
		byte type;
	} 
	static ScanDot[] dots;
	static int anzdots;
	
	/* xearth variables and default values */
	static int      proj_type=ProjTypeOrthographic;             /* projection type             */

	static double   fixedpos_lat=50.0;   /*position for fixed pos*/
	static double   fixedpos_lon=9.5;
	static double   sun_rel_lon=0;           /* view lon, relative to sun   */
	static double   sun_rel_lat=0;           /* view lat, relative to sun   */
	static double   view_mag=1.0;            /* viewing magnification       */
	static boolean  do_shade=true;           /* render with shading?        */
	static int      shift_x=0;               /* image shift (x, pixels)     */
	static int      shift_y=0;               /* image shift (y, pixels)     */
	static boolean  do_stars=true;           /* show stars in background?   */
	static double   star_freq=0.002;         /* frequency of stars          */
	static int      big_stars=0;             /* percent of doublewide stars */
	static boolean  do_grid=true;            /* show lon/lat grid?          */
	static int      grid_big=6;              /* lon/lat grid line spacing   */
	static int      grid_small=15;           /* dot spacing along grids     */
	static boolean  do_label=true;           /* label image                 */
	static boolean  do_markers=true;         /* display markers (X only)    */
	static boolean  do_overlay=false;        /* use cloud overlay    */
	static boolean  do_bitmap=false;         /* use bitmap basemap    */
	static int      wait_time=300;           /* wait time between redraw    */
	static double   time_warp=1.0;           /* passage of time multiplier  */
	static long     fixed_time=0;            /* fixed viewing time (ssue)   */
	static int      day=100;                 /* day side brightness (%)     */
	static int      night=5;                 /* night side brightness (%)   */
	static int      terminator=1;            /* terminator discontinuity, % */
	static double   xgamma  = 1.0;

	static Sun sun= new Sun();

	@Override
	public Engine onCreateEngine() { 
		return new XearthEngine(); 
	}

	public class XearthEngine  extends Engine implements OnSharedPreferenceChangeListener {
		private double tmp_lat,tmp_lon;
		boolean     first_scan = true;

		private float   mOffset;
		private long    mStartTime;
		private boolean mVisible;
		
		private SharedPreferences mPrefs;
		private final Runnable mDrawSphere = new Runnable() {
			public void run() {drawFrame();}
		};
		
		class ScanBit {
			short y;
			short lo_x;
			short hi_x;
			short val;
		}
		class EdgeXing {
			byte    type;
			int    cidx;
			double x, y;
			double angle;
		}
		ScanBit[] scanbits;
		int anzscanbits=0;

		EdgeXing[]  edgexings;
		int anzedgexing=0;
		
		
		int    min_y, max_y;
	    int    night_val;
		double day_val_base;
		double day_val_delta;
		int    scanbitcnt;
		int    dotcnt;

	
		boolean  compute_sun_pos=true;       /* compute sun's position?     */
		int      wdth=256;                  /* image width (pixels)        */
		int      hght=256;                  /* image height (pixels)       */
		String    markerfile;            /* for user-spec. marker info  */
		long start_time=0;
		long current_time;

		
		XearthEngine() {
			mPaint.setColor(0xffffffff);
			mPaint.setStyle(Paint.Style.STROKE);
			mStartTime = SystemClock.elapsedRealtime();
			mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
		}
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			proj_type =Integer.valueOf(prefs.getString("prefs_proj", String.valueOf(ProjTypeOrthographic)));
			viewpos.view_pos_type =Integer.valueOf(prefs.getString("prefs_pos", String.valueOf(Viewpos.ViewPosTypeSun)));
			viewpos.rotate_type =Integer.valueOf(prefs.getString("prefs_rot", String.valueOf(Viewpos.ViewRotNorth)));
			try {fixedpos_lat= (Double.parseDouble(prefs.getString("prefs_latpos", "9.0")));} catch(NumberFormatException nfe) {}
			try {fixedpos_lon= (Double.parseDouble(prefs.getString("prefs_lonpos", "50.0")));} catch(NumberFormatException nfe) {}

			day=Integer.valueOf(prefs.getString("prefs_day", "100"));
			night=Integer.valueOf(prefs.getString("prefs_night", "5"));
			terminator=Integer.valueOf(prefs.getString("prefs_term", "1"));
			try {view_mag=(Double.parseDouble(prefs.getString("prefs_mag", "1.0")));} catch(NumberFormatException nfe) {}
			shift_x=Integer.valueOf(prefs.getString("prefs_shiftx", "0"));
			shift_y=Integer.valueOf(prefs.getString("prefs_shifty", "0"));

			do_shade=prefs.getBoolean("prefs_shade", true);
			do_stars=prefs.getBoolean("prefs_stars", true);
			do_grid=prefs.getBoolean("prefs_grid", false);
			do_label=prefs.getBoolean("prefs_label", false);
			do_markers=prefs.getBoolean("prefs_marker", true);
			do_overlay=prefs.getBoolean("prefs_clouds", false);
			do_bitmap=!prefs.getString("prefs_basemap", "0").equalsIgnoreCase("0");
			bitMapFile=prefs.getString("prefs_basemap", "0");
			try {star_freq=Double.parseDouble(prefs.getString("prefs_starfreq","0.002"));} catch(NumberFormatException nfe) {}
			try {wait_time=Integer.valueOf(prefs.getString("prefs_wait","300"));} catch(NumberFormatException nfe) {}
			try {time_warp=Double.parseDouble(prefs.getString("prefs_timewarp","1.0"));} catch(NumberFormatException nfe) {}
		}
		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
		}
		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDrawSphere);
		}
		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible)  drawFrame();
			else  mHandler.removeCallbacks(mDrawSphere);
		}
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			wdth=width;
			hght=height;
			if(isPreview()) {
				if(wdth>240) wdth=240;
				if(hght>240) hght=240;
			}
			if (mVisible) drawFrame();
		}
		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {super.onSurfaceCreated(holder);}
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawSphere);
		}
		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xStep, float yStep, int xPixels, int yPixels) {
			 mOffset = xOffset;
			 drawFrame();
		}
		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. 
		 */
		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			if(map==null) map=new Map(getAssets());

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				synchronized(holder) {
					if (c != null) {
						drawSphere(c);
					}
				}
			} catch (SurfaceHolder.BadSurfaceTypeException e) {
				Log.e("TAG", "onDraw():  BadSurfaceTypeException");
			} finally { 
				if(c != null) {
					try { holder.unlockCanvasAndPost(c); } 
					catch (IllegalArgumentException iae) {
						Log.e("TAG", "onDraw():  IllegalArgumentException");
						// Catching this exception will save the wallpaper from crashing.
					}
				}
			}
			// Reschedule the next redraw
			mHandler.removeCallbacks(mDrawSphere);
			if (mVisible)   mHandler.postDelayed(mDrawSphere, wait_time*1000);
		}

		/* Now draw the earth image */ 
		
		void drawSphere(final Canvas c) {
			c.save();
			c.drawARGB(0xff, 0, 0, 0);
			if(ovl!=null) ovl.recycle();
			if(bmp!=null) bmp.recycle();
			try {
				if(do_overlay) ovl=new Overlay(getApplicationContext(),R.drawable.clouds_2048);
				else ovl=null;

				Log.d("TAG","compute_positions...");
				compute_positions();
				Log.d("TAG","scan map...");
				prepare_projection();

				if(do_bitmap) {
					if(bitMapFile.equalsIgnoreCase("2"))	bmp=new BitMap(getApplicationContext(),R.drawable.night_electric);
					else if(bitMapFile.equalsIgnoreCase("1"))	bmp=new BitMap(getApplicationContext(),R.drawable.usgs);
					else if(bitMapFile.equalsIgnoreCase("0")) {
						bmp=null;
						scan_map();
					} else {
						bmp=new BitMap(bitMapFile);
					}
				} else {
					bmp=null;
					scan_map();
				}
				Log.d("TAG","do_dots...");
				do_dots();

				map.reload_user_markers();

				Log.d("TAG","render...");
				render(c);
				Log.d("TAG","cleanup...");
				x11_cleanup(c); 
			} catch(OutOfMemoryError e) {
				final String text="Fatal ERROR: "+e.toString();
				int x=10;
				int y=hght/2;
				Log.e(TAG,text);
				mPaint.setColor(Color.BLUE);
				mPaint.setAntiAlias(false);
				c.drawText(text,x+1,y,mPaint);
				c.drawText(text,x-1,y,mPaint);
				c.drawText(text,x,y+1,mPaint);
				c.drawText(text,x,y-1,mPaint);
				mPaint.setColor(Color.WHITE);
				mPaint.setAntiAlias(true);
				c.drawText(text,x,y,mPaint);
				mPaint.setAntiAlias(false);
			}
			c.restore();
		}

		void compute_positions() {
			final Calendar cal = Calendar.getInstance();
			/* determine "current" time    	   */
			if(fixed_time==0) {
				current_time=cal.getTimeInMillis()/1000;
				if(start_time==0) start_time=current_time;
				else current_time=start_time+(long)((current_time-start_time)*time_warp);
			} else current_time=fixed_time;
			
			/* determine position on earth's surface where sun is directly
			 * overhead
			 */
			if (compute_sun_pos) {
				sun.sun_position(current_time);
				Log.d("TAG","Sun is at position: "+sun.lat+"/"+sun.lon);
			}
			/* determine viewing position
			 */
			if (viewpos.view_pos_type == Viewpos.ViewPosTypeSun) {
				sun_relative_position();
			} else if (viewpos.view_pos_type == Viewpos.ViewPosTypeOrbit) {
				viewpos.pick_simple_orbit(current_time);
			} else if (viewpos.view_pos_type == Viewpos.ViewPosTypeRandom) {
				viewpos.pick_random_position();
			} else if (viewpos.view_pos_type == Viewpos.ViewPosTypeMoon) {
				viewpos.pick_moon_position(current_time);
			} else {
				// fixed position
				viewpos.view_lat = fixedpos_lat;
				viewpos.view_lon = fixedpos_lon;
			}

			/* for ViewRotGalactic, compute appropriate viewing rotation
			 */
			if (viewpos.rotate_type == Viewpos.ViewRotGalactic) {
				viewpos.view_rot = sun.lat * Math.sin((viewpos.view_lon - sun.lon) * (Math.PI / 180));
			}
			Log.d("TAG","View position: "+viewpos.view_lat+"/"+viewpos.view_lon);
		}

		void sun_relative_position() {
			double lat, lon;
			lat = sun.lat + sun_rel_lat;
			lon = sun.lon + sun_rel_lon;
			if(lat>90) {
				lat  =180-lat;
				lon +=180;
			} else if(lat<-90) {
				lat  =-180-lat;
				lon +=180;
			}
			if(lon>180) {
				do lon-=360; while(lon>180);
			} else if(lon<-180) {
				do lon+=360; while(lon<-180);
			}
			viewpos.view_lat=lat;
			viewpos.view_lon=lon;
		}

		void prepare_projection() {
			viewpos.calc();
			if (proj_type == ProjTypeOrthographic) {
				projection.proj_scale = ((hght < wdth) ? hght : wdth) * (view_mag / 2) * 0.99;
			} else {
				/* proj_type is either ProjTypeMercator or ProjTypeCylindrical
				 */
				projection.proj_scale = (view_mag * wdth) / (2 * Math.PI);
			}
			projection.setoffset((double) wdth / 2 + shift_x,(double) hght / 2 + shift_y);
			projection.inv_proj_scale = 1 / projection.proj_scale;
		}

		void scan_map() {
			int i;
			/* the first time through, allocate scanbits and edgexings;
			 * on subsequent passes, simply reset them.
			 */
			if (first_scan) {
				scanbits  = new ScanBit[hght*15];
				anzscanbits=0;
				edgexings = new EdgeXing[hght*2];
				anzedgexing=0;
			} else {
				anzscanbits = 0;
				anzedgexing = 0;
			}

			/* maybe only allocate these once and reset them on
			 * subsequent passes (like scanbits and edgexings)?
			 */

			scanbuf=new double[wdth][hght];
			scanbufi=new int[hght];
			for (i=0; i<hght; i++) scanbufi[i]=0;

			if (proj_type == ProjTypeOrthographic) {
				orth_scan_outline();
				orth_scan_curves();
			} else if (proj_type == ProjTypeMercator) {
				merc_scan_outline();
				merc_scan_curves();
			} else /* (proj_type == ProjTypeCylindrical) */ {
				cyl_scan_outline();
				cyl_scan_curves();
			}

			for (i=0; i<hght; i++) scanbufi[i]=0;
			//    extarr_free(scanbuf[i]);
			//  free(scanbuf);
			/*TODO*/
			//  qsort(scanbits->body, scanbits->count, sizeof(ScanBit), scanbit_comp);
			Arrays.sort(scanbits,0,anzscanbits,new Comparator<ScanBit>() {
				@Override
				public int compare(ScanBit entry1, ScanBit entry2) {
					return(entry1.y-entry2.y);
				}
			});
			// Log.d("TAG","We have "+anzscanbits+" Scanbits.");

			first_scan = false;
		}
		void merc_scan_outline() {
			double left, right;
			double top, bottom;

			min_y = hght;
			max_y = -1;

			left   = projection.x(-Math.PI);
			right  = projection.x(Math.PI);
			top    = projection.y(BigNumber);
			bottom = projection.y(-BigNumber);

			scan(right, top, left, top);
			scan(left, top, left, bottom);
			scan(left, bottom, right, bottom);
			scan(right, bottom, right, top);

			get_scanbits(64);
		}
		void cyl_scan_outline() {
			double left, right;
			double top, bottom;

			min_y = hght;
			max_y = -1;

			left   = projection.x(-Math.PI);
			right  = projection.x(Math.PI);
			top    = projection.y(BigNumber);
			bottom = projection.y(-BigNumber);

			scan(right, top, left, top);
			scan(left, top, left, bottom);
			scan(left, bottom, right, bottom);
			scan(right, bottom, right, top);

			get_scanbits(64);
		}

		void orth_scan_outline() {
			min_y = hght;
			max_y = -1;
			orth_scan_arc(1.0, 0.0, 0.0, 1.0, 0.0, (2*Math.PI)); /*Ganze Erdscheibe mit Wasser füllen*/
			get_scanbits(64);
		}
		void orth_scan_arc(final double x_0, final double y_0, final double a_0, 
				final double x_1, final double y_1, final double a_1) {
			int    i;
			int    lo, hi;
			double angle, step;
			double prev_x, prev_y;
			double curr_x, curr_y;
			double c_step, s_step;
			double arc_x, arc_y;
			double tmp;

			step = projection.inv_proj_scale * 10;
			if (step > 0.05) step = 0.05;
			lo = (int) Math.ceil(a_0 / step);
			hi = (int) Math.floor(a_1 / step);

			prev_x = projection.x(x_0);
			prev_y = projection.y(y_0);
			if (lo <= hi) {
				c_step = Math.cos(step);
				s_step = Math.sin(step);

				angle = lo * step;
				arc_x = Math.cos(angle);
				arc_y = Math.sin(angle);

				for (i=lo; i<=hi; i++) {
					curr_x = projection.x(arc_x);
					curr_y = projection.y(arc_y);
					scan(prev_x, prev_y, curr_x, curr_y);

					/* instead of repeatedly calling cos() and sin() to get the next
					 * values for arc_x and arc_y, simply rotate the existing values
					 */
					tmp   = (c_step * arc_x) - (s_step * arc_y);
					arc_y = (s_step * arc_x) + (c_step * arc_y);
					arc_x = tmp;

					prev_x = curr_x;
					prev_y = curr_y;
				}
			}

			curr_x = projection.x(x_1);
			curr_y = projection.y(y_1);
			scan(prev_x, prev_y, curr_x, curr_y);
		}

		/*Gehe alle Poligonzuege durch.*/

		void orth_scan_curves() {
			int     i;
			int     cidx;
			int     npts;
			int     val;

			int rawoffset=0;
			double[] pos;
			double[] prev=new double[3];
			double[] curr=new double[3];

			cidx = 0;

			while (true) {
				npts = Map.data[0+rawoffset];
				if (npts == 0) break;
				val  = Map.data[1+rawoffset];
				rawoffset += 2;

				pos   = orth_extract_curve(npts, rawoffset);
				// Log.d("TAG","Curve #"+cidx+" "+npts+" points.");
				prev[0]  = pos[(npts-1)*3+0];
				prev[1]  = pos[(npts-1)*3+1];
				prev[2]  = pos[(npts-1)*3+2];

				min_y = hght;
				max_y = -1;

				for (i=0; i<npts; i++) {
					curr[0]  = pos[0+3*i];
					curr[1]  = pos[1+3*i];
					curr[2]  = pos[2+3*i];
					orth_scan_along_curve(prev, curr, cidx);
					prev[0]  = curr[0];
					prev[1]  = curr[1];
					prev[2]  = curr[2];
				}
				if (anzedgexing > 0)  orth_handle_xings();
				if (min_y <= max_y)   get_scanbits(val);
				cidx++;
				rawoffset  += 3*npts;
			}
		}

		void cyl_scan_curves() {
			int     i;
			int     cidx=0;
			int     npts;
			int     val;
			int rawoffset=0;
			double[] pos;
			double[] prev=new double[5];
			double[] curr=new double[5];
			while (true) {
				npts = Map.data[0+rawoffset];
				if (npts == 0) break;
				val  = Map.data[1+rawoffset];
				rawoffset += 2;
				pos   = cycl_extract_curve(npts, rawoffset);
				// Log.d("TAG","Curve #"+cidx+" "+npts+" points.");
				prev[0]  = pos[(npts-1)*5+0];
				prev[1]  = pos[(npts-1)*5+1];
				prev[2]  = pos[(npts-1)*5+2];
				prev[3]  = pos[(npts-1)*5+3];
				prev[4]  = pos[(npts-1)*5+4];

				min_y = hght;
				max_y = -1;

				for (i=0; i<npts; i++) { 
					curr[0]  = pos[0+5*i];
					curr[1]  = pos[1+5*i];
					curr[2]  = pos[2+5*i];
					curr[3]  = pos[3+5*i];
					curr[4]  = pos[4+5*i];

					cyl_scan_along_curve(prev, curr, cidx);
					prev[0]  = curr[0];
					prev[1]  = curr[1];
					prev[2]  = curr[2];
					prev[3]  = curr[3];
					prev[4]  = curr[4];

				}
				if (anzedgexing > 0)  cycl_handle_xings();
				if (min_y <= max_y)   get_scanbits(val);
				cidx++;
				rawoffset  += 3*npts;
			}
		}
		void merc_scan_curves() {
			int     i;
			int     cidx=0;
			int     npts;
			int     val;
			int rawoffset=0;
			double[] pos;
			double[] prev=new double[5];
			double[] curr=new double[5];
			while (true) {
				npts = Map.data[0+rawoffset];
				if (npts == 0) break;
				val  = Map.data[1+rawoffset];
				rawoffset += 2;
				pos   = merc_extract_curve(npts, rawoffset);
				// Log.d("TAG","Curve #"+cidx+" "+npts+" points.");
				prev[0]  = pos[(npts-1)*5+0];
				prev[1]  = pos[(npts-1)*5+1];
				prev[2]  = pos[(npts-1)*5+2];
				prev[3]  = pos[(npts-1)*5+3];
				prev[4]  = pos[(npts-1)*5+4];

				min_y = hght;
				max_y = -1;

				for (i=0; i<npts; i++) { 
					curr[0]  = pos[0+5*i];
					curr[1]  = pos[1+5*i];
					curr[2]  = pos[2+5*i];
					curr[3]  = pos[3+5*i];
					curr[4]  = pos[4+5*i];

					merc_scan_along_curve(prev, curr, cidx);
					prev[0]  = curr[0];
					prev[1]  = curr[1];
					prev[2]  = curr[2];
					prev[3]  = curr[3];
					prev[4]  = curr[4];

				}
				if (anzedgexing > 0)  merc_handle_xings();
				if (min_y <= max_y)   get_scanbits(val);
				cidx++;
				rawoffset  += 3*npts;
			}
		}


		double[] orth_extract_curve(final int npts, int offset) {
			int posoffset=0;
			int     x=0, y=0, z=0;
			double[] pos=new double[3];
			final double[] rslt=new double[3*npts];
			final double scale = 1.0 / MAP_DATA_SCALE;

			for (int i=0; i<npts; i++) {
				x += Map.data[0+offset];
				y += Map.data[1+offset];
				z += Map.data[2+offset];
				//Log.d("TAG","c: x="+x+" y="+y+" z="+z);
				pos[0] = x * scale;
				pos[1] = y * scale;
				pos[2] = z * scale;
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				pos=viewpos.XFORM_ROTATE(pos);
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				rslt[posoffset+0]=pos[0];
				rslt[posoffset+1]=pos[1];
				rslt[posoffset+2]=pos[2];
				offset += 3;
				posoffset  += 3;
			}
			return rslt;
		}
		double[] merc_extract_curve(final int npts, int offset) {
			int posoffset=0;
			int     x=0, y=0, z=0;
			double[] pos=new double[3];
			final double[] rslt=new double[5*npts];
			final double scale = 1.0 / MAP_DATA_SCALE;

			for (int i=0; i<npts; i++) {
				x += Map.data[0+offset];
				y += Map.data[1+offset];
				z += Map.data[2+offset];
				//Log.d("TAG","c: x="+x+" y="+y+" z="+z);
				pos[0] = x * scale;
				pos[1] = y * scale;
				pos[2] = z * scale;
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				pos=viewpos.XFORM_ROTATE(pos);
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				rslt[posoffset+0]=pos[0];
				rslt[posoffset+1]=pos[1];
				rslt[posoffset+2]=pos[2];
				rslt[posoffset+3]=Mercator.x(pos[0], pos[2]);
				rslt[posoffset+4]=Mercator.y(pos[1]);

				offset += 3;
				posoffset  += 5;
			}
			return rslt;
		}
		double[] cycl_extract_curve(final int npts,  int offset) {
			int posoffset=0;
			int     x=0, y=0, z=0;
			double[] pos=new double[3];
			final double[] rslt=new double[5*npts];
			final double scale = 1.0 / MAP_DATA_SCALE;

			for (int i=0; i<npts; i++) {
				x += Map.data[0+offset];
				y += Map.data[1+offset];
				z += Map.data[2+offset];
				//Log.d("TAG","c: x="+x+" y="+y+" z="+z);
				pos[0] = x * scale;
				pos[1] = y * scale;
				pos[2] = z * scale;
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				pos=viewpos.XFORM_ROTATE(pos);
				//  		Log.d("TAG","p: x="+pos[0]+" y="+pos[1]+" z="+pos[2]);

				rslt[posoffset+0]=pos[0];
				rslt[posoffset+1]=pos[1];
				rslt[posoffset+2]=pos[2];
				rslt[posoffset+3]=Cylindric.x(pos[0], pos[2]);
				rslt[posoffset+4]=Cylindric.y(pos[1]);

				offset += 3;
				posoffset  += 5;
			}
			return rslt;
		}

		void scan(final double x_0, final double y_0, final double x_1, final double y_1) {
			int    lo_y, hi_y;
			double x_value;
			double x_delta;

			if (y_0 < y_1) {
				lo_y = (int) Math.ceil(y_0 - 0.5);
				hi_y = (int) Math.floor(y_1 - 0.5);

				//    		if (hi_y == (int)(y_1 - 0.5))
				//    			hi_y -= 1;
			} else {
				lo_y = (int) Math.ceil(y_1 - 0.5);
				hi_y = (int) Math.floor(y_0 - 0.5);

				//    		if (hi_y == (int)(y_0 - 0.5))
				//    			hi_y -= 1;
			}


			if (lo_y < 0)     lo_y = 0;
			if (hi_y >= hght) hi_y = hght-1;
			//Log.d("TAG","scan: y=("+lo_y+","+hi_y+")");
			if (lo_y > hi_y)  return;		                     /* no scan lines crossed */
			//	Log.d("TAG","scan: y=("+lo_y+","+hi_y+")");


			if (lo_y < min_y) min_y = lo_y;
			if (hi_y > max_y) max_y = hi_y;

			x_delta = (x_1 - x_0) / (y_1 - y_0);
			x_value = x_0 + x_delta * ((lo_y + 0.5) - y_0);

			for (int i=lo_y; i<=hi_y; i++) {
				if(scanbufi[i]<wdth) {
					scanbuf[scanbufi[i]][i]=x_value;
					scanbufi[i]++;
				} else {
					Log.d("TAG","Hier stimmt was nicht.... scanbuf-ueberlauf");
				}
				x_value += x_delta;
			}
		}

		/*Scanbuffer zu Scanbits convertieren. Hierbei wird val as wert fuer scanbits verwendet*/

		void get_scanbits(final int val) {
			int      j;
			int      lo_x, hi_x;
			int nvals;

			// Log.d("TAG","Getscanbits: min="+min_y+" max="+max_y);

			for (int i=min_y; i<=max_y; i++) {
				nvals = scanbufi[i];
				if(nvals>0) {
					double[] vals=new double[nvals];
					for(j=0;j<nvals;j++) vals[j]=scanbuf[j][i];
					Arrays.sort(vals);
					for(j=0;j<nvals;j++) scanbuf[j][i]=vals[j];
					//qsort(vals, nvals, sizeof(double), double_comp);
					//Log.d("TAG","Hist("+i+"): "+nvals);
					for (j=0; j<nvals; j+=2) {
						lo_x = (int) Math.ceil(scanbuf[j][i] - 0.5);
						hi_x = (int) Math.floor(scanbuf[j+1][i] - 0.5);


						if (lo_x < 0)     lo_x = 0;
						if (hi_x >= wdth) hi_x = wdth-1;
						if (lo_x <= hi_x) {
							scanbits[anzscanbits]=new ScanBit();

							scanbits[anzscanbits].y    = (short) i;
							scanbits[anzscanbits].lo_x = (short) lo_x;
							scanbits[anzscanbits].hi_x = (short) hi_x;
							scanbits[anzscanbits].val  = (short) val;
							anzscanbits++;
						}
					}
				}
				scanbufi[i]=0;
			}
			//Log.d("TAG","We now have "+anzscanbits+" Scanbits.");
		}
		void cyl_scan_along_curve(final double[] prev, final double[] curr,final int  cidx) {
			double    mx, my;
			final double px = prev[3];
			final double cx = curr[3];
			final double py = prev[4];
			final double cy = curr[4];
			double dx = cx - px;

			if (dx > 0) {
				/* curr to the right of prev */
				if (dx > ((2*Math.PI) - dx)) {
					/* vertical edge crossing to the left of prev
					 */

					/* find exit point (left edge) */
					mx = - Math.PI;
					my = cyl_find_edge_xing(prev, curr);

					/* scan from prev to exit point */
					scan(projection.x(px), projection.y(py), projection.x(mx), projection.y(my));

					/* (mx, my) is an edge crossing (exit point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeExit;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 2;/* left edge */
					anzedgexing++;

					/* scan from entry point (right edge) to curr */
					mx = Math.PI;
					scan(projection.x(mx), projection.y(my), projection.x(cx), projection.y(cy));

					/* (mx, my) is an edge crossing (entry point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeEntry;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 0;/* right edge */
					anzedgexing++;
				} else {
					/* no vertical edge crossing  */
					scan(projection.x(px), projection.y(py), projection.x(cx), projection.y(cy));
				}
			} else {
				/* curr to the left of prev
				 */
				dx = - dx;

				if (dx > ((2*Math.PI) - dx)){
					/* vertical edge crossing to the right of prev
					 */

					/* find exit point (right edge) */
					mx = Math.PI;
					my = cyl_find_edge_xing(prev, curr);

					/* scan from prev to exit point */
					scan(projection.x(px), projection.y(py), projection.x(mx), projection.y(my));

					/* (mx, my) is an edge crossing (exit point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeExit;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 0;/* right edge */
					anzedgexing++;


					/* scan from entry point (left edge) to curr */
					mx = - Math.PI;
					scan(projection.x(mx), projection.y(my), projection.x(cx), projection.y(cy));

					/* (mx, my) is an edge crossing (entry point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeEntry;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 2;/* left edge */
					anzedgexing++;
				} else {
					/* no vertical edge crossing */
					scan(projection.x(px), projection.y(py), projection.x(cx), projection.y(cy));
				}
			}

		}
		void merc_scan_along_curve(final double[] prev, final double[] curr,final int  cidx) {
			final double    px, py;
			final double    cx, cy;
			double    dx;
			double    mx, my;

			px = prev[3];
			cx = curr[3];
			py = prev[4];
			cy = curr[4];
			dx = cx - px;

			if (dx > 0) {
				/* curr to the right of prev
				 */

				if (dx > ((2*Math.PI) - dx)) {
					/* vertical edge crossing to the left of prev
					 */

					/* find exit point (left edge) */
					mx = - Math.PI;
					my = merc_find_edge_xing(prev, curr);

					/* scan from prev to exit point */
					scan(projection.x(px), projection.y(py), projection.x(mx), projection.y(my));

					/* (mx, my) is an edge crossing (exit point) */

					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeExit;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 2;/* left edge */
					anzedgexing++;


					/* scan from entry point (right edge) to curr */
					mx = Math.PI;
					scan(projection.x(mx), projection.y(my), projection.x(cx), projection.y(cy));

					/* (mx, my) is an edge crossing (entry point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeEntry;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 0;/* right edge */
					anzedgexing++;
				} else {
					/* no vertical edge crossing
					 */
					scan(projection.x(px), projection.y(py), projection.x(cx), projection.y(cy));
				}
			} else {
				/* curr to the left of prev
				 */
				dx = - dx;

				if (dx > ((2*Math.PI) - dx))
				{
					/* vertical edge crossing to the right of prev
					 */

					/* find exit point (right edge) */
					mx = Math.PI;
					my = merc_find_edge_xing(prev, curr);

					/* scan from prev to exit point */
					scan(projection.x(px), projection.y(py), projection.x(mx), projection.y(my));

					/* (mx, my) is an edge crossing (exit point) */      		
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeExit;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 0;/* right edge */
					anzedgexing++;


					/* scan from entry point (left edge) to curr */
					mx = - Math.PI;
					scan(projection.x(mx), projection.y(my), projection.x(cx), projection.y(cy));

					/* (mx, my) is an edge crossing (entry point) */
					edgexings[anzedgexing]=new EdgeXing();
					edgexings[anzedgexing].type  = XingTypeEntry;
					edgexings[anzedgexing].cidx  = cidx;
					edgexings[anzedgexing].x     = mx;
					edgexings[anzedgexing].y     = my;
					edgexings[anzedgexing].angle = 2;/* left edge */
					anzedgexing++;
				} else {
					/* no vertical edge crossing
					 */
					scan(projection.x(px), projection.y(py), projection.x(cx), projection.y(cy));
				}
			}

		}
		void orth_scan_along_curve(final double[] prev, final double[] curr,final int  cidx) {
			double[]    extra;
			//Log.d("TAG","Scanalong curve: x="+prev[0]+" "+curr[0]);    	
			//Log.d("TAG","Scanalong curve: y="+prev[1]+" "+curr[1]);    	
			//Log.d("TAG","Scanalong curve: z="+prev[2]+" "+curr[2]);    	

			if (prev[2] <= 0) {            /* prev not visible */ 
				if (curr[2] <= 0)
					return;                   /* neither point visible */

				extra=orth_find_edge_xing(prev, curr);

				/* extra[] is an edge crossing (entry point) */
				edgexings[anzedgexing]=new EdgeXing();
				edgexings[anzedgexing].type  = XingTypeEntry;
				edgexings[anzedgexing].cidx  = cidx;
				edgexings[anzedgexing].x     = extra[0];
				edgexings[anzedgexing].y     = extra[1];
				edgexings[anzedgexing].angle = Math.atan2(extra[1], extra[0]);
				anzedgexing++;
				prev[0]=extra[0];
				prev[1]=extra[1];
				prev[2]=extra[2];
			} else if (curr[2] <= 0)        /* curr not visible */ {
				extra=orth_find_edge_xing(prev, curr);

				/* extra[] is an edge crossing (exit point) */
				edgexings[anzedgexing]=new EdgeXing();
				edgexings[anzedgexing].type  = XingTypeExit;
				edgexings[anzedgexing].cidx  = cidx;
				edgexings[anzedgexing].x     = extra[0];
				edgexings[anzedgexing].y     = extra[1];
				edgexings[anzedgexing].angle = Math.atan2(extra[1], extra[0]);
				anzedgexing++;
				curr[0] = extra[0];
				curr[1] = extra[1];
				curr[2] = extra[2];
			}
			scan(projection.x(prev[0]), projection.y(prev[1]),projection.x(curr[0]), projection.y(curr[1]));
		}

		double[] orth_find_edge_xing(final double[] prev, final double[] curr) {
			double tmp;
			double r0, r1;
			final double[]rslt=new double[3];
			tmp = curr[2] / (curr[2] - prev[2]);
			r0 = curr[0] - tmp * (curr[0] - prev[0]);
			r1 = curr[1] - tmp * (curr[1] - prev[1]);

			tmp = Math.sqrt((r0*r0) + (r1*r1));
			rslt[0] = r0 / tmp;
			rslt[1] = r1 / tmp;
			rslt[2] = 0;
			return rslt;
		}

		double merc_find_edge_xing(final double[] prev, final double[] curr) {
			final double scale;
			double z1, z2;

			if (curr[0] != 0) {
				final double ratio = (prev[0] / curr[0]);
				z1 = prev[1] - (ratio * curr[1]);
				z2 = prev[2] - (ratio * curr[2]);
			} else {
				z1 = curr[1];
				z2 = curr[2];
			}

			scale = ((z2 > 0) ? -1 : 1) / Math.sqrt((z1*z1) + (z2*z2));
			return Mercator.y(z1*scale);
		}
		double cyl_find_edge_xing(final double[] prev, final double[] curr) {
			final double scale;
			double z1, z2;

			if (curr[0] != 0) {
				final double ratio = (prev[0] / curr[0]);
				z1 = prev[1] - (ratio * curr[1]);
				z2 = prev[2] - (ratio * curr[2]);
			} else {
				z1 = curr[1];
				z2 = curr[2];
			}
			scale = ((z2 > 0) ? -1 : 1) / Math.sqrt((z1*z1) + (z2*z2));
			return Cylindric.y(z1*scale);
		}

		void merc_handle_xings() {
			int i,ifrom,ito;
			//qsort(xings, (unsigned) nxings, sizeof(EdgeXing), orth_edgexing_comp);
			Arrays.sort(edgexings,0,anzedgexing,new Comparator<EdgeXing>() {
				@Override
				public int compare(EdgeXing entry1, EdgeXing entry2) {
					if(entry1.angle<entry2.angle) return(-1);
					else if(entry1.angle>entry2.angle) return 1;
					else {
						if(entry1.angle==0) {
							if(entry1.y<entry2.y) return(-1);
							else if(entry1.y>entry2.y) return 1;
							else return(0);
						} else if(entry1.angle==2) {
							if(entry1.y<entry2.y) return(1);
							else if(entry1.y>entry2.y) return -1;
							else return(0);
						}
						return(0);
					}
				}
			});
			// Log.d("TAG","We have "+anzedgexing+" Edge crossings.");

			if (edgexings[0].type == XingTypeExit) {
				for (i=0; i<anzedgexing; i+=2) {
					ifrom = i;
					ito   = i+1;

					if((edgexings[ifrom].type != XingTypeExit) || (edgexings[ito].type != XingTypeEntry)) {
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					}
					merc_scan_edge(ifrom,ito); 
				}
			} else {
				ifrom = anzedgexing-1;
				ito   = 0;

				if ((edgexings[ifrom].type != XingTypeExit) ||
						(edgexings[ito].type != XingTypeEntry) ||
						(edgexings[ifrom].angle < edgexings[ito].angle))
					//  xing_error(__FILE__, __LINE__, nxings-1, nxings, xings);
					Log.d("TAG","ERROR ! xings");
				merc_scan_edge(ifrom,ito);

				for (i=1; i<(anzedgexing-1); i+=2) {
					ifrom = i;
					ito   = i+1;

					if ((edgexings[ifrom].type != XingTypeExit) ||
							(edgexings[ito].type != XingTypeEntry))
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					merc_scan_edge(ifrom,ito);
				}
			}
			anzedgexing = 0;	
		}
		void cycl_handle_xings() {
			int i,ifrom,ito;
			//qsort(xings, (unsigned) nxings, sizeof(EdgeXing), orth_edgexing_comp);
			Arrays.sort(edgexings,0,anzedgexing,new Comparator<EdgeXing>() {
				@Override
				public int compare(EdgeXing entry1, EdgeXing entry2) {
					if(entry1.angle<entry2.angle) return(-1);
					else if(entry1.angle>entry2.angle) return 1;
					else {
						if(entry1.angle==0) {
							if(entry1.y<entry2.y) return(-1);
							else if(entry1.y>entry2.y) return 1;
							else return(0);
						} else if(entry1.angle==2) {
							if(entry1.y<entry2.y) return(1);
							else if(entry1.y>entry2.y) return -1;
							else return(0);
						}
						return(0);
					}
				}
			});
			// Log.d("TAG","We have "+anzedgexing+" Edge crossings.");

			if (edgexings[0].type == XingTypeExit) {
				for (i=0; i<anzedgexing; i+=2) {
					ifrom = i;
					ito   = i+1;

					if((edgexings[ifrom].type != XingTypeExit) || (edgexings[ito].type != XingTypeEntry)) {
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					}
					cyl_scan_edge(ifrom,ito); 
				}
			} else {
				ifrom = anzedgexing-1;
				ito   = 0;

				if ((edgexings[ifrom].type != XingTypeExit) ||
						(edgexings[ito].type != XingTypeEntry) ||
						(edgexings[ifrom].angle < edgexings[ito].angle))
					//  xing_error(__FILE__, __LINE__, nxings-1, nxings, xings);
					Log.d("TAG","ERROR ! xings");
				cyl_scan_edge(ifrom,ito);

				for (i=1; i<(anzedgexing-1); i+=2) {
					ifrom = i;
					ito   = i+1;

					if ((edgexings[ifrom].type != XingTypeExit) ||
							(edgexings[ito].type != XingTypeEntry))
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					cyl_scan_edge(ifrom,ito);
				}
			}
			anzedgexing = 0;
		}
		void cyl_scan_edge(final int ifrom,final int ito) {
			int    s0,  s_new;
			double x_0, x_new;
			double y_0, y_new;

			s0 = (int)edgexings[ifrom].angle;
			x_0 = projection.x(edgexings[ifrom].x);
			y_0 = projection.y(edgexings[ifrom].y);

			final int s1 = (int)edgexings[ito].angle;
			final double x_1 = projection.x(edgexings[ito].x);
			final double y_1 = projection.y(edgexings[ito].y);

			while (s0 != s1) {
				switch (s0) {
				case 0:
					x_new = projection.x(Math.PI);
					y_new = projection.y(BigNumber);
					s_new = 1;
					break;

				case 1:
					x_new = projection.x(-Math.PI);
					y_new = projection.y(BigNumber);
					s_new = 2;
					break;

				case 2:
					x_new = projection.x(-Math.PI);
					y_new = projection.y(-BigNumber);
					s_new = 3;
					break;

				case 3:
					x_new = projection.x(Math.PI);
					y_new = projection.y(-BigNumber);
					s_new = 0;
					break;

				default:
					/* keep lint happy */
					x_new = y_new = s_new = 0;
				}
				scan(x_0, y_0, x_new, y_new);
				x_0 = x_new;
				y_0 = y_new;
				s0 = s_new;
			}
			scan(x_0, y_0, x_1, y_1);
		};
		void merc_scan_edge(final int ifrom,final int ito) {
			int    s0,  s_new;
			double x_0, x_new;
			double y_0, y_new;

			s0 = (int)edgexings[ifrom].angle;
			x_0 = projection.x(edgexings[ifrom].x);
			y_0 = projection.y(edgexings[ifrom].y);

			final int s1 = (int)edgexings[ito].angle;
			final double x_1 = projection.x(edgexings[ito].x);
			final double y_1 = projection.y(edgexings[ito].y);

			while (s0 != s1) {
				switch (s0)
				{
				case 0:
					x_new = projection.x(Math.PI);
					y_new = projection.y(BigNumber);
					s_new = 1;
					break;

				case 1:
					x_new = projection.x(-Math.PI);
					y_new = projection.y(BigNumber);
					s_new = 2;
					break;

				case 2:
					x_new = projection.x(-Math.PI);
					y_new = projection.y(-BigNumber);
					s_new = 3;
					break;

				case 3:
					x_new = projection.x(Math.PI);
					y_new = projection.y(-BigNumber);
					s_new = 0;
					break;

				default:
					/* keep lint happy */
					x_new = y_new = s_new = 0;
				}
				scan(x_0, y_0, x_new, y_new);
				x_0 = x_new;
				y_0 = y_new;
				s0 = s_new;
			}
			scan(x_0, y_0, x_1, y_1);
		};

		void orth_handle_xings() {
			int i,ifrom,ito;
			//qsort(xings, (unsigned) nxings, sizeof(EdgeXing), orth_edgexing_comp);
			Arrays.sort(edgexings,0,anzedgexing,new Comparator<EdgeXing>() {
				@Override
				public int compare(EdgeXing entry1, EdgeXing entry2) {
					if(entry1.angle==entry2.angle) return(0);
					else if(entry1.angle>entry2.angle) return 1;
					else return(-1);
				}
			});
			// Log.d("TAG","We have "+anzedgexing+" Edge crossings.");

			if (edgexings[0].type == XingTypeExit) {
				for (i=0; i<anzedgexing; i+=2) {
					ifrom = i;
					ito   = i+1;

					if((edgexings[ifrom].type != XingTypeExit) || (edgexings[ito].type != XingTypeEntry)) {
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					}
					orth_scan_arc(edgexings[ifrom].x, edgexings[ifrom].y, 
							edgexings[ifrom].angle,
							edgexings[ito].x, edgexings[ito].y,edgexings[ito].angle);
				}
			} else {
				ifrom = anzedgexing-1;
				ito   = 0;

				if ((edgexings[ifrom].type != XingTypeExit) ||
						(edgexings[ito].type != XingTypeEntry) ||
						(edgexings[ifrom].angle < edgexings[ito].angle))
					//  xing_error(__FILE__, __LINE__, nxings-1, nxings, xings);
					Log.d("TAG","ERROR ! xings");
				orth_scan_arc(edgexings[ifrom].x, edgexings[ifrom].y, 
						edgexings[ifrom].angle,
						edgexings[ito].x, edgexings[ito].y, 
						edgexings[ito].angle+(2*Math.PI));

				for (i=1; i<(anzedgexing-1); i+=2) {
					ifrom = i;
					ito   = i+1;

					if ((edgexings[ifrom].type != XingTypeExit) ||
							(edgexings[ito].type != XingTypeEntry))
						// xing_error(__FILE__, __LINE__, i, nxings, xings);
						Log.d("TAG","ERROR ! xings");
					orth_scan_arc(edgexings[ifrom].x, 
							edgexings[ifrom].y, 
							edgexings[ifrom].angle,
							edgexings[ito].x, 
							edgexings[ito].y, edgexings[ito].angle);
				}
			}

			anzedgexing = 0;
		}
		
		void do_dots() {
			if (dots == null) dots = new ScanDot[100000];
			anzdots = 0;
			if (do_stars) new_stars(star_freq);
			if (do_grid) new_grid(grid_big, grid_small);
			//	  qsort(dots->body, dots->count, sizeof(ScanDot), dot_comp);
			if(anzdots>0) {
				Arrays.sort(dots,0,anzdots,new Comparator<ScanDot>() {
					@Override
					public int compare(ScanDot entry1, ScanDot entry2) {
						return(entry1.y-entry2.y);
					}
				});
			}
		}
		void new_stars(final double freq) {
			int      x, y;
			int      max_stars;

			max_stars = (int) (wdth * hght * freq);

			for (int i=0; i<max_stars; i++) {
				x = (int) (Math.random()*wdth);
				y = (int) (Math.random()*hght);

				dots[anzdots]= new ScanDot();
				dots[anzdots].x    = (short) x;
				dots[anzdots].y    = (short) y;
				dots[anzdots].type = DotTypeStar;
				if(anzdots<dots.length-1) anzdots++;
				if ((big_stars>0) && (x+1 < wdth) && ((Math.random()*100) < big_stars)) {
					dots[anzdots]= new ScanDot();
					dots[anzdots].x    = (short) (x+1);
					dots[anzdots].y    = (short) y;
					dots[anzdots].type = DotTypeStar;
					if(anzdots<dots.length-1) anzdots++;
				}
			}
		}

		void new_grid(final int big, final int small) {
			int    i, j;
			int    cnt;
			double lat, lon;
			double lat_scale, lon_scale;
			final double[] cs_lat=new double[2];
			final double[] cs_lon=new double[2];

			/* lines of longitude
			 */
			lon_scale = Math.PI / (2 * big);
			lat_scale = Math.PI / (2 * big * small);
			for (i=(-2*big); i<(2*big); i++) {
				lon       = i * lon_scale;
				cs_lon[0] = Math.cos(lon);
				cs_lon[1] = Math.sin(lon);

				for (j=(-(big*small)+1); j<(big*small); j++) {
					lat       = j * lat_scale;
					cs_lat[0] = Math.cos(lat);
					cs_lat[1] = Math.sin(lat);

					new_grid_dot(cs_lat, cs_lon);
				}
			}

			/* lines of latitude
			 */
			lat_scale = Math.PI / (2 * big);
			for (i=(1-big); i<big; i++) {
				lat       = i * lat_scale;
				cs_lat[0] = Math.cos(lat);
				cs_lat[1] = Math.sin(lat);
				cnt       = 2 * ((int) ((cs_lat[0] * small) + 0.5)) * big;
				lon_scale = Math.PI / cnt;

				for (j=(-cnt); j<cnt; j++) {
					lon       = j * lon_scale;
					cs_lon[0] = Math.cos(lon);
					cs_lon[1] = Math.sin(lon);

					new_grid_dot(cs_lat, cs_lon);
				}
			}
		}

		void new_grid_dot(final double[] cs_lat, final double[] cs_lon) {
			double[]   pos=new double[3];
			pos[0] = cs_lon[1] * cs_lat[0];
			pos[1] = cs_lat[1];
			pos[2] = cs_lon[0] * cs_lat[0];

			pos=viewpos.XFORM_ROTATE(pos);
			// Log.d("TAG"," V=("+pos[0]+","+pos[1]+","+pos[2]);
			if (proj_type == ProjTypeOrthographic) {
				/* if the grid dot isn't visible, return immediately
				 */
				if (pos[2] <= 0) return;
			} else if (proj_type == ProjTypeMercator) {
				/* apply mercator projection
				 */
				pos[0] = Mercator.x(pos[0], pos[2]);
				pos[1] = Mercator.y(pos[1]);
			} else /* (proj_type == ProjTypeCylindrical) */ {
				/* apply cylindrical projection
				 */
				pos[0] = Cylindric.x(pos[0], pos[2]);
				pos[1] = Cylindric.y(pos[1]);
			}

			final int x = (int) projection.x(pos[0]);
			final int y = (int) projection.y(pos[1]);
			// Log.d("TAG"," xy=("+x+","+y+")");
			if ((x >= 0) && (x < wdth) && (y >= 0) && (y < hght)) {
				dots[anzdots]=new ScanDot();
				dots[anzdots].x    = (short) x;
				dots[anzdots].y    = (short) y;
				dots[anzdots].type = DotTypeGrid;
				if(anzdots<dots.length-1) anzdots++;
			} else Log.d("TAG","x="+x+"/"+wdth+" y="+y+"/"+hght);
		}

		double[] orth_compute_inv_x() {
			final double[] inv_x=new double[wdth];
			int i_lim;

			i_lim = wdth;
			for (int i=0; i<i_lim; i++)
				inv_x[i] = projection.inv_x(i);
			return inv_x;
		}


		/*  Bild zusammensetzen */

		void render(final Canvas c) {
			int scanbuf[];
			byte row[];
			int     i_lim;
			double[] inv_x;
			double[] sol=new double[3];
			double  tmp;

			inv_x = null;

			if (do_shade) {
				/* inv_x[] only gets used with orthographic projection
				 */
				if (proj_type == ProjTypeOrthographic) inv_x=orth_compute_inv_x();

				sol=sun.vector(viewpos);

				/* precompute shading parameters
				 */
				night_val     = (int) (night * (255.99/100.0));
				tmp           = terminator / 100.0;
				day_val_base  = (((tmp * day) + ((1-tmp) * night))  * (255.99/100.0));
				day_val_delta = (day * (255.99/100.0)) - day_val_base;
			}

			/* main render loop */
			dotcnt=0;
			scanbitcnt=0;

			i_lim = hght; /* (use i_lim to encourage compilers to register loop limit)*/
			for (int i=0; i<i_lim; i++) {
				scanbuf=render_next_row(i);

				if (!do_shade)
					row=no_shade_row(scanbuf);
				else if (proj_type == ProjTypeOrthographic)
					row=orth_shade_row(i, scanbuf, sol, inv_x);
				else if (proj_type == ProjTypeMercator)
					row=merc_shade_row(i, scanbuf, sol);
				else /* (proj_type == ProjTypeCylindrical) */
					row=cyl_shade_row(i, scanbuf, sol);

				x11_row(row,i,c);  /*Diese Zeile jetzt auf Bildschirm darstellen*/
			}
		}

		byte[] cyl_shade_row(final int idx, final int[] scanbuf, final double[] sol) {
			final byte[] rslt=new byte[wdth*3];
			final int    i_lim=wdth;
			int    scanbuf_val;
			int    val;
			double sin_theta;
			double cos_theta;
			double scale;
			double y_sol_1;

			final double y = Cylindric.inv_y(projection.inv_y(idx));

			/* conceptually, on each iteration of the i loop, we want:
			 *
			 *   x = sin(INV_XPROJECT(i)) * sqrt(1 - (y*y));
			 *   z = cos(INV_XPROJECT(i)) * sqrt(1 - (y*y));
			 *
			 * computing this directly is rather expensive, however, so we only
			 * compute the first (i=0) pair of values directly; all other pairs
			 * (i>0) are obtained through successive rotations of the original
			 * pair (by inv_proj_scale radians).
			 */

			/* compute initial (x, z) values
			 */
			double tmp = Math.sqrt(1 - (y*y));
			double x   = Math.sin(projection.inv_x(0)) * tmp;
			double z   = Math.cos(projection.inv_x(0)) * tmp;

			/* compute rotation coefficients used
			 * to find subsequent (x, z) values
			 */
			tmp = projection.inv_proj_scale;
			sin_theta = Math.sin(tmp);
			cos_theta = Math.cos(tmp);

			/* save a little computation in the inner loop
			 */
			y_sol_1 = y * sol[1];

			/* use i_lim to encourage compilers to register loop limit
			 */
			
			for (int i=0; i<i_lim; i++) {
				scanbuf_val = scanbuf[i];

				switch (scanbuf_val) {
				case Map.PixTypeSpace:        /* black */
				case Map.PixTypeStar:         /* white */
				case Map.PixTypeGridLand:
				case Map.PixTypeGridWater:
					rslt[0+3*i] = (byte) Color.red(scanbuf_val);
					rslt[1+3*i] = (byte) Color.green(scanbuf_val);
					rslt[2+3*i] = (byte) Color.blue(scanbuf_val);
					break;
				default:
					scale = (x * sol[0]) + y_sol_1 + (z * sol[2]);
					if (scale < 0) {
						val = night_val;
					} else  {
						val = (int) (day_val_base + (scale * day_val_delta));
						if (val > 255)  val = 255;
					}
					rslt[0+3*i] = (byte) (Color.red(scanbuf_val)*val/255);
					rslt[1+3*i] = (byte) (Color.green(scanbuf_val)*val/255);
					rslt[2+3*i] = (byte) (Color.blue(scanbuf_val)*val/255);
					break;
				}

				/* compute next (x, z) values via 2-d rotation
				 */
				tmp = (cos_theta * z) - (sin_theta * x);
				x   = (sin_theta * z) + (cos_theta * x);
				z   = tmp;
			}
			return rslt;
		}

		byte[] merc_shade_row(final int idx, final int[] scanbuf, final double[] sol) {
			final byte[] rslt=new byte[wdth*3];
			final int    i_lim= wdth;
			int    scanbuf_val;
			int    val;
			double sin_theta;
			double cos_theta;
			double scale;
			double y_sol_1;

			final double y = Mercator.inv_y(projection.inv_y(idx));

			/* conceptually, on each iteration of the i loop, we want:
			 *
			 *   x = sin(INV_XPROJECT(i)) * sqrt(1 - (y*y));
			 *   z = cos(INV_XPROJECT(i)) * sqrt(1 - (y*y));
			 *
			 * computing this directly is rather expensive, however, so we only
			 * compute the first (i=0) pair of values directly; all other pairs
			 * (i>0) are obtained through successive rotations of the original
			 * pair (by inv_proj_scale radians).
			 */

			/* compute initial (x, z) values
			 */
			double tmp = Math.sqrt(1 - (y*y));
			double x   = Math.sin(projection.inv_x(0)) * tmp;
			double z   = Math.cos(projection.inv_x(0)) * tmp;

			/* compute rotation coefficients used
			 * to find subsequent (x, z) values
			 */
			tmp = projection.inv_proj_scale;
			sin_theta = Math.sin(tmp);
			cos_theta = Math.cos(tmp);

			/* save a little computation in the inner loop
			 */
			y_sol_1 = y * sol[1];

			/* use i_lim to encourage compilers to register loop limit
			 */
			for (int i=0; i<i_lim; i++) {
				scanbuf_val = scanbuf[i];

				switch (scanbuf_val) {
				case Map.PixTypeSpace:        /* black */
				case Map.PixTypeStar:         /* white */
				case Map.PixTypeGridLand:
				case Map.PixTypeGridWater:
					rslt[0+3*i] = (byte) Color.red(scanbuf_val);
					rslt[1+3*i] = (byte) Color.green(scanbuf_val);
					rslt[2+3*i] = (byte) Color.blue(scanbuf_val);
					break;
				default:
					scale = (x * sol[0]) + y_sol_1 + (z * sol[2]);
					if (scale < 0) {
						val = night_val;
					} else  {
						val = (int) (day_val_base + (scale * day_val_delta));
						if (val > 255) val = 255;
					}
					rslt[0+3*i] = (byte) (Color.red(scanbuf_val)*val/255);
					rslt[1+3*i] = (byte) (Color.green(scanbuf_val)*val/255);
					rslt[2+3*i] = (byte) (Color.blue(scanbuf_val)*val/255);
					break;
				}

				/* compute next (x, z) values via 2-d rotation
				 */
				tmp = (cos_theta * z) - (sin_theta * x);
				x   = (sin_theta * z) + (cos_theta * x);
				z   = tmp;
			}
			return rslt;
		}

		/*Endgueltig render auf der Zeile fuer shade/orth, ergebnis sind rgb-werte*/

		byte[] orth_shade_row(final int idx, final int[] scanbuf, final double[] sol, final double[] inv_x) {
			final byte[] rslt=new byte[wdth*3];
			final int    i_lim= wdth;
			int    scanbuf_val;
			double    val;
			double x, z;
			double scale;
			double tmp;
		
			final double y = projection.inv_y(idx);

			/* save a little computation in the inner loop
			 */
			tmp     = 1 - (y*y);
			final double y_sol_1 = y * sol[1];

			/* use i_lim to encourage compilers to register loop limit
			 */
		
			for (int i=0; i<i_lim; i++) {
				scanbuf_val = scanbuf[i];

				switch (scanbuf_val) {
				case Map.PixTypeSpace:        /* black */
				case Map.PixTypeStar:         /* white */
				case Map.PixTypeGridLand:
				case Map.PixTypeGridWater:

					rslt[0+3*i] = (byte) Color.red(scanbuf_val);
					rslt[1+3*i] = (byte) Color.green(scanbuf_val);
					rslt[2+3*i] = (byte) Color.blue(scanbuf_val);
					break;
				default:
					x = inv_x[i];
					z = tmp - (x*x);
					if(z>=0) z=Math.sqrt(z);
					else z = 0;
					scale = (x * sol[0]) + y_sol_1 + (z * sol[2]);
					if (scale < 0)  {
						val = night_val;
					} else {
						val =  (day_val_base + (scale * day_val_delta));
						if (val > 255) val = 255;
						//					Log.d("TAG","scale="+scale+" delta="+day_val_delta+" val="+val);
					}
					rslt[0+3*i] = (byte) (Color.red(scanbuf_val)*val/255);
					rslt[1+3*i] = (byte) (Color.green(scanbuf_val)*val/255);
					rslt[2+3*i] = (byte) (Color.blue(scanbuf_val)*val/255);
					break;
				}


			}
			return rslt;
		}

		/*Endgueltig render auf der Zeile fuer noshade, ergebnis sind rgb-werte*/

		byte[] no_shade_row(final int[] scanbuf) {
			final byte[] rslt=new byte[wdth*3];
			for (int i=0; i<wdth; i++) {
				rslt[0+3*i] = (byte) Color.red(scanbuf[i]);
				rslt[1+3*i] = (byte) Color.green(scanbuf[i]);
				rslt[2+3*i] = (byte) Color.blue(scanbuf[i]);
			}
			return rslt;
		}


		void inverse_project(final int y, final int x) {
			final double ix = projection.inv_x(x);
			final double iy = projection.inv_y(y);
			double[] q=new double[3];
			double t;

			if (proj_type == ProjTypeOrthographic) {
				q[0] = ix;
				q[1] = iy;
				q[2] = Math.sqrt(1 - (ix*ix + iy*iy));
			} else if (proj_type == ProjTypeMercator) {
				q[1] = Mercator.inv_y(iy);
				t = Math.sqrt(1 - q[1]*q[1]);
				q[0] = Math.sin(ix) * t;
				q[2] = Math.cos(ix) * t;
			} else /* (proj_type == ProjTypeCylindrical) */
			{
				q[1] = Cylindric.inv_y(iy);
				t = Math.sqrt(1 - q[1]*q[1]);
				q[0] = Math.sin(ix) * t;
				q[2] = Math.cos(ix) * t;
			}
			/* inverse of XFORM_ROTATE */
			{
				double _p0_, _p1_, _p2_;
				double _c_, _s_, _t_;
				_p0_ = q[0];
				_p1_ = q[1];
				_p2_ = q[2];
				_c_ = viewpos.cos_rot;
				_s_ = -viewpos.sin_rot;
				_t_ = (_c_ * _p0_) - (_s_ * _p1_);
				_p1_ = (_s_ * _p0_) + (_c_ * _p1_);
				_p0_ = _t_;
				_c_ = viewpos.cos_lat;
				_s_ = -viewpos.sin_lat;
				_t_ = (_c_ * _p1_) - (_s_ * _p2_);
				_p2_ = (_s_ * _p1_) + (_c_ * _p2_);
				_p1_ = _t_;
				_c_ = viewpos.cos_lon;
				_s_ = -viewpos.sin_lon;
				_t_ = (_c_ * _p0_) - (_s_ * _p2_);
				_p2_ = (_s_ * _p0_) + (_c_ * _p2_);
				_p0_ = _t_;
				q[0] = _p0_;
				q[1] = _p1_;
				q[2] = _p2_;
			}
			//Log.d("OvO","lat="+tmp_lat+" lon="+tmp_lon);
			tmp_lat = Math.asin(q[1]);
			tmp_lon = Math.atan2(q[0], q[2]);
		}

		/*Ergebnis sind pixel-Werte fuer eine Zeile (noch nicht geshaded)*/
		int[] render_next_row( final int idx) {
			final int[] buf=new int[wdth];
			int      tmp;
			int i_lim;
			for(int i=0;i<wdth;i++) buf[i]=0;


			if(!do_bitmap || bmp==null) {
				/*Scanbits für diese Zeile aufaddieren*/
				while(scanbitcnt<anzscanbits && scanbits[scanbitcnt].y == idx) {
					i_lim = scanbits[scanbitcnt].hi_x;/* use i_lim to encourage compilers to register loop limit*/

					tmp   = scanbits[scanbitcnt].val;

					for (int i=scanbits[scanbitcnt].lo_x; i<=i_lim; i++) buf[i] += tmp;
					scanbitcnt++;
				}
				/*Werte in buf heissen:
				 * 0 = Space, Hintergrund
				 * 1-64=Wasser
				 * >64 =Land
				 * */

				/*Jetzt mache Farbwerte draus...*/

				i_lim = wdth;/* use i_lim to encourage compilers to register loop limit*/
				for(int i=0; i<i_lim; i++)  buf[i] = Map.scan_to_pix[(int) (buf[i] & 0xff)];

			} else {
				// Aus der Bitmap rendern....
				for (int i=0; i<wdth; i++) {
					inverse_project(idx, i);
					if(tmp_lat>-Math.PI && tmp_lat<Math.PI) {
						buf[i] = bmp.get_pixel(tmp_lat, tmp_lon)&0xffffff;
					}
				}
			}
			/*Jetzt noch Punkte hinzufuegen...*/


			while(dotcnt<anzdots && dots[dotcnt].y == idx) {
				tmp = dots[dotcnt].x;
				if (dots[dotcnt].type == DotTypeStar) {
					if (buf[tmp] == Map.PixTypeSpace)
						buf[tmp] = Map.PixTypeStar;
				} else {
					switch (buf[tmp]) {
					case Map.PixTypeLand:
						buf[tmp] = Map.PixTypeGridLand;
						break;
					case Map.PixTypeWater:
						buf[tmp] = Map.PixTypeGridWater;
						break;
					default:
						buf[tmp] = Map.PixTypeGridWater;
						break;
					}
				}
				dotcnt++;
			}
			/* Falls gewuenscht overlay hinzufuegen*/
			if(do_overlay && ovl!=null) {
				for (int i=0; i<wdth; i++)  {
					if(buf[i]!=Map.PixTypeSpace && (buf[i]&0xff000000)==0) {
						inverse_project(idx, i);
						buf[i] = ovl.get_pixel(tmp_lat, tmp_lon, buf[i]);
					}
				}
			}
			return buf;
		}
		/*Eine Zeile malen*/
		void x11_row(byte[] row,int y, Canvas c) {
			for(int x=0;x<wdth;x++) {
				mPaint.setARGB(255, row[3*x]&0xff, row[3*x+1]&0xff, row[3*x+2]&0xff);
				c.drawPoint(x, y, mPaint);
			}
		} 

		
		void draw_label(final Canvas c) {
			final int         dy=12;
			int         x, y;
			final int   label_xvalue=5;      /* label x position    */
			final int   label_yvalue=40;      /* label y position    */
			final Calendar cal = Calendar.getInstance();
			String buf=cal.getTime().toString();
			x=label_xvalue;
			y = label_yvalue;

			draw_outlined_string(c,Color.WHITE,Color.BLACK, x, y, buf);
			y += dy;

			buf=String.format("view %.1f %c %.1f %c",
					Math.abs(viewpos.view_lat), ((viewpos.view_lat < 0) ? 'S' : 'N'),
					Math.abs(viewpos.view_lon), ((viewpos.view_lon < 0) ? 'W' : 'E'));

			draw_outlined_string( c,Color.WHITE,Color.BLACK, x, y, buf);
			y += dy;

			buf=String.format("sun %.1f %c %.1f %c",
					Math.abs(sun.lat), ((sun.lat < 0) ? 'S' : 'N'),
					Math.abs(sun.lon), ((sun.lon < 0) ? 'W' : 'E'));

			draw_outlined_string(c, Color.WHITE, Color.BLACK, x, y, buf);
			y += dy;
		}


		void mark_location(Map.MarkerInfo info, final Canvas c) {
			int         x, y;
			double      lat, lon;
			double[]    pos=new double[3];
			String       text;

			// Log.d("TAG","Mark-Location "+info.label);

			lat = info.lat * (Math.PI/180);
			lon = info.lon * (Math.PI/180);

			pos[0] = Math.sin(lon) * Math.cos(lat);
			pos[1] = Math.sin(lat);
			pos[2] = Math.cos(lon) * Math.cos(lat);

			pos=viewpos.XFORM_ROTATE(pos);

			if (proj_type == ProjTypeOrthographic) {
				/* if the marker isn't visible, return immediately
				 */
				if (pos[2] <= 0) return;
			} else if (proj_type == ProjTypeMercator) {
				/* apply mercator projection
				 */
				pos[0] = Mercator.x(pos[0], pos[2]);
				pos[1] = Mercator.y(pos[1]);
			} else if  (proj_type == ProjTypeCylindrical) {

				/* apply cylindrical projection
				 */
				pos[0] = Cylindric.x(pos[0], pos[2]);
				pos[1] = Cylindric.y(pos[1]);
			}

			x = (int) projection.x(pos[0]);
			y = (int) projection.y(pos[1]);

			mPaint.setColor(Color.BLACK);
			c.drawCircle(x,y,4, mPaint);
			c.drawCircle(x,y,2, mPaint);
			mPaint.setColor(Color.RED);
			mPaint.setAntiAlias(true);
			c.drawCircle(x,y,3, mPaint);

			text = info.label;
			if (text != null) draw_outlined_string(c,Color.RED,Color.BLACK, x+6, y+3, text);

			mPaint.setColor(Color.WHITE);
			mPaint.setAntiAlias(false);
		}


		void x11_cleanup(final Canvas c) {
			if (do_markers) {
				for(int i=0;i<map.anzmarkers;i++) 
					mark_location(map.marker_info[i],c);
			}
			if (do_label) draw_label(c);
		}	
		void draw_outlined_string(final Canvas c,final int fg, final int bg, final int x, final int y, final String text) {
			mPaint.setColor(bg);
			mPaint.setAntiAlias(false);
			c.drawText(text,x+1,y,mPaint);
			c.drawText(text,x-1,y,mPaint);
			c.drawText(text,x,y+1,mPaint);
			c.drawText(text,x,y-1,mPaint);
			mPaint.setColor(fg);
			mPaint.setAntiAlias(true);
			c.drawText(text,x,y,mPaint);
			mPaint.setAntiAlias(false);
		}
	}
}
