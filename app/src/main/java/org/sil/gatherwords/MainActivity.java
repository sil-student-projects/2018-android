package org.sil.gatherwords;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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
import android.widget.Toast;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Moved to onResume() so that the ListView is reconstructed when the back button was pressed
        final ListView sessionList = findViewById(R.id.session_list);

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

    private static class FillSessionListTask extends AsyncTask<Void, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        FillSessionListTask(MainActivity mainActivity) {
            sDAO = AppDatabase.get(mainActivity).sessionDao();
            mainActivityRef = new WeakReference<>(mainActivity);
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
                    sessions
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

        SessionListAdapter(LayoutInflater flate, List<Session> sessionList) {
            inflater = flate;
            sessions = sessionList;
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

            ImageButton deleteButton = convertView.findViewById(R.id.session_list_button_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = inflater.getContext();
                    session.deletedAt = new Date();
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
            sDAO.updateSession(sessions);
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<Session> sessions) {
            MainActivity mainActivity = mainActivityRef.get();
            if ( mainActivity != null ) {
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
        mySnackbar.setAction( R.string.undo, new UndoListener() );
        mySnackbar.show();
    }

    public class UndoListener implements View.OnClickListener {
        @Override
        public void onClick( View v ) {
            //Toast.makeText(MainActivity.this, "I am calling the DB", Toast.LENGTH_SHORT).show();
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
            if ( mainActivity != null ) {
                SessionListAdapter sessionListAdapter = (SessionListAdapter) ((ListView) mainActivity.findViewById(R.id.session_list)).getAdapter();
                sessionListAdapter.sessions = sessions;
                sessionListAdapter.notifyDataSetChanged();
            }
        }
    }
}
