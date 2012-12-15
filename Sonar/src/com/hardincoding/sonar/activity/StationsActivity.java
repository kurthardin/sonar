package com.hardincoding.sonar.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.domain.Station;
import com.hardincoding.sonar.subsonic.service.CachedMusicService;
import com.hardincoding.sonar.subsonic.service.SubsonicMusicService;
import com.hardincoding.sonar.util.Util;

import de.umass.lastfm.Caller;
import de.umass.lastfm.cache.FileSystemCache;

public class StationsActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stations);
		
		Caller.getInstance().setUserAgent("Sonar");
		Caller.getInstance().getLogger().setLevel(Level.ALL); // Set debug level
		Caller.getInstance().setCache(new FileSystemCache(getCacheDir()));
		
		List<Station> stations = new ArrayList<Station>();
		stations.add(new Station("Create Station"));
		// TODO Add the stations
		setListAdapter(new StationAdapter(this, stations));
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
	    	
	    	CachedMusicService.INSTANCE.updateCache(this);
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_logout) {
			
			Util.getPreferences(this).edit()
			.remove(Util.PREFERENCES_KEY_SERVER)
			.remove(Util.PREFERENCES_KEY_USERNAME)
			.remove(Util.PREFERENCES_KEY_PASSWORD)
			.putBoolean(Util.PREFERENCES_KEY_FIRST_CACHE_FLAG, true)
			.apply();
			
			CachedMusicService.INSTANCE.deleteCache(this);
			
			Intent loginIntent = new Intent(this, LoginActivity.class);
			startActivity(loginIntent);
			finish();
			return true;
		}
		return false;
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		if (position == 0) {
			startActivity(new Intent(this, CreateStationActivity.class));
		}
	}
	
	
	public class StationAdapter extends ArrayAdapter<Station> {
		LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	    public StationAdapter(Context context, List<Station> values) {
	        super(context, R.layout.list_item_station, values);
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	View v = convertView;
	    	if (v == null) {
	    		v = mInflater.inflate(R.layout.list_item_station, parent, false);
	    	}
	        
	    	Station item = getItem(position);
	    	if (item != null) {
	    		TextView textView = (TextView) v.findViewById(R.id.text1);
	    		if (textView != null) {
	    			textView.setText(item.getName());
	    		}
	    	}
	        return v;
	    }
	}

}
