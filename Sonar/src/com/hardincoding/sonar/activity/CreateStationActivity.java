package com.hardincoding.sonar.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.domain.Station;
import com.hardincoding.sonar.subsonic.service.CachedMusicService;
import com.hardincoding.sonar.util.ModalBackgroundTask;
import com.hardincoding.sonar.util.Util;

import de.umass.lastfm.Artist;

public class CreateStationActivity extends Activity {
	
	private AutoCompleteTextView mArtistView;
	private GetArtistSuggestionsTask mGetArtistSuggestionTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_station);

		mArtistView = (AutoCompleteTextView) findViewById(R.id.station_artist);
		mArtistView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	// Do nothing
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            	// Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
            	if (mGetArtistSuggestionTask != null) {
            		mGetArtistSuggestionTask.cancel();
            	}
            	if (s.length() >= mArtistView.getThreshold()) {
            		mGetArtistSuggestionTask = new GetArtistSuggestionsTask(s.toString());
            		mGetArtistSuggestionTask.execute();
            	}
            }
        });
		mArtistView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				AutoCompleteArtistAdapter adapter = (AutoCompleteArtistAdapter) mArtistView.getAdapter();
				Artist selectedArtist = adapter.getItem(position);
				mArtistView.setText(selectedArtist.getName());
				createStation(selectedArtist);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void createStation(Artist artist) {
		if (artist != null ) {
			CachedMusicService.INSTANCE.getStations(CreateStationActivity.this).add(
					new Station(artist.getName()));
			CachedMusicService.INSTANCE.writeStations(CreateStationActivity.this);
			// TODO Determine similar artists
			// TODO Start music player Activity/Service
			finish();
		}
	}
	
	private class AutoCompleteArtistAdapter extends ArrayAdapter<Artist> {
		
		private final LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		private final List<Artist> mItems;
		
		public AutoCompleteArtistAdapter(Context context, List<Artist> items) {
			super(context, android.R.layout.simple_dropdown_item_1line, items);
			mItems = items;
		}
		
		@Override
		public Filter getFilter() {
			return new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults results = new FilterResults();
					results.values = mItems;
					results.count = mItems.size();
					return results;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					notifyDataSetChanged();
				}
				
			};
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	View v = convertView;
	    	if (v == null) {
	    		v = mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
	    	}
	    	
	    	Artist item = getItem(position);
			if (item != null) {
				TextView textView = (TextView) v.findViewById(android.R.id.text1);
				textView.setText(item.getName());
	    	}
    		
	        return v;
	    }
		
	}
	
	private class GetArtistSuggestionsTask extends ModalBackgroundTask<Collection<Artist>> {
		
		private final String mSearchString;

		public GetArtistSuggestionsTask(final String searchString) {
			super(CreateStationActivity.this, false);
			showDialog(false);
			mSearchString = searchString;
		}

		@Override
		protected Collection<Artist> doInBackground() throws Throwable {
			return Artist.search(mSearchString, Util.LASTFM_API_KEY);
		}
		
		@Override
		protected void cancel() {
			super.cancel();
		}

		@Override
		protected void done(Collection<Artist> result) {
			AutoCompleteArtistAdapter adapter = new AutoCompleteArtistAdapter(
					CreateStationActivity.this, new ArrayList<Artist>(result));
			mArtistView.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
		
	}

}
