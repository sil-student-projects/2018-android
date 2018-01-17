package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Data Access Object for the word table
 */
@Dao
public interface MeaningDao {
    // SELECT
    @Query("SELECT * FROM meaning WHERE wordID = :wordID ORDER BY id ASC")
    List<Meaning> getAll(long wordID);

    @Query("SELECT * FROM meaning WHERE wordID = :wordID and type = :type")
    Meaning getByType(long wordID, String type);

    // UPDATES
    @Update
    void updateMeanings(Meaning... meanings);

    // DELETE
    @Delete
    void deleteMeanings(Meaning... meanings);

    // INSERT
    @Insert
    void insertMeanings(Meaning... meanings);
}
