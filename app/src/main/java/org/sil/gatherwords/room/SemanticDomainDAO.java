package org.sil.gatherwords.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Data Access Object for the word table
 */
@Dao
public interface SemanticDomainDAO {
    // SELECT
    @Query("SELECT * FROM semanticDomain ORDER BY name ASC")
    List<SemanticDomain> getAll();

    @Query("SELECT * FROM semanticDomain WHERE name = :name")
    SemanticDomain getByName(String name);

    @Query("SELECT COUNT(*) FROM semanticDomain")
    int count();

    // INSERT
    @Insert
    void insertSemanticDomains(SemanticDomain... semanticDomains);
}
