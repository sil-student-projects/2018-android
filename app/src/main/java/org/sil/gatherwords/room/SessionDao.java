package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Data Access Object for the Session table
 */
@Dao
public interface SessionDao {
    // SELECTS
    @Query("SELECT * FROM session WHERE deletedAt IS NULL ORDER BY date DESC")
    List<Session> getAll();

	@Query("SELECT * FROM session WHERE id IN (:sessionIDs)")
	List<Session> getSessionsByID(Long... sessionIDs);

	@Query("SELECT * FROM session WHERE id = :sessionID")
	Session get(long sessionID);

	@Query("SELECT :columns FROM session WHERE :comparisonString")
	List<String> getWhere(List<String> columns, String comparisonString);

	// DELETE
	@Query("DELETE FROM session WHERE :comparisonString")
	int deleteColumn(String comparisonString);

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
