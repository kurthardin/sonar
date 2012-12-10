/**
 * 
 */
package com.hardincoding.sonar.util;

import java.io.Closeable;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author Kurt Hardin
 *
 */
public final class Util {
	
	private static final String TAG = Util.class.getSimpleName();

	private Util() {
		// Avoid instantiation
	}
	
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            Log.w(TAG, "Interrupted from sleep.", x);
        }
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        boolean wifiConnected = connected && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        boolean wifiRequired = false;//isWifiRequiredForDownload(context);

        return connected && (!wifiRequired || wifiConnected);
    }
	
}
