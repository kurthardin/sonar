package com.hardincoding.sonar.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.domain.Station;
import com.hardincoding.sonar.subsonic.service.CachedMusicService;
import com.hardincoding.sonar.util.ImageLoader;
import com.hardincoding.sonar.util.ImageLoader.ImageLoaderCompletionHandler;
import com.hardincoding.sonar.util.ModalBackgroundTask;
import com.hardincoding.sonar.util.Util;

import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.Track;

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
				MusicEntry selected = adapter.getItem(position);
				mArtistView.setText(selected.getName());
				createStation(selected);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void createStation(MusicEntry selection) {
		if (selection != null ) {
			Track track = null;
			String artistName = null;
			if (selection instanceof Artist) {
				artistName = ((Artist) selection).getName();
			} else if (selection instanceof Track) {
				track = (Track) selection;
				artistName = track.getArtist();
			}
			final Station newStation = new Station(artistName);
			// TODO load/save bitmap and set filename of newStation
			CachedMusicService.INSTANCE.getStations(CreateStationActivity.this).add(
					newStation);
			CachedMusicService.INSTANCE.writeStations(CreateStationActivity.this);
			// TODO Determine similar artists
			// TODO Start music player Activity/Service
			finish();
		}
	}
	
	private class AutoCompleteArtistAdapter extends ArrayAdapter<MusicEntry> {
		
		private static final int VIEW_TYPE_ARTIST = 0;
		private static final int VIEW_TYPE_TRACK = 1;
		
		private final LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		private final ImageLoader mImageLoader;
		private final List<MusicEntry> mItems;
		
		public AutoCompleteArtistAdapter(Context context, List<MusicEntry> items) {
			super(context, android.R.layout.simple_list_item_2, items);
			mImageLoader = new ImageLoader();
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
		public int getViewTypeCount () {
			return 2;
		}
		
		@Override
		public int getItemViewType (int position) {
			MusicEntry item = getItem(position);
			if (item != null && item instanceof Track) {
				return VIEW_TYPE_TRACK;
			} else {
				return VIEW_TYPE_ARTIST;
			}
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	View v = convertView;
	    	MusicEntry item = getItem(position);
	    	if (item != null) {
	    		
	    		if (v == null) {
	    			int layout = item instanceof Track ? 
	    					R.layout.dropdown_list_item_track : 
	    						R.layout.dropdown_list_item_artist;
	    			v = mInflater.inflate(layout, parent, false);
	    		}
	    		
	    		TextView textView;
	    		String text1;
	    		if (item instanceof Track) {
	    			text1 = item.getName();
		    		textView = (TextView) v.findViewById(R.id.text2);
		    		textView.setText("By " + ((Track) item).getArtist());
	    		} else {
	    			text1 = item.getName();
	    		}

	    		textView = (TextView) v.findViewById(R.id.text1);
	    		textView.setText(text1);
	    		
	    		final ImageView imageView = (ImageView) v.findViewById(R.id.list_item_icon);
	    		mImageLoader.loadImage(item.getImageURL(ImageSize.MEDIUM), false, 
	    				new ImageLoaderCompletionHandler() {
							@Override
							public void done(Bitmap bitmap) {
								imageView.setImageBitmap(bitmap);
							}
						});
	    	}

	        return v;
	    }
		
	}
	
	private class GetArtistSuggestionsTask extends ModalBackgroundTask<Collection<MusicEntry>> {
		
		private final String mSearchString;

		public GetArtistSuggestionsTask(final String searchString) {
			super(CreateStationActivity.this, false);
			showDialog(false);
			mSearchString = searchString;
		}

		@Override
		protected Collection<MusicEntry> doInBackground() throws Throwable {
			Collection<Artist> artists = Artist.search(mSearchString, Util.LASTFM_API_KEY);
			Collection<Track> tracks = Track.search(mSearchString, Util.LASTFM_API_KEY);
			PriorityQueue<MusicEntry> results = 
					new PriorityQueue<MusicEntry>(artists.size() + tracks.size(),
							new Comparator<MusicEntry>() {
								@Override
								public int compare(MusicEntry lhs,
										MusicEntry rhs) {
									float result = lhs.getSimilarityMatch() - rhs.getSimilarityMatch();
									if (result < 0) {
										return -1;
									} else if (result > 0) {
										return 1;
									} else {
										return 0;
									}
								}
							});
			results.addAll(artists);
			results.addAll(tracks);
			return results;
		}
		
		@Override
		protected void cancel() {
			super.cancel();
		}

		@Override
		protected void done(Collection<MusicEntry> result) {
			AutoCompleteArtistAdapter adapter = new AutoCompleteArtistAdapter(
					CreateStationActivity.this, new ArrayList<MusicEntry>(result));
			mArtistView.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
		
	}

}
