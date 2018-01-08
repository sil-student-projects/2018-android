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
	@Query("SELECT * FROM session")
	List<Session> getAll();

	@Query("SELECT :columns FROM session")
	List<String> getAll(List<String> columns);

	@Query("SELECT :columns FROM session WHERE :comparisonString")
	List<String> getWhere(List<String> columns, String comparisonString);

	// DELETE
	@Query("DELETE FROM session WHERE :comparisonString")
	int deleteColumn(String comparisonString);

	// INSERT
	@Insert
	void insertSession(Session... sessions);

	// Update
	@Update
	int updateSession(Session... sessions);

}
