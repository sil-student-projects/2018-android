package org.sil.gatherwords;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivityFragmentHost extends AppCompatActivity {

    /*
    Static variable assigned to the key-value pair for the preference.
    Ensure string matches the exact identifier as found in strings.xml.
    */

    public static final String
            KEY_LIST_PREF_LOCALIZATION_SELECT = "list_pref_localization";
    public static final String
            KEY_CHECKBOX_PREF_LANGUAGE_OPTIONS = "list_pref_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings_custom);

        /*
        Allows the fragment to be displayed as the main content of the activity.
        Typical pattern to add a fragment to a host activity.
         */

        //Enable localization fragment.
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new SettingsLocalizationFragment())
                .commit();

        //FIXME - crashing at the moment.  Disabled for now.
        //Enable word field setup fragment.
//        getSupportFragmentManager().beginTransaction()
//                .add(android.R.id.content, new SettingsWordFieldSetupFragment())
//                .commit();
    }
}
