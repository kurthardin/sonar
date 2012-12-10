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
package github.daneren2005.dsub.service.parser;

import github.daneren2005.dsub.domain.Artist;
import github.daneren2005.dsub.domain.Indexes;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

/**
 * @author Sindre Mehus
 */
public class IndexesParser extends AbstractParser {
    private static final String TAG = IndexesParser.class.getSimpleName();

    public Indexes parse(Reader reader) throws Exception {

        long t0 = System.currentTimeMillis();
        init(reader);

        List<Artist> artists = new ArrayList<Artist>();
        List<Artist> shortcuts = new ArrayList<Artist>();
        Long lastModified = null;
        int eventType;
        String index = "#";
        boolean changed = false;

        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("indexes".equals(name)) {
                    changed = true;
                    lastModified = getLong("lastModified");
                } else if ("index".equals(name)) {
                    index = get("name");

                } else if ("artist".equals(name)) {
                    Artist artist = new Artist();
                    artist.setId(get("id"));
                    artist.setName(get("name"));
                    artist.setIndex(index);
                    artists.add(artist);

                } else if ("shortcut".equals(name)) {
                    Artist shortcut = new Artist();
                    shortcut.setId(get("id"));
                    shortcut.setName(get("name"));
                    shortcut.setIndex("*");
                    shortcuts.add(shortcut);
                    
                } else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();

        if (!changed) {
            return null;
        }

        long t1 = System.currentTimeMillis();
//        Log.d(TAG, "Got " + artists.size() + " artist(s) in " + (t1 - t0) + "ms.");

        return new Indexes(lastModified == null ? 0L : lastModified, shortcuts, artists);
    }
}