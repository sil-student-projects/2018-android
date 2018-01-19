package org.sil.gatherwords;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Converters;
import org.sil.gatherwords.room.FilledWord;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connected = checkConnection();

        final ListView sessionList = findViewById(R.id.session_list);

        sessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), EntryActivity.class);
                Session session = (Session) sessionList.getAdapter().getItem(i);
                intent.putExtra(SessionActivity.ARG_ID, session.id);
                // Passes id of selected session into EntryActivity
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Insure the ListView is updated when the back button is pressed.
        new FillSessionListTask(this).execute();
    }

    /**
     * Check the current connection.
     *
     * @return true if currently connected, false otherwise
     */
    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return false;
        }
        return activeNetworkInfo.isConnected();
    }

    private static class FillSessionListTask extends AsyncTask<Void, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<MainActivity> mainActivityRef;
        private boolean connected = false;

        FillSessionListTask(MainActivity mainActivity) {
            sDAO = AppDatabase.get(mainActivity).sessionDao();
            mainActivityRef = new WeakReference<>(mainActivity);
            connected = mainActivity.connected;
        }

        @Override
        protected List<Session> doInBackground(Void... v) {
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<Session> sessions) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                ListView sessionList = mainActivity.findViewById(R.id.session_list);
                sessionList.setAdapter(new SessionListAdapter(
                    mainActivity.getLayoutInflater(),
                    sessions,
                        connected
                ));
            }
        }
    }

    //Enables icon to access Preferences activity.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.preference_menu, menu);
        return true;
    }

    /**
     * react to the user tapping/selecting an options menu item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preference_menu:
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivity(i);
                return true;
            case R.id.undo_session_delete:
                new UndoDeleteSessionFromDB(this).execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onCreateSessionClick(View v) {
        Intent intent = new Intent(this, SessionActivity.class);
        // To distinguish between creating a session and viewing the settings of an old one
        intent.putExtra(SessionActivity.ARG_CREATING_SESSION, true);
        startActivity(intent);
    }

    // TODO: Switch to CursorAdapter
    private static class SessionListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<Session> sessions;
        private boolean connected = false;

        SessionListAdapter(LayoutInflater flate, List<Session> sessionList, boolean connection) {
            inflater = flate;
            sessions = sessionList;
            connected = connection;
        }

        @Override
        public int getCount() {
            return sessions.size();
        }

        @Override
        public Object getItem(int i) {
            return sessions.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = inflater.inflate(
                    R.layout.session_list_item,
                    container,
                    false
                );
            }

            final Session session = sessions.get(i);

            // Send the session ID upstream

            // Get the date to prove that there is data being retrieved
            TextView labelText = convertView.findViewById(R.id.session_list_lable);
            labelText.setText(session.label);
            TextView date = convertView.findViewById(R.id.session_list_date);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            date.setText(df.format(session.date));
            TextView speaker = convertView.findViewById(R.id.session_list_speaker);
            speaker.setText(session.speaker);

            ImageButton editButton = convertView.findViewById(R.id.session_list_button_edit);
            editButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = inflater.getContext();
                    Intent intent = new Intent(context, SessionActivity.class);
                    intent.putExtra(SessionActivity.ARG_CREATING_SESSION, false);
                    intent.putExtra(SessionActivity.ARG_ID, session.id);
                    context.startActivity(intent);
                }
            });

            ImageButton uploadButton = convertView.findViewById(R.id.upload_session);
            uploadButton.setVisibility((connected) ? View.VISIBLE : View.GONE);
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new GetProjectMetaTask(inflater.getContext(),session.id).execute();
                }
            });

            ImageButton deleteButton = convertView.findViewById(R.id.session_list_button_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = inflater.getContext();
                    new DeleteSessionFromDB(context).execute(session);

                }
            });

            return convertView;
        }
    }

    private static class DeleteSessionFromDB extends AsyncTask<Session, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        DeleteSessionFromDB(Context context) {
            sDAO = AppDatabase.get(context).sessionDao();
            mainActivityRef = new WeakReference<>((MainActivity) context);
        }

        @Override
        protected List<Session> doInBackground(Session... sessions) {
            for (Session session : sessions) {
                session.deletedAt = new Date();
            }
            sDAO.updateSession(sessions);
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<Session> sessions) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                SessionListAdapter sessionListAdapter = (SessionListAdapter) ((ListView) mainActivity.findViewById(R.id.session_list)).getAdapter();
                sessionListAdapter.sessions = sessions;
                sessionListAdapter.notifyDataSetChanged();
                mainActivity.showUndoSnackbar();
            }
        }
    }

    private void showUndoSnackbar() {
        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.mainView),
                R.string.session_delete, Snackbar.LENGTH_LONG);
        mySnackbar.setAction(R.string.undo, new UndoListener());
        mySnackbar.show();
    }

    public class UndoListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            new UndoDeleteSessionFromDB(MainActivity.this).execute();
        }
    }

    private static class UndoDeleteSessionFromDB extends AsyncTask<Void, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        UndoDeleteSessionFromDB(MainActivity mainActivity) {
            sDAO = AppDatabase.get(mainActivity).sessionDao();
            mainActivityRef = new WeakReference<>(mainActivity);
        }

        @Override
        protected List<Session> doInBackground(Void... v) {
            sDAO.undoLastDeleted();
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<Session> sessions) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                SessionListAdapter sessionListAdapter = (SessionListAdapter) ((ListView) mainActivity.findViewById(R.id.session_list)).getAdapter();
                sessionListAdapter.sessions = sessions;
                sessionListAdapter.notifyDataSetChanged();
            }
        }
    }

    private static class GetProjectMetaTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityRef;
        private AppDatabase db;
        private SessionDao sDao;
        private WordDao wDao;
        private long sessionId;
        private RequestQueue queue;
        private String url_base;
        private String projectID;
        private List<Long> wordIds;
        private List<FilledWord> manyWords;

        GetProjectMetaTask(Context context, long id) {
            db = AppDatabase.get(context);
            activityRef = new WeakReference<>((MainActivity) context);
            sDao = db.sessionDao();
            wDao = db.wordDao();
            sessionId = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return null;
            }

            /* Collected word (lexeme:)
            {
                LANGUAGE-KEY: { // Like qaa-fonipa-x-kal
                    value: "some-word"
                }
                AUDIO-LANGUAGE-KEY: { // Like qaa-Zxxx-x-kal-audio
                    value: filepath
                }
            }
            */

            /* meanings, pictures, other things (senses:)
            {
                gloss: {
                    en: "some definition",
                    fr: "",
                    ...
                }
                pictures: [
                    filepath // Possibly different? I don't see a way to manually add pictures to LanguageForge
                ]
                location : {
                    value: ""
                }
            }
            */
            List<Session> sessionList = sDao.getSessionsByID(sessionId);
            if (sessionList == null || sessionList.isEmpty()) {
                Log.e(this.getClass().getSimpleName(), "Can't find the session to upload");
                return null;
            }
            Session session = sessionList.get(0);
            if (session == null) {
                Log.e(this.getClass().getSimpleName(), "The session is null");
                return null;
            }
            // Create the queue
            queue = Volley.newRequestQueue(activity.getApplicationContext());
            url_base = activity.getString(R.string.LF_base_url);

            wordIds = wDao.getIDsForSession(sessionId);
            manyWords = wDao.getManyFilled(wordIds);
            // Assume that the project has already been selected
            getProjectList(session);
            return null;
        }

        private void getProjectList(Session session) {
            getProjectInfo(session);
            return;
        }

        private void getProjectInfo(final Session session) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            projectID = activity.getString(R.string.LF_PID);
            String url = url_base + String.format(activity.getString(R.string.LF_GET_lex_projects), projectID);
            StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    JSONObject object = Converters.stringToJsonObject(response);
                    try {
                        session.inputSystems = object.getJSONObject("inputSystems");
                        postEntity(session);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Volley", "Something broke", error);
                }
            });
            queue.add(request);
        }

        private void postEntity(Session session) {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            String url = url_base + String.format(activity.getString(R.string.LF_POST_entry), projectID);
            for (final FilledWord word : manyWords) {
                String filepath = postAsset();
                final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        new WordFailedUploadTask(wDao).execute(word);
                    }
                }) {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        String requestBody = "";
                        try {
                            requestBody = word.toString();
                            return requestBody == null ? null : requestBody.getBytes("utf-8");
                        } catch (UnsupportedEncodingException uee) {
                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                            return null;
                        }
                    }

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        String responseString = "";
                        if (response != null) {
                            responseString = String.valueOf(response.statusCode);
                            // can get more details such as response.headers
                        }
                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                    }
                };
                queue.add(request);
            }
        }

        private String getEntity() {
            return null;
        }

        private String postAsset() {
            MainActivity activity = activityRef.get();
            if (activity == null) {
                return null;
            }
            String url = url_base + String.format(activity.getString(R.string.LF_POST_assets), projectID);
            // TODO upload picture and return filepath
            return null;
        }
    }

    private static class WordFailedUploadTask extends AsyncTask<FilledWord, Void, Void> {
        private WordDao wordDao;

        WordFailedUploadTask(WordDao wordDao) {
            this.wordDao = wordDao;
        }
        @Override
        protected Void doInBackground(FilledWord... filledWords) {
            for (Word word : filledWords) {
                word.hadError = true;
            }
            wordDao.updateWords((Word[]) filledWords);
            return null;
        }
    }

}
