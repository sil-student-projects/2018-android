package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * Entity class for each meaning of a word
 */
@Entity(foreignKeys = @ForeignKey(entity = Word.class,
                                  parentColumns = "id",
                                  childColumns = "wordID",
                                  onDelete = ForeignKey.CASCADE,
                                  onUpdate = ForeignKey.CASCADE),
        indices = @Index(value = {"wordID", "type"}, unique = true))
public class Meaning {
    @PrimaryKey (autoGenerate = true)
    public int id;

    public int wordID;

    // Potentially something like "en" or "IPA".
    // TODO: Should this field be renamed to something like "language"?
    public String type;
    // Description word/phrase for this type.
    public String data;

    public Meaning(int wordID, String type, String data) {
        this.wordID = wordID;
        this.type = type;
        this.data = data;
    }
}
