package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Entity class for a vernacular gathering session
 */
@Entity
public class Session {
	// Define the primary key
	@PrimaryKey(autoGenerate = true)
	public long id;

	// Define the columns
	public Date date = new Date();
	public String speaker;
	public String recorder;
	public String vernacular;
	public String listLanguages; // Also JSON
	public String location; // Human readable location
	public String gps; // google map gps string
	public String label;
	public Date deletedAt = null;

	// Mark if the session is queued for upload
	public boolean toBeUploaded = false;
	public String projectID = "";
	public JSONObject config;
	public JSONObject inputSystems;

	/**
	 * Generate the current unix timestamp
	 * @return The 64-bit timestamp
	 * Not currently used
	 */
	public String generateDate(){
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", // Quoted "Z" to indicate UTC, no timezone offset
            Locale.US
        );
		df.setTimeZone(tz);
		return df.format(new Date());
	}

	@Override
	public String toString() {
		String[] out = {"ID: " + String.valueOf(id), "Date: " + date, "Speaker: " + speaker, "Recorder: " + recorder, "Vernacular: " + vernacular, "listLanguage: " + listLanguages, "Location: " + location, "GPS: " + gps, "Location: " + location};
		return Arrays.toString(out);
	}
}
