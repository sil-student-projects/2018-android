package org.sil.gatherwords;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Begin code by Joseph Jinn */
        //////////////////////////////////////////////////////////////////////////////////////////////

        //Set preferences to default values.
        PreferenceManager.setDefaultValues(this, R.xml.settings_localization_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_word_field_setup_preferences, false);

        //Get the shared preferences file for the app.
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        //FIXME - ensure if no value is found, it is actually set to the default value.
        //Get value of this preference.  If no value found, set to default value.
        String listPrefLocalization = sharedPref.getString
                (SettingsActivityFragmentHost.KEY_LIST_PREF_LOCALIZATION_SELECT, "1");
//        String checkBoxPrefLanguage = sharedPref.getString
//                (SettingsActivityFragmentHost.KEY_CHECKBOX_PREF_LANGUAGE_OPTIONS, "1");

        /* End code by Joseph Jinn */
        //////////////////////////////////////////////////////////////////////////////////////////////

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

    /* Begin code by Joseph Jinn */
    //////////////////////////////////////////////////////////////////////////////////////////////

    //Method makes menu items visible.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_action_bar_items, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /*
    Method allows linking to other activity and app components.
    Add additional cases in switch statement as necessary.
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            //Links to the preferences/settings page.
            case R.id.app_action_bar_settings:

                Toast.makeText(getApplicationContext(), "Settings", Toast.LENGTH_SHORT).show();

                Intent settingsIntent = new Intent(this, SettingsActivityFragmentHost.class);
                startActivity(settingsIntent);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /* End code by Joseph Jinn */
    //////////////////////////////////////////////////////////////////////////////////////////////

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
