package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

/**
 * Data Access Object for the word table
 */
@Dao
public interface MeaningDAO {
    // SELECT
    @Query("SELECT * FROM meaning WHERE wordID = :wordID and type = :type")
    Meaning getByType(int wordID, String type);

    // UPDATES
    @Update
    void updateMeanings(Meaning... meanings);

    // INSERT
    @Insert
    void insertMeanings(Meaning... meanings);
}
