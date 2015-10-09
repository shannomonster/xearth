package de.drhoffmannsoftware.xearth;

/* BitMap.java    (c) 2011 by Markus Hoffmann
 *
 * This file is part of Xearth live Wallpaper for Android 
 * ==================================================================
 * Xearth live Wallpaper for Android is free software and comes with 
 * NO WARRANTY - read the file COPYING/LICENSE for details
 */

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;

/* Use internal bitmaps (from drawable) or external (from sdcard/xearth) */

public class BitMap {
	private Bitmap ovl=null;
	private int w,h;
	BitMap(final Context context,final int drawableId) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		//Unsere Internen Maps brauchen keine feine Farbschattierung
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		opts.inInputShareable=true;
		opts.inPurgeable=true;
	//	opts.inSampleSize=8;
		ovl=BitmapFactory.decodeResource(context.getResources(), drawableId, opts);		
		w=ovl.getWidth();
		h=ovl.getHeight();
	}
	BitMap(final String filename) {
		File imgFile = new  File(Environment.getExternalStorageDirectory()+"/xearth/"+filename);
		if(imgFile.exists()) {
			ovl = BitmapFactory.decodeFile(imgFile.getAbsolutePath());	
			w=ovl.getWidth();
			h=ovl.getHeight();
		}
	}
	public void recycle() {
		if(ovl!=null) ovl.recycle();
		ovl=null;
	}
	public int get_pixel(final double lat,final double lon) { 
		if(lon<-Math.PI || lon>Math.PI) return Color.BLACK;
		if(lat<-Math.PI/2 || lat>Math.PI/2) return Color.BLACK;
		int x = (int) ((lon + Math.PI) * w / (2*Math.PI));
		int y = (int) (-lat * h / Math.PI + h/2);
		if(ovl!=null) return ovl.getPixel(x,y);
		else return Color.MAGENTA;
	}
	
	/**
	 * This method returns a bitmap related to resource id.
	 * 
	 * @param context Context of calling activity
	 * @param drawableId Resource ID of bitmap drawable
	 * @return Bitmap whose resource id was passed to method.
	 */
	public static Bitmap getBitmapFromDrawableId(Context context,int drawableId){
	    Bitmap bitmap = null;
	    try {
	        BitmapDrawable drawable = (BitmapDrawable)context.getResources().getDrawable(drawableId);
	        bitmap = drawable.getBitmap();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return bitmap;
	}
}
