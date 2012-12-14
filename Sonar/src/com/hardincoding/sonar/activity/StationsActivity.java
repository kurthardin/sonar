package com.hardincoding.sonar.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.subsonic.service.SubsonicMusicService;
import com.hardincoding.sonar.util.Util;

public class StationsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stations);
		
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences prefs = Util.getPreferences(this);
		String server = prefs.getString(Util.PREFERENCES_KEY_SERVER, null);
	    String username = prefs.getString(Util.PREFERENCES_KEY_USERNAME, null);
	    String password = prefs.getString(Util.PREFERENCES_KEY_PASSWORD, null);
	    
	    if (server == null || username == null || password == null) {
	    	Intent loginIntent = new Intent(this, LoginActivity.class);
	    	startActivity(loginIntent);
	    	finish();
	    } else {
	    	SubsonicMusicService.INSTANCE.setServerAddress(server);
	    	SubsonicMusicService.INSTANCE.setCredentials(username, password);
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_stations, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_logout) {
			
			Util.getPreferences(this).edit()
			.remove(Util.PREFERENCES_KEY_SERVER)
			.remove(Util.PREFERENCES_KEY_USERNAME)
			.remove(Util.PREFERENCES_KEY_PASSWORD)
			.apply();
			
			Intent loginIntent = new Intent(this, LoginActivity.class);
			startActivity(loginIntent);
			finish();
			return true;
		}
		return false;
	}
	
}
