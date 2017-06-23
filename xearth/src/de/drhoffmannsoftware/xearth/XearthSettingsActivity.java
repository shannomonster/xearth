package de.drhoffmannsoftware.xearth;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/* This file is part of Xearth Wallpaper, the xearth android live background 
 * =========================================================================
 * xearth is free software and comes with NO WARRANTY - read the file
 * COPYING for details
 */

public class XearthSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	final static String TAG="xearthconfig";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.xearth_settings);

		findPreference("about_version").setSummary(applicationVersion());
		findPreference("about_credits_xearth").setSummary(XearthWallpaper.HomePageURL);
		for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
			initSummary(getPreferenceScreen().getPreference(i));
		}

		//   getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();		
		loadFileList();
		ListPreference listPreferenceCategory = (ListPreference) findPreference("prefs_basemap");
		if (listPreferenceCategory != null) {
			CharSequence entries[] = new String[mFileList.length+3];
			CharSequence entryValues[] = new String[mFileList.length+3];
			entries[0]="default xearth coastlines green/blue";
			entryValues[0]="0";
			entries[1]="earth USGS topo + land/ocean";
			entryValues[1]="1";
			entries[2]="earth night-electric";
			entryValues[2]="2";
			for(int i=0;i<mFileList.length;i++) {
				entries[3+i]=mFileList[i];
				entryValues[3+i]=mFileList[i];
			}
			listPreferenceCategory.setEntries(entries);
			listPreferenceCategory.setEntryValues(entryValues);
		}
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes             
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     

	}
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));
	}
	private static String[] mFileList;

	private void loadFileList() {
		//	File mConfigPath= new File(getApplicationContext().getFilesDir().getAbsolutePath());
		File mConfigPath = new File(Environment.getExternalStorageDirectory()+"/xearth"); 
		if(mConfigPath.exists()){
			FilenameFilter filter = new FilenameFilter(){
				public boolean accept(File dir, String filename){
					File sel = new File(dir, filename);
					return (filename.endsWith(".jpg") || filename.endsWith(".png")) && !sel.isDirectory();
				}
			};
			mFileList = mConfigPath.list(filter);
		} else {
			Log.d(TAG,"Path not found!");
			mFileList= new String[0];
		}
	}

	@Override
	public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference)  {
		final String key = preference.getKey();
		if ("about_version".equals(key)) {
			showDialog(4);
		} else if ("about_copyright".equals(key)) {
			showDialog(0);
		} else if ("about_help".equals(key)) {
			showDialog(1);
		} else if ("about_credits_xearth".equals(key)) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(XearthWallpaper.HomePageURL)));
		} else if ("about_market_app".equals(key)) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("market://details?id=%s", getPackageName()))));
			finish();
		} else if ("about_market_publisher".equals(key))  {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:\"Markus Hoffmann\"")));
			finish();
		}
		return false;
	}

	private Dialog scrollableDialog(final String title, final String text) {
		final Dialog dialog = new Dialog(this);
		if(title==null || title=="") dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		else dialog.setTitle(title);

		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.setContentView(R.layout.maindialog);
		final TextView wV= (TextView) dialog.findViewById(R.id.TextView01);
		wV.setText(Html.fromHtml(text));
		//set up button
		final Button button = (Button) dialog.findViewById(R.id.Button01);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		return dialog;
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog = null;
		if(id==4) {
			dialog = new Dialog(XearthSettingsActivity.this);
			final TextView wV = new TextView(this);
			String t=getResources().getString(R.string.impressum);;
			wV.setText(Html.fromHtml(t));
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(wV);
			dialog.setCanceledOnTouchOutside(true);
		} else if(id==1) {
			dialog=scrollableDialog("",getResources().getString(R.string.helpdialog));
		} else if(id==0) {
			dialog=scrollableDialog("",getResources().getString(R.string.aboutdialog)+
					getResources().getString(R.string.impressum));
		}   
		return dialog;
	}


	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory)p;
			for(int i=0;i<pCat.getPreferenceCount();i++) {
				initSummary(pCat.getPreference(i));
			}
		} else { updatePrefSummary(p); }
	}

	private void updatePrefSummary(Preference p){
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p; 
			p.setSummary(listPref.getEntry()); 
		}
		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p; 
			p.setSummary(editTextPref.getText()); 
		}
	}

	private final String applicationVersion() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException x)  {
			return "unknown";
		}
	}
}