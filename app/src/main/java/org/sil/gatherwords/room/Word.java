package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.nio.file.Path;

/**
 * Entity class for each word of a session
 */

@Entity
public class Word {
	@PrimaryKey
	public int id;

	@ForeignKey(entity = Session.class,
	parentColumns = "id",
	childColumns = "session")
	public int session;

	public String[] meanings;
	public Path audio;
	public Path picuture;
}
