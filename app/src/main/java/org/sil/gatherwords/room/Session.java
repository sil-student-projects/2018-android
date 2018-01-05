package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Entity class for a language gathering session
 */

@Entity
public class Session {
	// Define the primary key
	@PrimaryKey
	public int id;

	// Define the columns
	public String date;
	public String speaker;
	public String recorder;
	public String wordList; // JSON, arrays and lists are can't be stored
	public String language;
	public String listLanguages; // Also JSON
	public String location;
}
