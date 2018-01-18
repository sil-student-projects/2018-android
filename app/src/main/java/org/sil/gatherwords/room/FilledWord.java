package org.sil.gatherwords.room;

import android.arch.persistence.room.Relation;
import android.graphics.Bitmap;

import java.util.List;

public class FilledWord {
    public long id;
    public long sessionID;
    public Bitmap picture;
    @Relation(parentColumn = "id", entityColumn = "wordID")
    public List<Meaning> meanings;
}
