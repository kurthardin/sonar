package com.hardincoding.sonar.domain;

import java.io.Serializable;

public class Station implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String mName;
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

}
