package org.sil.gatherwords.room;

import android.arch.persistence.room.Relation;

import java.util.List;

public class FilledWord {
    public long id;
    public long sessionID;
    @Relation(parentColumn = "id", entityColumn = "wordID")
    public List<Meaning> meanings;
    public String semanticDomain;
}
