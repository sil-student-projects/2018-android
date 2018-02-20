package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for the word table
 */
@Dao
public interface WordDAO {
    // SELECT
    @Query("SELECT id FROM word WHERE sessionID = :sessionID AND deletedAt IS NULL ORDER BY id ASC")
    List<Integer> getIDsForSession(int sessionID);

    @Query("SELECT * FROM word WHERE id = :wordID")
    Word get(int wordID);

    @Query("SELECT word.id, sessionID, audio, picture, " +
                "semanticDomain.name AS semanticDomain " +
            "FROM word " +
            "LEFT JOIN semanticDomain ON semanticDomainID = semanticDomain.id " +
            "WHERE word.id = :wordID")
    @Transaction
    FilledWord getFilled(int wordID);

    // UPDATES
    @Update
    void updateWords(Word... words);

    @Query("UPDATE word SET deletedAt = NULL WHERE deletedAt = " +
            "(SELECT MAX(deletedAt) FROM word WHERE sessionID = :sessionID)")
    int undoLastDeleted(int sessionID);

    // DELETE
    @Query("UPDATE word SET deletedAt = :deletedAt WHERE id IN (:wordIDs)")
    void softDeleteWords(Date deletedAt, Integer... wordIDs);

    // INSERT
    @Insert
    long insertWord(Word word);
}
