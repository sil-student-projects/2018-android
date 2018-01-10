package org.sil.gatherwords;

import android.os.AsyncTask;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Accesses the database in a worker thread.
 * Prevents blocking the main thread
 */

public class DatabaseAccess extends AsyncTask<String, Void, List<?>> {
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
	protected List<?> doInBackground(String... strings) {
		List<?> result = null;
		appDatabase.beginTransaction();
		if (strings[0].equals("insert")) {
			if (sessions != null) {
				sd.insertSession(sessions);
			}

			if (words != null) {
				wd.insertWords(words);
			}
		}
		if (strings[0].equals("select")) {
			if (strings[1].equals("session")) {
				if (stringList == null && comparisonString == null) {
					result = sd.getAll();
				} else if (stringList == null && comparisonString != null) {
					stringList = Arrays.asList("id", "label", "date", "speaker", "recorder", "vernacular", "listLanguages", "location", "gps");
					result = sd.getWhere(stringList, comparisonString);
				} else if (stringList != null && comparisonString == null) {
					result = sd.getAll(stringList);
				} else {
					result = sd.getWhere(stringList, comparisonString);
				}
			}
		}
		appDatabase.endTransaction();
		return result;
	}

	/**
	 * Reset the arrays
	 */
	@Override
	protected void onPostExecute(List<?> result) {
		super.onPostExecute(result);
		words = null;
		sessions = null;
		stringList = null;
		comparisonString = null;
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
	public AsyncTask<String, Void, List<?>> insert() {
		return this.execute("insert");
	}

	public AsyncTask<String, Void, List<?>> select(String entity) {
		return this.execute("select", entity);
	}
}