package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Data Access Object for the word table
 */

@Dao
public interface WordDao {
	// SELECT
	@Query("SELECT * FROM word")
	List<Word> getAll();

	@Query("SELECT :columns FROM word")
	List get(List<String> columns);

	@Query("SELECT :columns FROM word WHERE :whereColumn :compare :value")
	List getWhere(List<String> columns, String whereColumn, String compare, String value);

	// UPDATES
	@Query("UPDATE word SET :column = :value WHERE :whereColumn :compare :whereValue ")
	int updateColumnWhere(String column, String value, String whereColumn, String compare, String whereValue);

	@Query("UPDATE word SET :column = :value")
	int updateColumn(String column, String value);

	// DELETE
	@Query("DELETE FROM word WHERE :column :compare :value")
	int deleteColumn(String column, String compare, String value);

	// INSERT
	@Insert
	int insertSession(Word session);
}
