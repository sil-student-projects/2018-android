package org.sil.gatherwords;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

        final ListView sessionList = findViewById(R.id.session_list);
        sessionList.setAdapter(new SessionListAdapter());

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
    }

    public void onCreateSessionClick(View v) {
        Intent intent = new Intent(this, SessionActivity.class);
        // To distinguish between creating a session and viewing the settings of an old one
        intent.putExtra(SessionActivity.ARG_CREATING_SESSION, true);
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

            final Session session = sessions.get(i);

            // Get the date to prove that there is data being retrieved
            TextView labelText = convertView.findViewById(R.id.session_list_lable);
            labelText.setText(session.label);
            TextView date = convertView.findViewById(R.id.session_list_date);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            date.setText( df.format(session.date) );
            TextView speaker = convertView.findViewById(R.id.session_list_speaker);
            speaker.setText(session.speaker);

            ImageButton button = convertView.findViewById(R.id.session_list_button);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), SessionActivity.class);
                    intent.putExtra(SessionActivity.ARG_CREATING_SESSION, false);
                    intent.putExtra(SessionActivity.ARG_ID, session.id);
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}
