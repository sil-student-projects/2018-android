package org.sil.gatherwords;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SessionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    // Used to track location through multiple methods
    private FusedLocationProviderClient mFusedLocationClient;
    boolean locationEnabled;
    Location location;
    boolean creatingNewSession;
    AppDatabase db;
    AppCompatSpinner spinner;
    String worldListToLoad;

    public static final String CREATING_SESSION = "creating_session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        creatingNewSession = getIntent().getBooleanExtra(CREATING_SESSION, true);


        locationEnabled = false;

        spinner = findViewById(R.id.word_list_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.word_lists, R.layout.world_list_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Set date, time, and timezone fields
        EditText dateField, timeField, timeZoneField;
        dateField = findViewById(R.id.session_create_date);
        timeField = findViewById(R.id.session_create_time);
        timeZoneField= findViewById(R.id.session_create_time_zone);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        dateField.setText(sdf.format(date));
        sdf.applyPattern("HH:mm");
        timeField.setText(sdf.format(date));
        sdf.applyPattern("z");
        String timeZoneString = sdf.format(date);
        timeZoneField.setText(timeZoneString.substring(3, timeZoneString.length()));

        // Disables location and loaded word list if not creating a new session
        // May want to remove the location disable
        if ( !creatingNewSession ) {
            SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
            sw.setEnabled(false);
            spinner.setEnabled(false);

        }
    }

    //TODO: Do something legitimate with the data
    // Run when the FAB is pressed, right now it creates a session and returns to Main
    // Seconds not currently recorded
    public void save_settings_fab_pressed(View view) {
        EditText name = findViewById(R.id.session_create_name);
        EditText eliciter = findViewById(R.id.session_create_eliciter);
        EditText speaker = findViewById(R.id.session_create_speaker);
        EditText date = findViewById(R.id.session_create_date);
        EditText time = findViewById(R.id.session_create_time);
        EditText timeZone = findViewById(R.id.session_create_time_zone);

        String iso8601 = date.getText().toString() + "T" + time.getText().toString() + ":00" + timeZone.getText().toString();

        Session session = new Session();
        session.label = name.getText().toString();
        session.recorder = eliciter.getText().toString();
        session.speaker = speaker.getText().toString();

        // TODO: decide on internal format
        // session.date = date.getText().toString();

        // Acquire db instance and insert the session
        new DatabaseAccess(AppDatabase.get(this)).setSessions(session).insert();


        Intent i;
        if ( name.getText().toString().equals("shipit_") ) {
            i = new Intent(this, ShipItActivity.class);
        } else {
            i = new Intent(this, MainActivity.class);
        }
        startActivity(i);
    }

    // Run when the location switch is toggled
    public void input_location(View view) {
        // If location was set, remove it
        if (locationEnabled) {
            locationEnabled = false;
            location = null;
        } else {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            location = new Location("vanDellen 362");
            locationEnabled = false;

            // If location permission is not granted, request it. Otherwise prep location get.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            } else {
                locationEnabled = true;
            }

            if (locationEnabled) {
                setSessionLocation();
            }
        }
    }

    // Recieves and stores the device's current location
    // TODO: Handle missing location permissions
    @SuppressLint("MissingPermission") // Suppress the location permissions warning
    private void setSessionLocation() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // If location services are not enabled, tell the user to enable them and reset switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
            sw.setChecked(false);
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
                    "Please enable location services to access this feature", Snackbar.LENGTH_LONG);
            mySnackbar.show();
            locationEnabled = false;
        //Otherwise grab location
        } else {
            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //TODO: Something with this location
        }

    }

    // Runs when the user selects whether to grant a permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: { // Case 1 - Location
                // Permission granted
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.locationEnabled = true;
                    setSessionLocation();
                // Permission denied
                } else {
                    locationEnabled = false;
                    SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
                    sw.setChecked(false);
                }
                return;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        switch ( pos ) {
            case 0:
                worldListToLoad = "";
                break;
            case 1:
                worldListToLoad = "swadesh-100.json";
                break;
            case 2:
                worldListToLoad = "swadesh-207.json";
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        worldListToLoad = "";
    }
}
