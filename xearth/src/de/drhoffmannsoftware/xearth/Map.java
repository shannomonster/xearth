package de.drhoffmannsoftware.xearth;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

/* Load Map-Data and Markers from files (either from assets or from sdcard)*/


public class Map {
	final static String TAG="Map";
	final static int[] data=new int[53320];
	private static boolean mapdataisloaded=false;
	AssetManager assetManager;
	private static short readShort(InputStream in) throws IOException {
		return (short)(in.read() | (in.read() << 8));
	}
	private int anzbmarkers=0;
	public int anzmarkers=0;
	/* types of marker label alignment
	 */
	final static int  MarkerAlignDefault=(0);
	final static int  MarkerAlignLeft   =(1);
	final static int  MarkerAlignRight  =(2);
	final static int  MarkerAlignAbove  =(3);
	final static int  MarkerAlignBelow  =(4);

	class MarkerInfo {
		double lat;
		double lon;
		String label;
		short   align;
	} 
	static int[]   scan_to_pix=null;

	/* types of pixels */
	final static int PixTypeSpace    =(0x000000);
	final static int PixTypeLand     =(0x00ff00);
	final static int PixTypeWater    =(0x0000ff);
	final static int PixTypeStar     =(0x1ffffff);
	final static int PixTypeGridLand =(0x2ffffff);
	final static int PixTypeGridWater=(0x3ffffff);

	MarkerInfo[] marker_info=new MarkerInfo[1000];

	Map(final AssetManager amgr) {
		assetManager=amgr;
		if(!mapdataisloaded) load_mapdata();
		if(anzbmarkers==0) load_builtin_markers();
		anzmarkers=anzbmarkers;
		// load_user_markers();
		if(scan_to_pix==null) render_rows_setup();
		Log.d("TAG","Length="+data.length+" Mappoints and "+anzmarkers+" Builtin Markers.");
	}
	/* Erstellt Umrechnungstabelle */
	private void render_rows_setup() {
		scan_to_pix=new int[256];
		/* precompute table for translating between
		 * scan buffer values and pixel types
		 */
		for (int i=0; i<256; i++)
			if (i == 0)
				scan_to_pix[i] = PixTypeSpace;
			else if (i > 64)
				scan_to_pix[i] = PixTypeLand;
			else
				scan_to_pix[i] = PixTypeWater;
	}
	private void load_mapdata() {
		InputStream in = null;
		Log.d(TAG,"load mapdata...");
		try {
			in = assetManager.open("mapdata.dat");
			for(int i=0; i<data.length;i++) {
				data[i]=readShort(in);
			}
			in.close();
			mapdataisloaded=true;
			in = null;
		} catch(Exception e) {
			Log.e(TAG, e.toString());
		}
	}
	private MarkerInfo process_markerline(String l) {
		MarkerInfo ret=new MarkerInfo();
		String[] sep=l.split("#");
		l=sep[0].trim();
		l=l.replace("\t", " ");
		l=l.replace("  ", " ");
		l=l.replace("  ", " ");
		l=l.replace("  ", " ");
		sep=l.split(" ");
		if(sep.length<3) return null;
		ret.lat=(float) Double.parseDouble(sep[0]);
		ret.lon=(float) Double.parseDouble(sep[1]);
		sep=l.split("\"");
		if(sep.length>=2) ret.label=sep[1];
		else ret.label="";
		ret.align=0;
		return ret;
	}

	private void load_builtin_markers() {
		InputStream in = null;
		Log.d(TAG,"load builtin markers...");
		try {
			in = assetManager.open("builtin_marker_data.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while( ( line = reader.readLine() ) != null) {
				if(anzmarkers<marker_info.length) marker_info[anzmarkers++]=process_markerline(line);
			}
			reader.close();
			in.close();
			anzbmarkers=anzmarkers;
			in = null;
		} catch(Exception e) {
			Log.e(TAG, e.toString());
		}
	}
	public void reload_user_markers() {
		load_user_markers();
	}

	private void load_user_markers() {
		File file = new File(Environment.getExternalStorageDirectory()+"/xearth/markers.txt"); 
		if(file.exists()) {
			InputStream in = null;
			Log.d(TAG,"load user markers...");
			try {
				in =  new FileInputStream(file); 
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				anzmarkers=0; //Damit überschreibt er die internen
				while( ( line = reader.readLine() ) != null) {
					line=line.trim();
					if(line.length()>0 && !line.startsWith("#")) {
						if(anzmarkers<marker_info.length) {
							MarkerInfo a=process_markerline(line);
							if(a!=null) marker_info[anzmarkers++]=a;
						}
					}
				}
				reader.close();
				in.close();
				in = null;
			} catch(Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}
}
