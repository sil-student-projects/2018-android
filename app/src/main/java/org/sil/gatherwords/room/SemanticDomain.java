package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

// TODO: What keys/constraints do we actually need.
@Entity(indices = @Index(value = {"name"}, unique = true))
public class SemanticDomain {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String guid;
    public String key;
    public String abbr;
    public String name;
    public String description;

    @Override
    public String toString() {
        return name;
    }
}
