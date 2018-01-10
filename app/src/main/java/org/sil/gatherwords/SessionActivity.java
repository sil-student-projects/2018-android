package org.sil.gatherwords;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;

public class SessionActivity extends AppCompatActivity {
    // Used to track location through multiple methods
    boolean locationEnabled;
    Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        locationEnabled = false;

        Calendar cal = Calendar.getInstance();
        // Grab the box to display the date
        EditText dateField, timeField, timeZoneField;
        dateField= findViewById(R.id.session_create_date);
        // Get today's day and format
        String dayOfMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        dayOfMonth = addLeadingZero(dayOfMonth);
        // Get the current month and format
        int month = cal.get(Calendar.MONTH);
        month++;
        String monthString = String.valueOf(month);
        monthString = addLeadingZero(monthString);

        dateField.setText(cal.get(Calendar.YEAR) + "-" + monthString + "-" + dayOfMonth);

        // Get the current time and format it
        timeField= findViewById(R.id.session_create_time);
        String hourOfDay = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        hourOfDay = addLeadingZero(hourOfDay);
        String minute = String.valueOf(cal.get(Calendar.MINUTE));
        minute = addLeadingZero(minute);
        timeField.setText(hourOfDay + ":" + minute);

        // Get the offset from utc and format it
        timeZoneField= findViewById(R.id.session_create_time_zone);
        String utc;
        Date date = new Date();
        utc = String.valueOf((cal.getTimeZone().getOffset(date.getTime() / 1000)) / 3600);
        utc = formatUTC(utc);
        timeZoneField.setText(utc);

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
        session.setData("name", name.getText().toString());
        session.setData("elictier", eliciter.getText().toString());
        session.setData("speaker", speaker.getText().toString());
        session.setData("date", date.getText().toString());

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    // Run when the location switch is toggled
    public void input_location(View view) {
        // If location was set, remove it
        if (locationEnabled) {
            locationEnabled = false;
            location = null;
        } else {
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
    private void setSessionLocation() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // If location services are not enabled, tell the user to enable them and reset switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
            sw.setChecked(false);
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_scroll_lin),
                    "Please enable location services to access this feature", Snackbar.LENGTH_LONG);
            mySnackbar.show();
            locationEnabled = false;
        //Otherwise grab location
        } else {
            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //TODO: Something with this location
        }

    }

    // The calendar returns many single digit numbers and this method adds a leading 0 when appropriate
    private String addLeadingZero(String s) {
        if (s.length() < 2) {
            return ("0" + s);
        }
        return s;
    }

    // Creates a ISO 8601 UTC offset from system time
    private String formatUTC(String utc) {
        utc = utc.substring(0, utc.length() - 1 );
        if ( !utc.contains("-") ) {
            utc = "+" + utc;
        }
        if (utc.substring(1, utc.length()).length() < 4 ) {
            utc = utc.substring(0, 1) + "0" + utc.substring(1, utc.length());
        }
        utc = utc.replaceAll("50", "30");
        utc = utc.substring(0,3) + ":" + utc.substring(3, utc.length());

        return utc;
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
}
