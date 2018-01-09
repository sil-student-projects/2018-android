package org.sil.gatherwords;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivityCustom extends AppCompatActivity {

    /*
    Static variable assigned to the key-value pair for the preference.
    Ensure string matches the exact identifier as found in strings.xml.
    */

    public static final String
            KEY_LIST_PREF_LANGUAGE_SELECT = "list_pref_1anguage";

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
