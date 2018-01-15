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
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SessionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    // Used to track location through multiple methods
    private FusedLocationProviderClient mFusedLocationClient;
    boolean locationEnabled;
    Location location;
    boolean creatingNewSession;
    AppDatabase db;
    Spinner spinner;
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
                R.array.word_lists, R.layout.world_list_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Set date, time, and timezone fields
        EditText dateField, timeField, timeZoneField;
        dateField = findViewById(R.id.session_create_date);
        timeField = findViewById(R.id.session_create_time);
        timeZoneField= findViewById(R.id.session_create_time_zone);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
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

        // Acquire db instance, insert the session and get the id of the session
        AsyncTask<Session, Void, List<Long>> task = new InsertSessionsTask(AppDatabase.get(this)).execute(session);
        InsertWordsTask wordsTask =new InsertWordsTask(task, this);
        wordsTask.execute();

        Intent i;
        if ( name.getText().toString().equals("shipit_") ) {
            // Easter egg
            i = new Intent(this, ShipItActivity.class);
        } else {
            i = new Intent(this, MainActivity.class);
        }
        startActivity(i);
    }

    private static class InsertSessionsTask extends AsyncTask<Session, Void, List<Long>> {
        private SessionDao sDAO;

        InsertSessionsTask(AppDatabase db) {
            sDAO = db.sessionDao();
        }

        @Override
        protected List<Long> doInBackground(Session... sessions) {
            return sDAO.insertSession(sessions);
        }

    }

    /**
     * Async task for inserting words from the selected word list
     */
    private static class InsertWordsTask extends AsyncTask<Void, Double, List<Long>> {
        private WordDao wordDao;
        private AsyncTask<Session, Void, List<Long>> task;
        private final ThreadLocal<SessionActivity> sessionActivity = new ThreadLocal<>();
        private Long sessionID;
        private int maxProgress;

        InsertWordsTask(AsyncTask<Session, Void, List<Long>> sessionTask, SessionActivity activity) {
            sessionActivity.set(activity);
            wordDao = sessionActivity.get().db.wordDao();
            task = sessionTask;
        }

        @Override
        protected List<Long> doInBackground(Void... voids) {
            List<Long> ids = new ArrayList<>();
            try {
                // Wait for the insert session task to finish and get the result (the id(s) of the session)
                ids = task.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e("InsertWordsTask", "Getting the id of the session failed", e);
            }
            if (ids.size() == 1) {
                sessionID = ids.get(0);
            }
            insertFromAsset();

            return null;
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);
            double progress = (values[0] / maxProgress)*100;
            CharSequence sequence = new DecimalFormat("##.##%").format(progress);
            // Too slow!!! TODO get quick progress updates
//            Toast.makeText(sessionActivity.getApplicationContext(), sequence, Toast.LENGTH_SHORT).show();
        }

        /**
         * Insert the words from the selected asset or exit if none selected
         */
        private void insertFromAsset() {
            if (sessionActivity.get().worldListToLoad.equals("")) {
                // There was no file selected
                return;
            }

            // Begin the transaction
            sessionActivity.get().db.beginTransaction();
            try {
                JSONArray jsonArray = new JSONArray(loadWordList());
                maxProgress = jsonArray.length();

                // Iterate through each word from the file
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    Word word = new Word();

                    // Link the word to the session that was just created
                    word.sessionId = sessionID;
                    word.meanings = json.getString("entry");

                    // Insert
                    wordDao.insertWords(word);
                    this.publishProgress((double) i);
                }

                // Mark to commit the changes to the DB
                sessionActivity.get().db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("InsertWordsTask", "Exception while inserting words", e);
            }
            finally {
                // Commit or rollback the database
                sessionActivity.get().db.endTransaction();
            }
        }

        /**
         * Read the selected file
         *
         * @return The contents of the file
         */
        private String loadWordList() {
            String file = "";
            try {
                InputStream is = sessionActivity.get().getApplicationContext().getAssets().open("wordLists/" + sessionActivity.get().worldListToLoad);
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();
                file = new String(buffer, "UTF-8");
            } catch (Exception e) {
                Log.e("InsertWordsTask", "Exception while loading the word list", e);
            }

            return file;
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
}
