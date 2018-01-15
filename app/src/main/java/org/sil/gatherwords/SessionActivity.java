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

    EditText dateField, timeField, timeZoneField, labelField, speakerField, eliciterField;
    SimpleDateFormat dateSDF, timeSDF, timeZoneSDF;
    // Used to track location through multiple methods
    private FusedLocationProviderClient mFusedLocationClient;
    boolean locationEnabled;
    Location location;
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

        locationEnabled = false;

        spinner = findViewById(R.id.word_list_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.word_lists, R.layout.world_list_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Sets the input_location() function to run when the switch is clicked or slid across
        // Fixes bug where input_location() was only run when clicked
        SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
        sw.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton sw, boolean isChecked) {
                input_location(findViewById(R.id.session_create_location_swtich));
            }
        });

        // Find lable, speaker, eliciter EditTexts
        labelField = findViewById(R.id.session_create_name);
        speakerField = findViewById(R.id.session_create_speaker);
        eliciterField = findViewById(R.id.session_create_eliciter);

        // Set date, time, and timezone fields
        dateField = findViewById(R.id.session_create_date);
        timeField = findViewById(R.id.session_create_time);
        timeZoneField= findViewById(R.id.session_create_time_zone);

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
        // Disables location and loaded word list if not creating a new session
        else {
            // Set fields to fields from database
            new GetInfoFromDB(this).execute(sessionID);

            // Disables changing of location storage
            // May want to allow this
            sw.setEnabled(false);

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
        new InsertSessionsTask(AppDatabase.get(this)).execute(session);

        Intent i;
        if ( name.getText().toString().equals("shipit_") ) {
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

            // If location permission is not granted, request it. Otherwise prep location getAll.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
            } else {
                locationEnabled = true;
            }

            if (locationEnabled) {
                setSessionLocation();
            }
        }
    }

    // Receives and stores the device's current location
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
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
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
    private static class GetInfoFromDB extends AsyncTask<Integer, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<SessionActivity> sessionActivityRef;

        GetInfoFromDB(SessionActivity sessionActivity) {
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
            } else {
                Log.e("SessionActivity", "empty or size>1 Session[] grabbed from database");
            }
        }
    }
}
