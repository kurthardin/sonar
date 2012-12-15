package com.hardincoding.sonar.domain;

import java.io.Serializable;

public class Station implements Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private String mName;
	
	public Station(final String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}

}
