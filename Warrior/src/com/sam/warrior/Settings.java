package com.sam.warrior;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity 
{
	private static final boolean OPT_MUSIC_DEF =true;


	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.settings);
	
	}
	
	public static boolean getMusic(Context context) 
	{
		return PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean("sound", OPT_MUSIC_DEF);
		}

}
