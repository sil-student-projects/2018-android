package org.sil.gatherwords.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import org.sil.gatherwords.R;

/**
 * The database class.
 */
@Database(entities = {Session.class, Word.class, Meaning.class}, version = 15)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    // Singleton db instance; avoids expensive init.
    private static AppDatabase INSTANCE = null;

    private static void init(Context context) {
        INSTANCE = Room.databaseBuilder(
            context,
            AppDatabase.class,
            context.getString(R.string.database_name)
        )
        .fallbackToDestructiveMigration()
        .build();
    }

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            init(context);
        }
        return INSTANCE;
    }

    public abstract SessionDAO sessionDAO();
    public abstract WordDAO wordDAO();
    public abstract MeaningDAO meaningDAO();
}
