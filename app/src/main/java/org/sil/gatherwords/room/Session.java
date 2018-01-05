package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;

import java.util.Date;

/**
 * Entity class for a language gathering session
 */

@Entity
public class Session {
	// Define the primary key
	@PrimaryKey
	public int id;

	// Define the columns
	public Date date;
	public String speaker;
	public String recorder;
	public String[] wordList;
	public String language;
	public String[] listLanguages;
	public Location location;
}
