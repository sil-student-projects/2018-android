package org.sil.gatherwords;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Meaning;
import org.sil.gatherwords.room.MeaningDao;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    public static final String ARG_CREATING_SESSION = "creating_session";
    public static final String ARG_ID = "id";

    // UI Elements
    private EditText dateField, timeField, timeZoneField, labelField, speakerField, eliciterField, locationField;
    private SimpleDateFormat dateSDF, timeSDF, timeZoneSDF;
    private SwitchCompat gpsSwitch;
    private SwitchCompat.OnCheckedChangeListener gpsSwitchChangeListener;
    private Spinner wordListSpinner;

    // Activity variables
    private boolean gpsEnabled, creatingNewSession;
    private String worldListToLoad;

    // Session variables
    private long sessionID;
    private FusedLocationProviderClient mFusedLocationClient; // Used to track gps through multiple methods
    private Date date;
    private Location gps;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        creatingNewSession = getIntent().getBooleanExtra(ARG_CREATING_SESSION, true);
        sessionID = getIntent().getLongExtra(ARG_ID, 0);

        gpsEnabled = false;

        wordListSpinner = findViewById(R.id.word_list_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.word_lists, R.layout.world_list_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wordListSpinner.setAdapter(adapter);
        wordListSpinner.setOnItemSelectedListener(this);

        // Sets the input_gps() function to run when the switch is clicked or slid across
        // Fixes bug where input_gps() was only run when clicked
        gpsSwitchChangeListener = new SwitchCompat.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton sw, boolean isChecked) {
            input_gps(findViewById(R.id.session_create_gps_swtich));
            }
        };
        gpsSwitch = findViewById(R.id.session_create_gps_swtich);
        gpsSwitch.setOnCheckedChangeListener(gpsSwitchChangeListener);

        // Find lable, speaker, eliciter EditTexts
        labelField = findViewById(R.id.session_create_name);
        speakerField = findViewById(R.id.session_create_speaker);
        eliciterField = findViewById(R.id.session_create_eliciter);

        // Set date, time, timezone, and gps fields
        dateField = findViewById(R.id.session_create_date);
        timeField = findViewById(R.id.session_create_time);
        timeZoneField = findViewById(R.id.session_create_time_zone);
        locationField = findViewById(R.id.session_create_location);

        dateSDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timeSDF = new SimpleDateFormat("HH:mm", Locale.US);
        timeZoneSDF = new SimpleDateFormat("z", Locale.US);

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
            new LoadSessionDataFromDB(this).execute(sessionID);

            // Hides the spinner
            wordListSpinner.setEnabled(false);
            wordListSpinner.setVisibility(View.GONE);
            TextView spinnerText = findViewById(R.id.word_list_spinner_text_view);
            spinnerText.setVisibility(View.GONE);

        }

        //FIXME: Because we are storing a Date object, changing the text fields has no effect
        dateField.setEnabled(false);
        timeField.setEnabled(false);
        timeZoneField.setEnabled(false);
    }

    //TODO: Save settings instead of write to new session
    /* Run when the FAB is pressed

     */
    public void save_settings_fab_pressed(View view) {
        if (creatingNewSession) {
            create_new_session();
        } else {
            update_session();
        }
    }

    /* Creates a new session in the database with the information in the current fields

     */
    private void create_new_session() {
        session = new Session();

        // TODO: Formatting for gps string?
        String gpsString = null;
        if ( gps != null ) {
            gpsString = Location.convert(gps.getLatitude(), Location.FORMAT_SECONDS)
                    + "," + Location.convert(gps.getLongitude(), Location.FORMAT_SECONDS);
        }
        // TODO: Implement iso8601 somewhere
        //String iso8601 = date.getText().toString() + "T" + time.getText().toString() + ":00" + timeZone.getText().toString();

        session.label = labelField.getText().toString();
        session.recorder = eliciterField.getText().toString();
        session.speaker = speakerField.getText().toString();
        session.location = locationField.getText().toString();
        session.gps = gpsString;


        // TODO: decide on internal format
        // session.date = date.getText().toString();

        // Acquire db instance and insert the session
        new InsertSessionsTask(this).execute(session);

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
        private WordDao wordDao;
        private Long sessionID;
        private AppDatabase db;
        private String wordList;
        private AssetManager assets;

        InsertSessionsTask(SessionActivity activity) {
            db = AppDatabase.get(activity.getApplicationContext());
            wordList = activity.worldListToLoad;
            wordDao = db.wordDao();
            sDAO = db.sessionDao();
            assets = activity.getApplicationContext().getAssets();
        }

        @Override
        protected Void doInBackground(Session... sessions) {
            long[] ids;
            ids = sDAO.insertSession(sessions);

            if (ids.length > 0) {
                sessionID = ids[0];
                insertFromAsset();
            }
            return null;
        }

        /**
         * Insert the words from the selected asset or exit if none selected
         */
        private void insertFromAsset() {
            if (wordList.isEmpty()) {
                // There was no file selected
                return;
            }

            // Begin the transaction
            db.beginTransaction();
            try {
                MeaningDao meaningDao = db.meaningDao();

                JSONArray jsonArray = new JSONArray(loadWordList());

                // Iterate through each word from the file
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    Word word = new Word();

                    // Link the word to the session that was just created
                    word.sessionID = sessionID;

                    // Insert
                    long wordID = wordDao.insertWord(word);

                    meaningDao.insertMeanings(
                        new Meaning(wordID, "entry", json.getString("entry")),
                        new Meaning(wordID, "pos", json.getString("pos")),
                        new Meaning(wordID, "notes", json.getString("notes"))
                    );
                }

                // Mark to commit the changes to the DB
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("InsertSessionsTask", "Exception while inserting words", e);
            }
            finally {
                // Commit or rollback the database
                db.endTransaction();
            }
        }

        /**
         * Read the selected file
         *
         * @return The contents of the file
         */
        private String loadWordList() {
            InputStream is = null;
            BufferedReader br = null;
            String file = null;
            try {
                is = assets.open("wordLists/" + wordList);
                br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                file = sb.toString();
            } catch (IOException e) {
                Log.e("InsertSessionsTask", "IOException while reading the word list file", e);
            } finally {
                // Assume the input stream is not null because it is used to construct the buffered reader
                if (br != null) {
                    try {
                        is.close();
                        br.close();
                    } catch (IOException e) {
                        Log.e("InsertSessionsTask", "IOException when closing buffered reader and stream", e);
                    }
                }
            }
            return file;
        }
    }

    /* Updates the session in the database with the information in the EditTexts

     */
    private void update_session() {
        String gpsString = null;
        if (gps != null) {
            gpsString = Location.convert(gps.getLatitude(), Location.FORMAT_SECONDS)
                    + "," + Location.convert(gps.getLongitude(), Location.FORMAT_SECONDS);
        }
        // TODO: Implement iso8601 somewhere
        //String iso8601 = date.getText().toString() + "T" + time.getText().toString() + ":00" + timeZone.getText().toString();

        session.label = labelField.getText().toString();
        session.recorder = eliciterField.getText().toString();
        session.speaker = speakerField.getText().toString();
        session.location = locationField.getText().toString();
        session.gps = gpsString;

        new UpdateSessionDataToDB(this).execute(session);

        //TODO: Update this to start EntryActivity when it can be used
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
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
            gpsEnabled = false;

            // If location permission is not granted, request it. Otherwise prep location getAll.
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

    // Receives and stores the device's current location
    // TODO: Handle missing location permissions. May be done or may not be
    @SuppressLint("MissingPermission") // Suppress the location permissions warning
    private void setSessionGPS() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SwitchCompat sw = findViewById(R.id.session_create_gps_swtich);
        // If location services are not enabled, tell the user to enable them and reset switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sw.setChecked(false);
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
                    R.string.en_loc_services, Snackbar.LENGTH_LONG);
            mySnackbar.show();
            gpsEnabled = false;
        //Otherwise grab location
        } else {
            gps = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if ( gps == null) {
                sw.setChecked(false);
                Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
                        "Device does not have a location. Please try again.", Snackbar.LENGTH_LONG);
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
                    gpsSwitch.setChecked(false);
                }
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        switch (pos) {
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

    /* Database access when loading information from a previously created session

     */
    private static class LoadSessionDataFromDB extends AsyncTask<Long, Void, List<Session>> {
        private SessionDao sDAO;
        private WeakReference<SessionActivity> sessionActivityRef;

        LoadSessionDataFromDB(SessionActivity sessionActivity) {
            sDAO = AppDatabase.get(sessionActivity).sessionDao();
            sessionActivityRef = new WeakReference<>(sessionActivity);
        }

        @Override
        protected List<Session> doInBackground(Long... ids) {
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
                sessionActivity.session = session;

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
                if (session.gps == null) {
                    sessionActivity.gpsSwitch.setChecked(false);
                // Else set to be checked. Disable checked listener to not toggle event
                } else {
                    sessionActivity.gpsSwitch.setOnCheckedChangeListener(null);
                    sessionActivity.gpsSwitch.setChecked(true);
                    sessionActivity.gpsSwitch.setHighlightColor(sessionActivity.getResources().getColor(R.color.colorAccent));
                    sessionActivity.gpsSwitch.setOnCheckedChangeListener(sessionActivity.gpsSwitchChangeListener);
                }
                // Disable the toggling of the switch. May want to change this
                sessionActivity.gpsSwitch.setEnabled(false);

            } else {
                Log.e("SessionActivity", "empty or size>1 Session[] grabbed from database");
            }
        }
    }

    /* Make a call to update the current session in the DB

     */
    private static class UpdateSessionDataToDB extends AsyncTask<Session, Void, Void> {
        private SessionDao sDAO;

        UpdateSessionDataToDB(SessionActivity sessionActivity) {
            sDAO = AppDatabase.get(sessionActivity).sessionDao();
        }

        @Override
        protected Void doInBackground(Session... sessions) {
            sDAO.updateSession(sessions);
            return null;
        }
    }
}
