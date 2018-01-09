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
	public AppDatabase appDatabase;
	public Session[] sessions;
	public Word[] words;
	public List<String> stringList;
	public String comparisonString;

	private SessionDao sd;
	private WordDao wd;

	DatabaseAccess(AppDatabase ad){
		appDatabase = ad;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		sd = appDatabase.sessionDao();
		wd = appDatabase.wordDao();
	}

	@Override
	protected Void doInBackground(String... strings) {
		if (strings[0].equals("insert")) {
			sd.insertSession(sessions);
			wd.insertWords(words);
		}
		return null;
	}

}

