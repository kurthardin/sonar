/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.hardincoding.sonar.subsonic.domain;

import java.io.Serializable;

/**
 * @author Kurt Hardin
 */
public class Artist implements Serializable {

	private static final long serialVersionUID = 390216832389328219L;
	private String mId;
    private String mName;
    private String mIndex;
    private String mCorrectedName;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getIndex() {
        return mIndex;
    }

    public void setIndex(String index) {
        mIndex = index;
    }

    public String getCorrectedName() {
        return mCorrectedName;
    }

    public void setCorrectedName(String correctedName) {
        mCorrectedName = correctedName;
    }

    @Override
    public String toString() {
        return "[name=" + mName +
        		", correctedName=" + mCorrectedName + 
        		", id=" + mId +
        		", index=" + mIndex;
    }
}