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
package com.hardincoding.sonar.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

/**
 * Asynchronous loading of images, with caching.
 * <p/>
 * There should normally be only one instance of this class.
 *
 * @author Sindre Mehus
 */
public class ImageLoader implements Runnable {

    private static final String TAG = ImageLoader.class.getSimpleName();
    private static final int CONCURRENCY = 5;

    private final LRUCache<String, Bitmap> cache = new LRUCache<String, Bitmap>(100);
    private final BlockingQueue<Task> queue;

    public ImageLoader() {
        queue = new LinkedBlockingQueue<Task>(500);
        for (int i = 0; i < CONCURRENCY; i++) {
            new Thread(this, "ImageLoader").start();
        }
    }

    public void loadImage(String url, boolean saveToFile, ImageLoaderCompletionHandler completionHandler) {
    	Bitmap bitmap = cache.get(url);
        if (bitmap != null) {
            completionHandler.done(bitmap);
        } else {
        	completionHandler.done(null);
        	queue.add(new Task(url, saveToFile, completionHandler));
        }
    }

    public void clear() {
        queue.clear();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Task task = queue.take();
                task.execute();
            } catch (Throwable x) {
                Log.e(TAG, "Unexpected exception in ImageLoader.", x);
            }
        }
    }

    private class Task {
        private final String mUrl;
        private final boolean mSaveToFile;
        private final ImageLoaderCompletionHandler mCompletionHandler;
        private final Handler mHandler;

        public Task(String url, boolean saveToFile, ImageLoaderCompletionHandler completionHandler) {
            mUrl = url;
            mSaveToFile = saveToFile;
            mCompletionHandler = completionHandler;
            mHandler = new Handler();
        }

        public void execute() {
            try {
            	URL url = new URL(mUrl);
            	URLConnection ucon = url.openConnection();
            	Bitmap bitmap = null;
            	InputStream in = null;
                try {
                    in = ucon.getInputStream();
                    byte[] bytes = Util.toByteArray(in);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } finally {
                    Util.close(in);
                }
                cache.put(mUrl, bitmap);
                
                if (mSaveToFile) {
                	// TODO Save bitmap file
                }
                
                mCompletionHandler.setDrawable(bitmap);
                mHandler.post(mCompletionHandler);
            } catch (Throwable x) {
                Log.e(TAG, "Failed to download album art.", x);
            }
        }
    }
	
	public static abstract class ImageLoaderCompletionHandler implements Runnable {
		
		private Bitmap mDrawable;
		
		private void setDrawable(final Bitmap bitmap) {
			mDrawable = bitmap;
		}
		
		@Override
		public void run() {
			done(mDrawable);
		}
		
		public abstract void done(final Bitmap bitmap);
		
	}
}
