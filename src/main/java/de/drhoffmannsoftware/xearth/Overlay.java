package de.drhoffmannsoftware.xearth;

/* Overlay.java      (c) 2011-2015 by Markus Hoffmann and 
 *                   (C) 1997-2006 by Greg Hewgill
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

public class Overlay {
	private Bitmap ovl=null;
	private int w,h;
	Overlay(Context context,int drawableId) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		//Wolken brauchen keine feine Farbschattierung
//		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		//Die internen Wolken sind nur schwarz/weiss
		opts.inPreferredConfig = Bitmap.Config.ALPHA_8;
		opts.inInputShareable=true;
		opts.inPurgeable=true;
		ovl=BitmapFactory.decodeResource(context.getResources(), drawableId, opts);
		
//		ovl=getBitmapFromDrawableId(context,drawableId);
		w=ovl.getWidth();
		h=ovl.getHeight();
	}
	Overlay(final String filename) {
		File imgFile = new  File(filename);
		if(imgFile.exists()) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			//Wolken brauchen keine feine Farbschattierung
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			ovl = BitmapFactory.decodeFile(imgFile.getAbsolutePath(),opts);	
			w=ovl.getWidth();
			h=ovl.getHeight();
		}
	}
	public void recycle() {
		if(ovl!=null) ovl.recycle();
		ovl=null;
	}
	
	public int get_pixel(final double lat,final double lon,final int in) { 
		if(lon<-Math.PI || lon>Math.PI) return in;
		if(lat<-Math.PI/2 || lat>Math.PI/2) return in;
		final int x = (int) ((lon + Math.PI) * w / (2*Math.PI));
		final int y = (int) (-lat * h / Math.PI + h/2);

		int a=ovl.getPixel(x,y);
		int r=Color.red(in);
		int g=Color.green(in);
		int b=Color.blue(in);
		
		r=r+Color.red(a)*(255-r)/255;
		g=g+Color.green(a)*(255-g)/255;
		b=b+Color.blue(a)*(255-b)/255;
		return Color.argb(0,r,g,b);
	}
}
