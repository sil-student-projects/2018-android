package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

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
	List getAll(List<String> columns);

	@Query("SELECT :columns FROM session WHERE :whereColumn :compare :value")
	List getWhere(List<String> columns, String whereColumn, String compare, String value);

	// UPDATES
	@Query("UPDATE session SET :column = :value WHERE :whereColumn :compare :whereValue ")
	int updateColumnWhere(String column, String value, String whereColumn, String compare, String whereValue);

	@Query("UPDATE session SET :column = :value")
	int updateColumn(String column, String value);

	// DELETE
	@Query("DELETE FROM session WHERE :column :compare :value")
	int deleteColumn(String column, String compare, String value);

	// INSERT
	@Insert
	int insertSession(Session session);
}
