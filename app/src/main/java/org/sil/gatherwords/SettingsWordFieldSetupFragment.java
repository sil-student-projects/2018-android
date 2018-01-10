package org.sil.gatherwords;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsWordFieldSetupFragment extends PreferenceFragmentCompat {


    public SettingsWordFieldSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState,
                                    String rootKey) {
        setPreferencesFromResource(R.xml.settings_word_field_setup_preferences, rootKey);
    }
}
