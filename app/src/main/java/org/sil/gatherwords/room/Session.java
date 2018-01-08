package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Entity class for a language gathering session
 */

@Entity
public class Session {
	// Define the primary key
	@PrimaryKey(autoGenerate = true)
	public int id = 0;

	// Define the columns
	public String date;
	public String speaker;
	public String recorder;
	public String wordList; // JSON, arrays and lists are can't be stored
	public String language;
	public String listLanguages; // Also JSON
	public String location; // Possibly JSON, TODO: decide location format when added

	void insertWordlist(String wordlistName, Context context) {
		String json = null;
		try {
			InputStream is = context.getAssets().open("wordLists/" + wordlistName);
			int size = is.available();

			byte[] buffer = new byte[size];

			is.read(buffer);

			is.close();

			json = new String(buffer, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.wordList = json;
	}
}
