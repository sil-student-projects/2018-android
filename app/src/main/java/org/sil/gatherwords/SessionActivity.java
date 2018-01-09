package org.sil.gatherwords;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;

//TODO: Andrew freaking document this. Okay self, I gotchu.

public class SessionActivity extends AppCompatActivity {

	private FusedLocationProviderClient mFusedLocationClient;
	boolean locationEnabled;
	Location location;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session);

		locationEnabled = false;

		long date = System.currentTimeMillis() / 1000L;

		EditText dateField = findViewById(R.id.session_create_date);
		dateField.setText(Long.toString(date));
		dateField.setEnabled(false);


	}

	public void save_settings_fab_pressed(View view) {
		EditText name = findViewById(R.id.session_create_name);
		EditText eliciter = findViewById(R.id.session_create_eliciter);
		EditText speaker = findViewById(R.id.session_create_speaker);
		EditText date = findViewById(R.id.session_create_date);

		Session session = new Session();
		session.label = name.getText().toString();
		session.recorder = eliciter.getText().toString();
		session.speaker = speaker.getText().toString();
		// TODO: decide on internal format
		// session.date = date.getText().toString();

		// Create db instance and insert the session
		AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, getString(R.string.database_name)).build();
		new DatabaseAccess(appDatabase).setSessions(session).insert();

		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
	}

	public void input_location(View view) {
		if (locationEnabled) {
			locationEnabled = false;
			location = null;
		} else {
			mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
			location = new Location("vanDellen 362");
			locationEnabled = false;

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

	// TODO: Handle missing location permissions
	@SuppressLint("MissingPermission") // Suppress the location permissions warning
	private void setSessionLocation() {
		LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			SwitchCompat sw = findViewById(R.id.session_create_location_swtich);
			Snackbar mySnackbar = Snackbar.make(findViewById(R.id.session_create_layout),
					"Please enable location services to access this feature", Snackbar.LENGTH_LONG);
			mySnackbar.show();
			sw.setChecked(false);
			locationEnabled = false;
		} else {
			location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			//TODO: Something with this location

//            EditText locationField = findViewById(R.id.session_create_location);
//            locationField.setText(Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude()));
//            locationField.setEnabled(false);
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case 1: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					this.locationEnabled = true;
					setSessionLocation();

				} else {

				}
				return;
			}
		}
	}
}