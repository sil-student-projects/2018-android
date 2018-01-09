package org.sil.gatherwords;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * A simple {@link Fragment} specialized subclass. A regular activity, in this case, SettingsActivityCustom,
 * will host this fragment.  Fragments are modular sections of an activity, possessing its own lifecycle,
 * which allows it to be added or removed while the activity is running already.
 */
public class SettingsLocalizationFragment extends PreferenceFragmentCompat {


    //Constructor can be removed - fragment not displayed by itself.
    public SettingsLocalizationFragment() {
        // Required empty public constructor
    }


    /*
    Using this method because we are adding this fragment to the existing SettingsActivityCustom to display preferences.
    Preference fragment rooted to the preferences screen using "rootKey".
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState,
                                    String rootKey) {

        //Associate this fragment with the preferences.xml file.
        setPreferencesFromResource(R.xml.settings_localization_preferences, rootKey);
    }
}
