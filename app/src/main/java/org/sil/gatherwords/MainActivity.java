package org.sil.gatherwords;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set custom support action bar.
        Toolbar actionBar = findViewById(R.id.app_action_support_bar);
        setSupportActionBar(actionBar);

        //Set preferences to default values.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Get the shared preferences file for the app.
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        //FIXME - ensure if no value is found, it is actually set to the default value.
        //Get value of this preference.  If no value found, set to default value.
        String listPrefLanguage = sharedPref.getString
                (SettingsActivityCustom.KEY_LIST_PREF_LANGUAGE_SELECT, "1");

        //Display value of this preference (for testing purposes).
        //Toast.makeText(this, listPrefLanguage, Toast.LENGTH_SHORT).show();
    }

    //Method makes menu visible.
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

                Intent settingsIntent = new Intent(this, SettingsActivityCustom.class);
                startActivity(settingsIntent);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
