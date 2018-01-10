package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Entity class for a vernacular gathering session
 */

@Entity
public class Session {
	// Define the primary key
	@PrimaryKey(autoGenerate = true)
	public int id = 0;

	// Define the columns
	public String date = generateDate();
	public String speaker;
	public String recorder;
	public String vernacular;
	public String listLanguages; // Also JSON
	public String location; // Human readable location
	public String gps; // google map gps string
	public String label;

	/**
	 * Generate the current unix timestamp
	 * @return The 64-bit timestamp
	 */
	public String generateDate(){
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		return df.format(new Date());
	}
}
