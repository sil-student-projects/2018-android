package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
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
    @Query("SELECT id FROM word WHERE sessionId = :sessionID AND deletedAt IS NULL ORDER BY id ASC")
    List<Long> getIDsForSession(long sessionID);

    @Query("SELECT * FROM word WHERE id = :wordID")
    Word get(long wordID);

    @Query("SELECT word.id, sessionID, audio, picture, " +
                "semanticDomain.name AS semanticDomain " +
            "FROM word " +
            "LEFT JOIN semanticDomain ON semanticDomainID = semanticDomain.id " +
            "WHERE word.id = :wordID")
    @Transaction
    FilledWord getFilled(long wordID);

    @Query("SELECT :columns FROM word WHERE :comparisonStatement")
    List<String> getWhere(List<String> columns, String comparisonStatement);

    // UPDATES
    @Update
    void updateWords(Word... words);

    @Query("UPDATE word SET deletedAt = NULL WHERE deletedAt = " +
            "(SELECT MAX(deletedAt) FROM word WHERE sessionID = :sessionID)")
    long undoLastDeleted(long sessionID);

    // DELETE
    @Delete
    void deleteWords(Word... words);

    @Query("UPDATE word SET deletedAt = :deletedAt WHERE id IN (:wordIDs)")
    void softDeleteWords(Date deletedAt, Long... wordIDs);

    // INSERT
    @Insert
    long insertWord(Word word);

    @Insert
    void insertWords(Word... words);
}
