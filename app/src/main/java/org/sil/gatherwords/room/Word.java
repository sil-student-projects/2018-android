package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

/**
 * Entity class for each word of a session
 */
@Entity(foreignKeys = @ForeignKey(entity = Session.class,
                                  parentColumns = "id",
                                  childColumns = "sessionID",
                                  onDelete = ForeignKey.CASCADE,
                                  onUpdate = ForeignKey.CASCADE),
        indices = @Index("sessionID"))
public class Word {
    @PrimaryKey (autoGenerate = true)
    public long id;

    public long sessionID;

    public Date deletedAt;

    public String semanticDomain = "None";

    // Only filename stored (no path).
    public String audio; // NOTE: LF does NOT support multiple audio files

    // Only filename stored (no path).
    public String picture; // NOTE: LF DOES support multiple picture files
}
