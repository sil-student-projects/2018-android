package org.sil.gatherwords;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

        // TODO: This doesn't seem to work currently
        sessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), EntryActivity.class);
                Session session = (Session) sessionList.getAdapter().getItem(i);
                intent.putExtra(SessionActivity.ARG_ID, session.id);
                // Passes id of selected session into EntryActivity
                // TODO: EntryActivity currently does nothing with
                startActivity(intent);
            }
        });

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

            // Get the date to prove that there is data being retrieved
            TextView labelText = convertView.findViewById(R.id.session_list_lable);
            labelText.setText(session.label);
            TextView date = convertView.findViewById(R.id.session_list_date);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            date.setText(df.format(session.date));
            TextView speaker = convertView.findViewById(R.id.session_list_speaker);
            speaker.setText(session.speaker);

            ImageButton editButton = convertView.findViewById(R.id.session_list_button);
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
                    // TODO upload
                }
            });

            return convertView;
        }
    }
}
