package com.hardincoding.sonar.domain;

import java.io.Serializable;

public class Station implements Serializable {
	
	private static final long serialVersionUID = 3L;
	
	private String mName;
	private String mArtist;
	private String mTrack;
	private String mImageFilename;
	
	public Station(final String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getImageFilename() {
		return mImageFilename;
	}
	
	public void setImageFilename(final String filename) {
		mImageFilename = filename;
	}

	public String getArtist() {
		return mArtist;
	}

	public void setArtist(String artist) {
		mArtist = artist;
	}

	public String getTrack() {
		return mTrack;
	}

	public void setTrack(String track) {
		mTrack = track;
	}

}
