/**
 * 
 */
package com.hardincoding.sonar.subsonic.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.subsonic.domain.Indexes;
import com.hardincoding.sonar.subsonic.domain.ServerInfo;
import com.hardincoding.sonar.subsonic.domain.Version;
import com.hardincoding.sonar.subsonic.service.parser.ErrorParser;
import com.hardincoding.sonar.subsonic.service.parser.IndexesParser;
import com.hardincoding.sonar.subsonic.service.parser.LicenseParser;
import com.hardincoding.sonar.subsonic.service.ssl.SSLSocketFactory;
import com.hardincoding.sonar.subsonic.service.ssl.TrustSelfSignedStrategy;
import com.hardincoding.sonar.util.CancellableTask;
import com.hardincoding.sonar.util.FileUtil;
import com.hardincoding.sonar.util.ProgressListener;
import com.hardincoding.sonar.util.Util;


/**
 * @author Kurt Hardin
 *
 */
public enum SubsonicMusicService {
	
	INSTANCE;

    private static final String TAG = SubsonicMusicService.class.getSimpleName();

    private static final int SOCKET_CONNECT_TIMEOUT = 10 * 1000;
    private static final int SOCKET_READ_TIMEOUT_DEFAULT = 10 * 1000;
//    private static final int SOCKET_READ_TIMEOUT_DOWNLOAD = 30 * 1000;
//    private static final int SOCKET_READ_TIMEOUT_GET_RANDOM_SONGS = 60 * 1000;
//    private static final int SOCKET_READ_TIMEOUT_GET_PLAYLIST = 60 * 1000;

    // Allow 20 seconds extra timeout per MB offset.
//    private static final double TIMEOUT_MILLIS_PER_OFFSET_BYTE = 20000.0 / 1000000.0;

    /**
     * URL from which to fetch latest versions.
     */
//    private static final String VERSION_URL = "http://subsonic.org/backend/version.view";

    private static final int HTTP_REQUEST_MAX_ATTEMPTS = 5;
    private static final long REDIRECTION_CHECK_INTERVAL_MILLIS = 60L * 60L * 1000L;

    // REST protocol version and client ID.
    // Note: Keep it as low as possible to maintain compatibility with older servers.
    public static final String REST_PROTOCOL_VERSION = "1.2.0";
    public static final String REST_CLIENT_ID = "Sonar";
    
    private Version mServerRestVersion;

    private final ThreadSafeClientConnManager connManager;
	private final DefaultHttpClient mHttpClient;
	private String mServerAddress;
    private String redirectFrom;
    private String redirectTo;
    private long redirectionLastChecked;
    private int redirectionNetworkType = -1;
    
    private String mUsername;
    private String mPassword;
	
	private SubsonicMusicService() {
		// Create and initialize default HTTP parameters
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 20);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20));
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_READ_TIMEOUT_DEFAULT);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Create and initialize scheme registry
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", createSSLSocketFactory(), 443));

        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		mHttpClient = new DefaultHttpClient(connManager, params);
		
		// TODO Use HTTP Basic Auth
		// Configure preemptive HTTP Basic Authentication
