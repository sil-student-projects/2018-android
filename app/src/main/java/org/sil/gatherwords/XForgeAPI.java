package org.sil.gatherwords;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class XForgeAPI {
    private static final String TAG = XForgeAPI.class.getSimpleName();

    private Context m_context;
    private String m_jwt;

    public XForgeAPI(Context context) {
        this(context, null);
    }

    public XForgeAPI(Context context, String jwt) {
        m_context = context;
        m_jwt = jwt;
    }

    public String login(String accessToken) {
        URL url = getApiUrl("login");
        if (url == null) {
            return null;
        }

        String jwt = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                jwt = doPostRequest(conn, accessToken);
            } finally {
                conn.disconnect();
            }

        } catch (IOException e) {
            Log.e(TAG, "Network error during login", e);
        }

        return jwt;
    }

    public boolean isJwtAuthorized(String token) {
        // TODO: Ping server to see if jwt expired.
        return true;
    }

    @Nullable
    private URL getApiUrl(String path) {
        try {
            return new URL(
                m_context.getString(R.string.x_forge_base_path) + path
            );
        } catch (MalformedURLException e) {
            Log.d(TAG, "Failed to create api url for: " + path, e);
            return null;
        }
    }

    @Nullable
    private String doPostRequest(HttpURLConnection conn, String data) throws IOException {
        Log.d(TAG, "POST: " + conn.getURL());

        if (m_jwt != null) {
            conn.setRequestProperty("Authorization", "Bearer " + m_jwt);
        }

        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(data.length());

        OutputStream out = new BufferedOutputStream(conn.getOutputStream());
        out.write(data.getBytes());
        out.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "POST failed with response code: " + responseCode);
            return null;
        }

        InputStream in = new BufferedInputStream(conn.getInputStream());
        String output = Util.readStream(in);
        in.close();

        return output;
    }

    @Nullable
    private String doGetRequest(HttpURLConnection conn) throws IOException {
        Log.d(TAG, "GET: " + conn.getURL());

        if (m_jwt != null) {
            conn.setRequestProperty("Authorization", "Bearer " + m_jwt);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "GET failed with response code: " + responseCode);
            return null;
        }

        InputStream in = new BufferedInputStream(conn.getInputStream());
        String output = Util.readStream(in);
        in.close();

        return output;
    }
}
