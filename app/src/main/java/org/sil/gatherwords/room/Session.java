package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.Locale;

/**
 * Entity class for a vernacular gathering session
 */
@Entity
public class Session {
    // Define the primary key
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Define the columns
    public Date date = new Date();
    public String speaker;
    public String recorder;
    public String vernacular;
    public String listLanguages; // Also JSON
    public String location; // Human readable location
    public String gps; // google map gps string
    public String label;
    public Date deletedAt = null;
    public Status state = Status.Editing;

    @Override
    public String toString() {
        return String.format(
            Locale.US,
            "ID: %d, DATE: %s, Speaker: %s, Recorder: %s, Vernacular: %s, ListLanguage: %s, Location: %s, GPS: %s, Label: %s",
            id,
            date,
            speaker,
            recorder,
            vernacular,
            listLanguages,
            location,
            gps,
            label
        );
    }

    public enum Status {
        Editing,
        Uploading,
        Uploaded
    }
}
