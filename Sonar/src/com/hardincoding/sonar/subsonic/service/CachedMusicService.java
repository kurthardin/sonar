package com.hardincoding.sonar.subsonic.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.hardincoding.sonar.domain.Station;
import com.hardincoding.sonar.subsonic.domain.Artist;
import com.hardincoding.sonar.subsonic.domain.Indexes;
import com.hardincoding.sonar.util.FileUtil;
import com.hardincoding.sonar.util.ModalBackgroundTask;
import com.hardincoding.sonar.util.Util;

public enum CachedMusicService {
	INSTANCE;
	
	private ArtistSyncTask mArtistSyncer;
	
	private LinkedList<Station> mStations = null;
	private LinkedList<Artist> mArtists = null;
	
	private CachedMusicService() {
		
	}
	
	public boolean updateCache(Activity activity) {
		if (mArtistSyncer == null) {
			mArtistSyncer = new ArtistSyncTask(activity, false);
			mArtistSyncer.execute();
			return true;
		}
		return false;
	}
	
	public List<Station> getStations(Context context) {
		if (mStations == null) {
			mStations = FileUtil.deserialize(context, getStationsFilename(), false);
			if (mStations == null) {
				mStations = new LinkedList<Station>();
			}
		}
		return mStations;
	}
	
	public void writeStations(Context context) {
		FileUtil.serialize(context, mStations, getStationsFilename(), false);
	}
	
	private String getStationsFilename() {
    	String s = SubsonicMusicService.INSTANCE.getRestUrl(null);
        return "artists-" + Math.abs(s.hashCode()) + ".ser";
    }
	
	public List<Artist> getArtists(Context context) {
		if (mArtists == null) {
			mArtists = FileUtil.deserialize(context, getCachedArtistsFilename(), false);
			if (mArtists == null) {
				mArtists = new LinkedList<Artist>();
			}
		}
		return mArtists;
	}

    public void writeCachedArtists(Context context) {
        FileUtil.serialize(context, mArtists, getCachedArtistsFilename(), false);
    }
    
    public void deleteCache(Context context) {
    	File file = new File(context.getCacheDir(), getCachedArtistsFilename());
    	file.delete();
    	mArtists = null;
    }
    
    private String getCachedArtistsFilename() {
    	String s = SubsonicMusicService.INSTANCE.getRestUrl(null);
        return "artists-" + Math.abs(s.hashCode()) + ".ser";
    }
    
    private class ArtistSyncTask extends ModalBackgroundTask<Boolean> {
    	
    	private final String TAG = ArtistSyncTask.class.getSimpleName();

    	public ArtistSyncTask(Activity activity, boolean finishActivityOnCancel) {
    		super(activity, finishActivityOnCancel);
    		SharedPreferences prefs = Util.getPreferences(getActivity());
    		showDialog(prefs.getBoolean(Util.PREFERENCES_KEY_FIRST_CACHE_FLAG, true));
    	}

    	@Override
    	protected Boolean doInBackground() throws Throwable {
    		
    		Indexes idxs = SubsonicMusicService.INSTANCE.getIndexes(null, true, getActivity(), this);
    		List<Artist> subsonicArtists = idxs.getArtists();
    		List<Artist> cachedArtists = CachedMusicService.INSTANCE.getArtists(getActivity());
    		boolean checkCache = !cachedArtists.isEmpty();
    		boolean writeCache = false;
    		int j = 0;
    		for (int i = 0; i < subsonicArtists.size(); i++) {
    			updateProgress("Processing artist " + (i + 1) + " of " + subsonicArtists.size());
    			
    			Artist subsonicArtist = subsonicArtists.get(i);
    			
    			if (checkCache && j < cachedArtists.size()) {
    				Artist cachedArtist = cachedArtists.get(j);
    				while (cachedArtist.getName().compareTo(subsonicArtist.getName()) < 0) {
    					cachedArtists.remove(j);
    					Log.i(TAG, "Artist cache: removed " + cachedArtist.getName());
    					writeCache = true;
    					cachedArtist = cachedArtists.get(j);
    				}
    				
    				if (subsonicArtist.getName().equals(cachedArtist.getName())) {
    					if (!subsonicArtist.getId().equals(cachedArtist.getId())) {
    						cachedArtist.setId(subsonicArtist.getId());
    						Log.i(TAG, "Artist cache: updated " + cachedArtist.getName());
    						writeCache = true;
    					}
    					j++;
    					continue;
    				}
    			}
    			
    			de.umass.lastfm.Artist lastfmArtist = 
    					de.umass.lastfm.Artist.getCorrection(
    							subsonicArtist.getName(), Util.LASTFM_API_KEY);
    			subsonicArtist.setCorrectedName(lastfmArtist == null ? null : lastfmArtist.getName());
				cachedArtists.add(j++, subsonicArtist);
				Log.i(TAG, "Artist cache: added " + subsonicArtist.getName());
				writeCache = true;
    			Thread.sleep(200);
    		}
    		
    		if (writeCache) {
    			CachedMusicService.INSTANCE.writeCachedArtists(getActivity());
    		}
    		
    		Util.getPreferences(getActivity()).edit()
    		.putBoolean(Util.PREFERENCES_KEY_FIRST_CACHE_FLAG, false)
    		.apply();
    		
    		return true;
    	}

    	@Override
    	protected void done(Boolean result) {
    		mArtistSyncer = null;
    	}
    	
    	@Override
    	protected void cancel() {
    		super.cancel();
    		mArtistSyncer = null;
    	}
    	
    	@Override
        protected void error(Throwable error) {
    		super.error(error);
    		mArtistSyncer = null;
    	}
    	
    }
}
