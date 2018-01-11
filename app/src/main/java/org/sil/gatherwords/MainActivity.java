package org.sil.gatherwords;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private AppDatabase ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ad = AppDatabase.get(this);
        setContentView(R.layout.activity_main);

        ListView sessionList = findViewById(R.id.session_list);
        sessionList.setAdapter(new SessionListAdapter());

        sessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), EntryActivity.class);
                startActivity(intent);
            }
        });

        //Load preference fragment.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainView, new org.sil.gatherwords.PreferenceFragment())
                    .commit();
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
                //Toast.makeText(this, "ADD!", Toast.LENGTH_SHORT).show();
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
        intent.putExtra(SessionActivity.CREATING_SESSION, true);
        startActivity(intent);
    }

    // TODO: Switch to CursorAdapter
    private class SessionListAdapter extends BaseAdapter {
        private List<Session> sessions = new ArrayList<>();
        DatabaseAccess databaseAccess;

        SessionListAdapter() {
            try {
                databaseAccess = new DatabaseAccess(ad);
                sessions = (List<Session>) databaseAccess.select("session").get();

            } catch (InterruptedException | ExecutionException e) {
                Log.e("SessionList Adapter", "There was a problem in reading from the database", e);
            }
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
                convertView = getLayoutInflater().inflate(
                    R.layout.session_list_item,
                    container,
                    false
                );
            }

            Session session = sessions.get(i);

            // Get the date to prove that there is data being retrieved
            TextView dateText = convertView.findViewById(R.id.date);
            dateText.setText(session.date);
            TextView location = convertView.findViewById(R.id.location);
            location.setText(session.location);
            TextView person = convertView.findViewById(R.id.person);
            person.setText(session.recorder);

            return convertView;
        }
    }
}
