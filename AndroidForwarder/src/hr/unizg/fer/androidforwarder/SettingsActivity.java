package hr.unizg.fer.androidforwarder;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final String PREFERENCE_MESSAGE_SOURCE = "pref_key_source_list";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
	}

	
	@Override
	protected void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	    
	    Preference p = findPreference(PREFERENCE_MESSAGE_SOURCE);
	    if( p!= null ) {
	    	
	    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
	    	String val = sp.getString(PREFERENCE_MESSAGE_SOURCE, "???");
	    
	    	p.setSummary(val);
	    }
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		//update DataSource choice, if changed
		if( key.equals(PREFERENCE_MESSAGE_SOURCE) ) {
			
			Preference p = findPreference(key);
			p.setSummary(sharedPreferences.getString(key, ""));
		}
		
	}
}
