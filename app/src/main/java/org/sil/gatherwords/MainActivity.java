package org.sil.gatherwords;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.SessionDAO;
import org.sil.gatherwords.room.SessionMeta;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private AccountManager am;
    private Account[] accounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.GET_ACCOUNTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.GET_ACCOUNTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
        am = AccountManager.get(getApplicationContext());

        accounts = am.getAccountsByType("com.google");
        Bundle options = new Bundle();
        if (accounts.length != 1) {
            Intent i = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, true, null, null, null, null);
            // Todo: crashes on the first run
            startActivityForResult(i, 1);
        }
        am.getAuthToken(accounts[0],
                "oauth2:openid",            // Auth scope
                options,                        // Authenticator-specific options
                this,                           // Your activity
                new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                new Handler(new OnError()));

        setContentView(R.layout.activity_main);

        final ListView sessionList = findViewById(R.id.session_list);

        sessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), EntryActivity.class);
                SessionMeta session = (SessionMeta) sessionList.getAdapter().getItem(i);
                intent.putExtra(SessionActivity.ARG_SESSION_ID, session.id);
                // Passes id of selected session into EntryActivity
                startActivity(intent);
            }
        });
        // Initializes the default preferences so that program does not crash on word creation
        //  if the settings have not been accessed
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Insure the ListView is updated when the back button is pressed.
        new FillSessionListTask(this).execute();
    }

    private static class FillSessionListTask extends AsyncTask<Void, Void, List<SessionMeta>> {
        private SessionDAO sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        FillSessionListTask(MainActivity mainActivity) {
            sDAO = AppDatabase.get(mainActivity).sessionDAO();
            mainActivityRef = new WeakReference<>(mainActivity);
        }

        @Override
        protected List<SessionMeta> doInBackground(Void... v) {
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<SessionMeta> sessions) {
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
        private List<SessionMeta> sessions;

        SessionListAdapter(LayoutInflater flate, List<SessionMeta> sessionList) {
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

            final SessionMeta session = sessions.get(i);

            // Get the date to prove that there is data being retrieved
            TextView labelText = convertView.findViewById(R.id.session_list_lable);
            labelText.setText(session.label);
            TextView date = convertView.findViewById(R.id.session_list_date);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            date.setText(df.format(session.date));
            TextView speaker = convertView.findViewById(R.id.session_list_speaker);
            speaker.setText(session.speaker);

            ProgressBar progressBar = convertView.findViewById(R.id.audio_progress);
            if (session.progress == null) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Hiding progress bar for empty session " + session.label + " (" + session.id + ")");
            }
            else {
                progressBar.setMax(session.progress.total);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(session.progress.completed, true);
                } else {
                    progressBar.setProgress(session.progress.completed);
                }
            }

            ImageButton editButton = convertView.findViewById(R.id.session_list_button_edit);
            editButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = inflater.getContext();
                    Intent intent = new Intent(context, SessionActivity.class);
                    intent.putExtra(SessionActivity.ARG_CREATING_SESSION, false);
                    intent.putExtra(SessionActivity.ARG_SESSION_ID, session.id);
                    context.startActivity(intent);
                }
            });

            ImageButton deleteButton = convertView.findViewById(R.id.session_list_button_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = inflater.getContext();
                    new DeleteSessionFromDB(context).execute(session.id);

                }
            });

            ImageButton uploadButton = convertView.findViewById(R.id.upload_session);
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Todo: Use UploadService for uploading
                    Util.sendGetTokenRequest(AccountManager.get(view.getContext()));
                }
            });
            return convertView;
        }
    }

    private static class DeleteSessionFromDB extends AsyncTask<Long, Void, List<SessionMeta>> {
        private SessionDAO sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        DeleteSessionFromDB(Context context) {
            sDAO = AppDatabase.get(context).sessionDAO();
            mainActivityRef = new WeakReference<>((MainActivity) context);
        }

        @Override
        protected List<SessionMeta> doInBackground(Long... sessionIDs) {
            sDAO.softDeleteSessions(new Date(), sessionIDs);
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<SessionMeta> sessions) {
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

    private static class UndoDeleteSessionFromDB extends AsyncTask<Void, Void, List<SessionMeta>> {
        private SessionDAO sDAO;
        private WeakReference<MainActivity> mainActivityRef;

        UndoDeleteSessionFromDB(MainActivity mainActivity) {
            sDAO = AppDatabase.get(mainActivity).sessionDAO();
            mainActivityRef = new WeakReference<>(mainActivity);
        }

        @Override
        protected List<SessionMeta> doInBackground(Void... v) {
            sDAO.undoLastDeleted();
            return sDAO.getAll();
        }

        @Override
        protected void onPostExecute(List<SessionMeta> sessions) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                SessionListAdapter sessionListAdapter = (SessionListAdapter) ((ListView) mainActivity.findViewById(R.id.session_list)).getAdapter();
                sessionListAdapter.sessions = sessions;
                sessionListAdapter.notifyDataSetChanged();
            }
        }
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            Log.d(TAG, "run: test");
        }
    }

    private class OnError implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            return false;
        }
    }
}
