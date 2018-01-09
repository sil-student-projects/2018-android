package org.sil.gatherwords.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.sql.Blob;

/**
 * Entity class for each word of a session
 */

@Entity
public class Word {
	@PrimaryKey (autoGenerate = true)
	public int id;

	@ForeignKey(entity = Session.class,
	parentColumns = "id",
	childColumns = "session",
	onDelete = ForeignKey.CASCADE,
	onUpdate = ForeignKey.CASCADE)
	public int sessionId;

	public String meanings; // JSON, TODO: possibly split to meanings table

	@Ignore
	public Blob audio; // NOTE: LF does NOT support multiple audio files

	@Ignore
	public Blob picture; // NOTE: LF DOES support multiple picture files
}