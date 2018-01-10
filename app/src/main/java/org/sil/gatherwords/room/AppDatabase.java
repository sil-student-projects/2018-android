package org.sil.gatherwords.room;

import android.arch.persistence.room.RoomDatabase;

/**
 * The database class.
 */

@android.arch.persistence.room.Database(entities = {Session.class, Word.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
	public abstract SessionDao sessionDao();
	public abstract WordDao wordDao();
}
