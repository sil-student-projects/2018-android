package org.sil.gatherwords;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    public void onCreateSessionClick(View v) {
        Intent intent = new Intent(this, SessionActivity.class);
        startActivity(intent);
    }

    // TODO: Switch to CursorAdapter
    private class SessionListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int i) {
            return null;
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

            TextView dateText = convertView.findViewById(R.id.date);
            dateText.setText("1:42 PM 1/5/2018");

            TextView locationText = convertView.findViewById(R.id.location);
            locationText.setText("Thailand");

            TextView personText = convertView.findViewById(R.id.person);
            personText.setText("Example Person");

            return convertView;
        }
    }
}
