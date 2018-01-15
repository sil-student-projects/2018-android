package org.sil.gatherwords;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    EditText dateField, timeField, timeZoneField, labelField, speakerField, eliciterField, locationField;
    SimpleDateFormat dateSDF, timeSDF, timeZoneSDF;
    SwitchCompat sw;
    SwitchCompat.OnCheckedChangeListener OnCheckedChangeListener;
    // Used to track gps through multiple methods
    private FusedLocationProviderClient mFusedLocationClient;
    boolean gpsEnabled;
    Location gps;
    boolean creatingNewSession;
    int sessionID;
    Spinner spinner;
    String worldListToLoad;

    public static final String ARG_CREATING_SESSION = "creating_session";
    public static final String ARG_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        creatingNewSession = getIntent().getBooleanExtra(ARG_CREATING_SESSION, true);
        sessionID = getIntent().getIntExtra(ARG_ID, 0);

        gpsEnabled = false;

        spinner = findViewById(R.id.word_list_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.word_lists, R.layout.world_list_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Sets the input_gps() function to run when the switch is clicked or slid across
        // Fixes bug where input_gps() was only run when clicked
        OnCheckedChangeListener = new SwitchCompat.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton sw, boolean isChecked) {
                input_gps(findViewById(R.id.session_create_gps_swtich));
            }
        };
        sw = findViewById(R.id.session_create_gps_swtich);
        sw.setOnCheckedChangeListener(OnCheckedChangeListener);

        // Find lable, speaker, eliciter EditTexts
        labelField = findViewById(R.id.session_create_name);
        speakerField = findViewById(R.id.session_create_speaker);
        eliciterField = findViewById(R.id.session_create_eliciter);

        // Set date, time, timezone, and gps fields
        dateField = findViewById(R.id.session_create_date);
        timeField = findViewById(R.id.session_create_time);
        timeZoneField= findViewById(R.id.session_create_time_zone);
        locationField = findViewById(R.id.session_create_location);

        dateSDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timeSDF = new SimpleDateFormat("HH:mm", Locale.US);
        timeZoneSDF = new SimpleDateFormat("z", Locale.US);
        Date date;

        if (creatingNewSession) {
            date = new Date();
            dateField.setText(dateSDF.format(date));
            timeField.setText(timeSDF.format(date));
            String timeZoneString = timeZoneSDF.format(date);
            timeZoneField.setText(timeZoneString.substring(3)); // GMT+X:00 >>> +X:00
        }
        // Disables gps and loaded word list if not creating a new session
        else {
            // Set fields to fields from database
            new LoadSessionDataFromDB(this).execute(sessionID);


            // Hides the spinner
            spinner.setEnabled(false);
            spinner.setVisibility(View.GONE);
            TextView spinnerText = findViewById(R.id.word_list_spinner_text_view);
            spinnerText.setVisibility(View.GONE);

        }
    }

    //TODO: Save settings instead of write to new session
    // Run when the FAB is pressed, right now it creates a session and returns to Main
    // Seconds not currently recorded
    public void save_settings_fab_pressed(View view) {

        // TODO: Formatting for gps string?
        String gpsString = "";
        if ( gps != null ) {
            gpsString = Location.convert(gps.getLatitude(), Location.FORMAT_SECONDS)
                    + "," + Location.convert(gps.getLongitude(), Location.FORMAT_SECONDS);
        }
        // TODO: Implement iso8601 somewhere
        //String iso8601 = date.getText().toString() + "T" + time.getText().toString() + ":00" + timeZone.getText().toString();


        Session session = new Session();
        session.label = labelField.getText().toString();
        session.recorder = eliciterField.getText().toString();
        session.speaker = speakerField.getText().toString();
        session.location = locationField.getText().toString();
        session.gps = gpsString;


        // TODO: decide on internal format
        // session.date = date.getText().toString();

        // Acquire db instance and insert the session
        new InsertSessionsTask(AppDatabase.get(this)).execute(session);

        Intent i;
        if ( labelField.getText().toString().equals("shipit_") ) {
            // Easter egg
            i = new Intent(this, ShipItActivity.class);
        } else {
            i = new Intent(this, MainActivity.class);
        }
        startActivity(i);
    }

    private static class InsertSessionsTask extends AsyncTask<Session, Void, Void> {
        private SessionDao sDAO;

        InsertSessionsTask(AppDatabase db) {
            sDAO = db.sessionDao();
        }

        @Override
        protected Void doInBackground(Session... sessions) {
            sDAO.insertSession(sessions);
            return null;
        }
    }

    // Run when the gps switch is toggled
    public void input_gps(View view) {
        // If gps was set, remove it
        if (gpsEnabled) {
            gpsEnabled = false;
            gps = null;
        // Else set attempt to set gps
        } else {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            gps = new Location("vanDellen 362");
            gpsEnabled = false;

            // If gps permission is not granted, request it. Otherwise prep gps getAll.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
            } else {
                gpsEnabled = true;
            }

            if (gpsEnabled) {
                setSessionGPS();
            }
        }
    }

    // Receives and stores the device's current gps
    // TODO: Handle missing gps permissions. May be done or may not be
    @SuppressLint("MissingPermission") // Suppress the gps permissions warning
    private void setSessionGPS() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SwitchCompat sw = findViewById(R.id.session_create_gps_swtich);
        // If gps services are not enabled, tell the user to enable them and reset switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sw.setChecked(false);
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
                    "Please enable gps services to access this feature", Snackbar.LENGTH_LONG);
            mySnackbar.show();
            gpsEnabled = false;
        //Otherwise grab gps
        } else {
            gps = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if ( gps == null) {
                sw.setChecked(false);
                Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
                        "Device does not have a gps. Please try again.", Snackbar.LENGTH_LONG);
                mySnackbar.show();
                gpsEnabled = false;
            }
        }

    }

    // Runs when the user selects whether to grant a permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                // Permission granted
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.gpsEnabled = true;
                    setSessionGPS();
                // Permission denied
                } else {
                    gpsEnabled = false;
                    sw.setChecked(false);
                }
                break;
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


    //TODO: Rename
    private static class LoadSessionDataFromDB extends AsyncTask<Integer, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<SessionActivity> sessionActivityRef;

        LoadSessionDataFromDB(SessionActivity sessionActivity) {
            sDAO = AppDatabase.get(sessionActivity).sessionDao();
            sessionActivityRef = new WeakReference<>(sessionActivity);
        }

        @Override
        protected List<Session> doInBackground(Integer... ids) {
            return sDAO.getSessionsByID(ids);
        }

        @Override
        protected void onPostExecute(List<Session> sessions) {
            SessionActivity sessionActivity = sessionActivityRef.get();
            if (sessionActivity == null) {
                return;
            }

            if (sessions != null && sessions.size() == 1) {
                Session session = sessions.get(0);
                // Insert previous date
                Date date = session.date;
                sessionActivity.dateField.setText(sessionActivity.dateSDF.format(date));
                sessionActivity.timeField.setText(sessionActivity.timeSDF.format(date));
                String timeZoneString = sessionActivity.timeZoneSDF.format(date);
                sessionActivity.timeZoneField.setText(timeZoneString.substring(3, timeZoneString.length()));

                // Insert previous lable, speaker, eliciter
                sessionActivity.labelField.setText(session.label);
                sessionActivity.speakerField.setText(session.speaker);
                sessionActivity.eliciterField.setText(session.recorder);

                // Insert previous location and set switch based on whether gps was set
                sessionActivity.locationField.setText(session.location);
                // If no location was stored, show the switch to be unchecked
                if ( session.gps.equals("") ) {
                    sessionActivity.sw.setChecked(false);
                // Else set to be checked. Disable checked listener to not toggle event
                } else {
                    sessionActivity.sw.setOnCheckedChangeListener(null);
                    sessionActivity.sw.setChecked(true);
                    sessionActivity.sw.setHighlightColor(sessionActivity.getResources().getColor(R.color.colorAccent));
                    sessionActivity.sw.setOnCheckedChangeListener(sessionActivity.OnCheckedChangeListener);
                }
                // Disable the toggling of the switch. May want to change this
                sessionActivity.sw.setEnabled(false);

            } else {
                Log.e("SessionActivity", "empty or size>1 Session[] grabbed from database");
            }
        }
    }
}
