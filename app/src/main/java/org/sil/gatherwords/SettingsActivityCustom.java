package org.sil.gatherwords;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivityCustom extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings_custom);

        /*
        Allows the fragment to be displayed as the main content of the activity.
        Typical pattern to add a fragment to a host activity.
         */
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragmentCustom())
                .commit();
    }
}
