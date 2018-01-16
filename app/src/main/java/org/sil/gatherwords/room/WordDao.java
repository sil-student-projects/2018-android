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
public interface WordDao {
    // SELECT
    @Query("SELECT id FROM word WHERE sessionId = :sessionID ORDER BY id ASC")
    List<Long> getIDsForSession(long sessionID);

    @Query("SELECT * FROM word WHERE id = :wordID")
    Word get(long wordID);

    @Query("SELECT :columns FROM word WHERE :comparisonStatement")
    List<String> getWhere(List<String> columns, String comparisonStatement);

    // UPDATES
    @Update
    void updateWords(Word... words);

    // DELETE
    @Delete
    void deleteWords(Word... words);

    // INSERT
    @Insert
    long insertWord(Word word);

    @Insert
    void insertWords(Word... words);
}
