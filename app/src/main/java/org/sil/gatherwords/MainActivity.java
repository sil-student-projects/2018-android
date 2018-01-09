package org.sil.gatherwords;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar actionBar = findViewById(R.id.app_action_support_bar);
        setSupportActionBar(actionBar);
    }

    //Method makes menu visible.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_action_bar_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Method allows linking to other activity and app components.
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
