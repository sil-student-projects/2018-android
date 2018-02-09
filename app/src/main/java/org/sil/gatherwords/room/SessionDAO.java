package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for the Session table
 */
@Dao
public interface SessionDAO {
    // SELECTS
    @Query("SELECT id, date, speaker, label, completed, total FROM session " +
            "LEFT JOIN (" +
                "SELECT COUNT(DISTINCT audio) completed, COUNT(*) total, sessionID " +
                "FROM word WHERE deletedAt IS NULL " +
                "GROUP BY sessionID) t1 " +
            "ON session.id = t1.sessionID " +
            "WHERE deletedAt IS NULL ORDER BY date DESC")
    List<SessionMeta> getAll();

    @Query("SELECT * FROM session WHERE id IN (:sessionIDs)")
    List<Session> getSessionsByID(Long... sessionIDs);

    @Query("SELECT * FROM session WHERE id = :sessionID")
    Session get(long sessionID);

    @Query("SELECT :columns FROM session WHERE :comparisonString")
    List<String> getWhere(List<String> columns, String comparisonString);

    // DELETE
    @Query("UPDATE session SET deletedAt = :deletedAt WHERE id IN (:sessionIDs)")
    void softDeleteSessions(Date deletedAt, Long... sessionIDs);

    // INSERT
    @Insert
    long[] insertSession(Session... sessions);

    // UPDATE
    @Update
    void updateSession(Session... sessions);

    @Query("UPDATE session SET deletedAt = NULL WHERE deletedAt = " +
           "(SELECT MAX(deletedAt) FROM session)")
    void undoLastDeleted();
}