//		HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
//		    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
//		        AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
//		        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
//		                ClientContext.CREDS_PROVIDER);
//		        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//		        
//		        if (authState.getAuthScheme() == null) {
//		            AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
//		            Credentials creds = credsProvider.getCredentials(authScope);
//		            if (creds != null) {
//		                authState.setAuthScheme(new BasicScheme());
//		                authState.setCredentials(creds);
//		            }
//		        }
//		    }    
//		};
//		mHttpClient.addRequestInterceptor(preemptiveAuth, 0);
	}

	private SocketFactory createSSLSocketFactory() {
        try {
            return new SSLSocketFactory(new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Throwable x) {
            Log.e(TAG, "Failed to create custom SSL socket factory, using default.", x);
            return org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
        }
    }
	
	public void setServerRestVersion(Version restVersion) {
		mServerRestVersion = restVersion;
	}
	
	public void setServerAddress(final String serverAddress) {
		mServerAddress =  serverAddress;
		Log.w(TAG, "mServerAddress = '" + serverAddress + "'");
	}
	
	public void setCredentials(final String username, final String password) {
		mUsername = username;
		mPassword = password;
		
		AuthScope auth = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		mHttpClient.getCredentialsProvider().setCredentials(auth, credentials);
	}
	
	public void ping(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "ping", null);
        
        try {
            new ErrorParser(context).parse(reader);
        } finally {
            Util.close(reader);
        }
    }

    public boolean isLicenseValid(Context context, ProgressListener progressListener) throws Exception {
        Reader reader = getReader(context, progressListener, "getLicense", null);
        try {
            ServerInfo serverInfo = new LicenseParser(context).parse(reader);
            return serverInfo.isLicenseValid();
        } finally {
            Util.close(reader);
        }
    }
    
    public Indexes getIndexes(String musicFolderId, boolean refresh, Context context, ProgressListener progressListener) throws Exception {
        Indexes cachedIndexes = readCachedIndexes(context, musicFolderId);
        if (cachedIndexes != null && !refresh) {
            return cachedIndexes;
        }

        long lastModified = cachedIndexes == null ? 0L : cachedIndexes.getLastModified();

        List<String> parameterNames = new ArrayList<String>();
        List<Object> parameterValues = new ArrayList<Object>();

        parameterNames.add("ifModifiedSince");
        parameterValues.add(lastModified);

        if (musicFolderId != null) {
            parameterNames.add("musicFolderId");
            parameterValues.add(musicFolderId);
        }

        Reader reader = getReader(context, progressListener, "getIndexes", null, parameterNames, parameterValues);
        try {
            Indexes indexes = new IndexesParser(context).parse(reader, progressListener);
            if (indexes != null) {
                writeCachedIndexes(context, indexes, musicFolderId);
                return indexes;
            }
            return cachedIndexes;
        } finally {
            Util.close(reader);
        }
    }

    private Indexes readCachedIndexes(Context context, String musicFolderId) {
        String filename = getCachedIndexesFilename(context, musicFolderId);
        return FileUtil.deserialize(context, filename, true);
    }

    private void writeCachedIndexes(Context context, Indexes indexes, String musicFolderId) {
        String filename = getCachedIndexesFilename(context, musicFolderId);
        FileUtil.serialize(context, indexes, filename, true);
    }

    private String getCachedIndexesFilename(Context context, String musicFolderId) {
        String s = getRestUrl(null) + musicFolderId;
        return "indexes-" + Math.abs(s.hashCode()) + ".ser";
    }

    private Reader getReader(Context context, ProgressListener progressListener, String method, HttpParams requestParams) throws Exception {
        return getReader(context, progressListener, method, requestParams, Collections.<String>emptyList(), Collections.emptyList());
    }

