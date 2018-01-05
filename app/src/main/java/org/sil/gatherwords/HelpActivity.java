package org.sil.gatherwords;

import android.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toolbar;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        //Include app action bar as part of the help page.
        Toolbar actionBar = (Toolbar) findViewById(R.id.app_action_bar);
        setActionBar(actionBar);

        //Display the back button.
        //TODO - make it work.
        ActionBar getActionBar = getActionBar();
        getActionBar.setDisplayShowHomeEnabled(true);
        getActionBar.setDisplayHomeAsUpEnabled(true);
    }
}
