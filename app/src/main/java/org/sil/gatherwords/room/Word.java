package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.Bitmap;

import java.sql.Blob;
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

    @Ignore
    public Blob audio; // NOTE: LF does NOT support multiple audio files

    public Bitmap picture; // NOTE: LF DOES support multiple picture files

    // Mark true if uploaded, false otherwise
    public boolean uploadComplete = false;
    public boolean hadError = false;
}
