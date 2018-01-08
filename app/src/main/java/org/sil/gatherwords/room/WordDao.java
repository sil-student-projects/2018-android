package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
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
	@Query("SELECT * FROM word")
	List<Word> getAll();

	@Query("SELECT :columns FROM word")
	List<String> get(List<String> columns);

	@Query("SELECT :columns FROM word WHERE :comparisonStatement")
	List<String> getWhere(List<String> columns, String comparisonStatement);

	// UPDATES
	@Update
	int updateWords(Word... words);

	// DELETE
	@Query("DELETE FROM word WHERE :comparisonStatement")
	int deleteColumn(String comparisonStatement);

	// INSERT
	@Insert
	void insertWords(Word... words);
}