//    private Reader getReader(Context context, ProgressListener progressListener, String method,
//                             HttpParams requestParams, String parameterName, Object parameterValue) throws Exception {
//        return getReader(context, progressListener, method, requestParams, Arrays.asList(parameterName), Arrays.<Object>asList(parameterValue));
//    }

    private Reader getReader(Context context, ProgressListener progressListener, String method,
                             HttpParams requestParams, List<String> parameterNames, List<Object> parameterValues) throws Exception {

        if (progressListener != null) {
            progressListener.updateProgress(R.string.service_connecting);
        }
        
        String url = getRestUrl(method);
        return getReaderForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
    }

    private Reader getReaderForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                   List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        HttpEntity entity = getEntityForURL(context, url, requestParams, parameterNames, parameterValues, progressListener);
        if (entity == null) {
            throw new RuntimeException("No entity received for URL " + url);
        }

        InputStream in = entity.getContent();
        return new InputStreamReader(in, Util.UTF_8);
    }

    private HttpEntity getEntityForURL(Context context, String url, HttpParams requestParams, List<String> parameterNames,
                                       List<Object> parameterValues, ProgressListener progressListener) throws Exception {
        return getResponseForURL(context, url, requestParams, parameterNames, parameterValues, null, progressListener, null).getEntity();
    }

    private HttpResponse getResponseForURL(Context context, String url, HttpParams requestParams,
                                           List<String> parameterNames, List<Object> parameterValues,
                                           List<Header> headers, ProgressListener progressListener, CancellableTask task) throws Exception {
        Log.d(TAG, "Connections in pool: " + connManager.getConnectionsInPool());

        // If not too many parameters, extract them to the URL rather than relying on the HTTP POST request being
        // received intact. Remember, HTTP POST requests are converted to GET requests during HTTP redirects, thus
        // loosing its entity.
        if (parameterNames != null && parameterNames.size() < 10) {
            StringBuilder builder = new StringBuilder(url);
            for (int i = 0; i < parameterNames.size(); i++) {
                builder.append("&").append(parameterNames.get(i)).append("=");
                builder.append(URLEncoder.encode(String.valueOf(parameterValues.get(i)), "UTF-8"));
            }
            url = builder.toString();
            parameterNames = null;
            parameterValues = null;
        }

        String rewrittenUrl = rewriteUrlWithRedirect(context, url);
        return executeWithRetry(context, rewrittenUrl, url, requestParams, parameterNames, parameterValues, headers, progressListener, task);
    }

    private HttpResponse executeWithRetry(Context context, String url, String originalUrl, HttpParams requestParams,
                                          List<String> parameterNames, List<Object> parameterValues,
                                          List<Header> headers, ProgressListener progressListener, CancellableTask task) throws IOException {
        Log.i(TAG, "Using URL " + url);

        final AtomicReference<Boolean> cancelled = new AtomicReference<Boolean>(false);
        int attempts = 0;
        while (true) {
            attempts++;
            HttpContext httpContext = new BasicHttpContext();
            final HttpPost request = new HttpPost(url);

            if (task != null) {
                // Attempt to abort the HTTP request if the task is cancelled.
                task.setOnCancelListener(new CancellableTask.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        cancelled.set(true);
                        request.abort();
                    }
                });
            }

            if (parameterNames != null) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (int i = 0; i < parameterNames.size(); i++) {
                    params.add(new BasicNameValuePair(parameterNames.get(i), String.valueOf(parameterValues.get(i))));
                }
                request.setEntity(new UrlEncodedFormEntity(params, Util.UTF_8));
            }

            if (requestParams != null) {
                request.setParams(requestParams);
                Log.d(TAG, "Socket read timeout: " + HttpConnectionParams.getSoTimeout(requestParams) + " ms.");
            }

            if (headers != null) {
                for (Header header : headers) {
                    request.addHeader(header);
                }
            }

            try {
                HttpResponse response = mHttpClient.execute(request, httpContext);
                detectRedirect(originalUrl, context, httpContext);
                return response;
            } catch (IOException x) {
                request.abort();
                if (attempts >= HTTP_REQUEST_MAX_ATTEMPTS || cancelled.get()) {
                    throw x;
                }
                if (progressListener != null) {
                    String msg = context.getResources().getString(R.string.music_service_retry, attempts, HTTP_REQUEST_MAX_ATTEMPTS - 1);
                    progressListener.updateProgress(msg);
                }
                Log.w(TAG, "Got IOException (" + attempts + "), will retry", x);
                increaseTimeouts(requestParams);
                Util.sleepQuietly(2000L);
            }
        }
    }

    private void increaseTimeouts(HttpParams requestParams) {
        if (requestParams != null) {
            int connectTimeout = HttpConnectionParams.getConnectionTimeout(requestParams);
            if (connectTimeout != 0) {
                HttpConnectionParams.setConnectionTimeout(requestParams, (int) (connectTimeout * 1.3F));
            }
            int readTimeout = HttpConnectionParams.getSoTimeout(requestParams);
            if (readTimeout != 0) {
                HttpConnectionParams.setSoTimeout(requestParams, (int) (readTimeout * 1.5F));
            }
        }
    }

    private void detectRedirect(String originalUrl, Context context, HttpContext httpContext) {
        HttpUriRequest request = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

        // Sometimes the request doesn't contain the "http://host" part so we
        // must take from the HttpHost object.
        String redirectedUrl;
        if (request.getURI().getScheme() == null) {
            redirectedUrl = host.toURI() + request.getURI();
        } else {
            redirectedUrl = request.getURI().toString();
        }

        redirectFrom = originalUrl.substring(0, originalUrl.indexOf("/rest/"));
        redirectTo = redirectedUrl.substring(0, redirectedUrl.indexOf("/rest/"));

        Log.i(TAG, redirectFrom + " redirects to " + redirectTo);
        redirectionLastChecked = System.currentTimeMillis();
        redirectionNetworkType = getCurrentNetworkType(context);
    }

    private String rewriteUrlWithRedirect(Context context, String url) {

        // Only cache for a certain time.
        if (System.currentTimeMillis() - redirectionLastChecked > REDIRECTION_CHECK_INTERVAL_MILLIS) {
            return url;
        }

        // Ignore cache if network type has changed.
        if (redirectionNetworkType != getCurrentNetworkType(context)) {
            return url;
        }

        if (redirectFrom == null || redirectTo == null) {
            return url;
        }

        return url.replace(redirectFrom, redirectTo);
    }

    private int getCurrentNetworkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo == null ? -1 : networkInfo.getType();
    }
    
    public String getRestUrl(String method) {
        StringBuilder builder = new StringBuilder();
        String serverUrl = mServerAddress;
        builder.append(serverUrl);
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append("/");
        }
        builder.append("rest/").append(method).append(".view");
        builder.append("?u=").append(mUsername);
        String password = "enc:" + Util.utf8HexEncode(mPassword);
        builder.append("&p=").append(password);
        builder.append("&v=").append(REST_PROTOCOL_VERSION);
        builder.append("&c=").append(REST_CLIENT_ID);
        return builder.toString();
    }

}
