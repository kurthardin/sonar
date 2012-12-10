package com.hardincoding.sonar.trials;

import github.daneren2005.dsub.service.RESTMusicService;
import github.daneren2005.dsub.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;

public class LastfmJavaTrial {
	
	public static void main(String[] args) {
		Caller.getInstance().setUserAgent("SonarTrials");
		Caller.getInstance().getLogger().setLevel(Level.ALL); // Set debug level
		
		Artist a = Artist.getCorrection("Snop Dog", Util.SONAR_API_KEY);
		Collection<Artist> similarArtists = Artist.getSimilar(a.getName(), Util.SONAR_API_KEY);
//		for (Artist artist : similarArtists) {
//			System.out.println(artist.getName());
//		}
//		System.out.println();
		
		RESTMusicService subRest = new RESTMusicService();
		
		Map<String, String> correctedIndexes = null;
		try {
			correctedIndexes = subRest.getCorrectedIndexes(null, true);
		} catch (Exception e) {
			// Do nothing
		}
		if (correctedIndexes != null) {
			List<String> matchingArtists = new ArrayList<String>(similarArtists.size());
			for (Artist artist : similarArtists) {
				String indexId = correctedIndexes.get(artist.getName());
				if (indexId != null) {
					matchingArtists.add(indexId);
					System.out.println(artist.getName());
				}
			}
		}		
	}

}
