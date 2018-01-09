package org.sil.gatherwords;

import android.os.AsyncTask;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.util.List;

/**
 * Accesses the database in a worker thread.
 * Prevents blocking the main thread
 */

public class DatabaseAccess extends AsyncTask<String, Void, Void> {
	private AppDatabase appDatabase;
	private SessionDao sd;
	private WordDao wd;

	private Session[] sessions;
	private Word[] words;
	private List<String> stringList;
	private String comparisonString;

	/**
	 * Construct the task
	 * @param ad The database instance because this class does not have access to getApplicationContext()
	 */
	DatabaseAccess(AppDatabase ad){
		appDatabase = ad;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		sd = appDatabase.sessionDao();
		wd = appDatabase.wordDao();
	}

	/**
	 * Run all database access in a worker thread, so that the main thread does not get blocked
	 * @param strings Which function insert, select, update, delete to run
	 * @return null
	 */
	@Override
	protected Void doInBackground(String... strings) {
		appDatabase.beginTransaction();
		if (strings[0].equals("insert")) {
			sd.insertSession(sessions);
			wd.insertWords(words);
		}
		appDatabase.endTransaction();
		return null;
	}

	/**
	 * Reset the arrays
	 */
	@Override
	protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);
		words = null;
		sessions = null;
	}

	/**
	 * Overwrites the sessions array
	 * @param sessions the sessions for the next execute
	 * @return self
	 */
	public DatabaseAccess setSessions(Session... sessions) {
		this.sessions = sessions;
		return this;
	}

	/**
	 * Overwrites the words array
	 * @param words the words for the next execute
	 * @return self
	 */
	public DatabaseAccess setWords(Word... words) {
		this.words = words;
		return this;
	}

	/**
	 * Overwrites the stringList for some operations
	 * @param stringList the list of strings, like column names, for some operations
	 * @return self
	 */
	public DatabaseAccess setStringList(List<String> stringList){
		this.stringList = stringList;
		return this;
	}

	/**
	 * Overwrites the comparison string for some operations
	 * @param comparisonString the comparison string, like "x=y", for some operations
	 * @return self
	 */
	public DatabaseAccess setComparisonString(String comparisonString) {
		this.comparisonString = comparisonString;
		return this;
	}

	/**
	 * Alias for execute("insert")
	 */
	public void insert() {
		this.execute("insert");
	}
}

