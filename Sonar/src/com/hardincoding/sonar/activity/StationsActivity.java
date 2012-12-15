package com.hardincoding.sonar.activity;

import java.util.List;
import java.util.logging.Level;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
	    
		setContentView(R.layout.activity_stations);
		
		Caller.getInstance().setUserAgent("Sonar");
		Caller.getInstance().getLogger().setLevel(Level.ALL); // Set debug level
		Caller.getInstance().setCache(new FileSystemCache(getCacheDir()));
		
		registerForContextMenu(getListView());
		setListAdapter(new StationAdapter(this, CachedMusicService.INSTANCE.getStations(this)));
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((StationAdapter) getListAdapter()).notifyDataSetChanged();
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (((AdapterContextMenuInfo) menuInfo).id == 0) {
			return;
		}
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.context_activity_stations, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	        case R.id.menu_remove_station:
	        	int idx = (int) info.id - 1;
	            CachedMusicService.INSTANCE.getStations(this).remove(idx);
	            CachedMusicService.INSTANCE.writeStations(this);
	            ((StationAdapter) getListAdapter()).notifyDataSetChanged();
	            return true;
	        default:
	            return super.onContextItemSelected(item);
	    }
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		if (position == 0) {
			startActivity(new Intent(this, CreateStationActivity.class));
		} else {
			// TODO Start playing station at position - 1
		}
	}
	
	
	public class StationAdapter extends ArrayAdapter<Station> {
		LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	    public StationAdapter(Context context, List<Station> values) {
	        super(context, R.layout.list_item_station, values);
	    }
	    
	    @Override
	    public int getCount() {
	    	return super.getCount() + 1;
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	View v = convertView;
	    	if (v == null) {
	    		v = mInflater.inflate(R.layout.list_item_station, parent, false);
	    	}
	    	TextView textView = (TextView) v.findViewById(R.id.text1);
    		if (textView != null) {
    			if (position == 0) {
    				textView.setText(R.string.action_add_station);
    			} else {
    				Station item = getItem(position - 1);
    				if (item != null) {
		    			textView.setText(item.getName());
		    			ImageView imageView = (ImageView) v.findViewById(R.id.list_item_station_icon);
		    			imageView.setVisibility(View.INVISIBLE);
		    		}
		    	}
	    	}
    		
	    	
	        return v;
	    }
	}

}
