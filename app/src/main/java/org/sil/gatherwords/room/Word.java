package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.nio.file.Path;

/**
 * Entity class for each word of a session
 */

@Entity
public class Word {
	@PrimaryKey
	public Session session;

	public Path audio;
	public Path picuture;
	public String[] meanings;
}
