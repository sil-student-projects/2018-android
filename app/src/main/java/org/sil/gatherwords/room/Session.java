package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Entity class for a vernacular gathering session
 */

@Entity
public class Session {
	// Define the primary key
	@PrimaryKey(autoGenerate = true)
	public int id = 0;

	// Define the columns
	public Long date = generateDate();
	public String speaker;
	public String recorder;
	public String vernacular;
	public String listLanguages; // Also JSON
	public String location; // Human readable location
	public String gps; // google map gps string

	/**
	 * Generate the current unix timestamp
	 * @return The 64-bit timestamp
	 */
	public Long generateDate(){
		return System.currentTimeMillis()/1000L;
	}
}
