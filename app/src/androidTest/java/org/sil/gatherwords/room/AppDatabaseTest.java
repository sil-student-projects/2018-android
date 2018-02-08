package org.sil.gatherwords.room;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;


@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {
	private WordDAO wordDAO;
	private SessionDAO sessionDAO;
	private AppDatabase db;
	private Context context;

	@Before
	public void createDb() {
		context = InstrumentationRegistry.getTargetContext();
		db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
		sessionDAO = db.sessionDAO();
		wordDAO = db.wordDAO();
	}

	@After
	public void closeDb() throws IOException {
		db.close();
	}

	@Test
	public void insertSessions() throws Exception {
		Session s1 = new Session();
		sessionDAO.insertSession(s1);
		List<Session> sessions = sessionDAO.getAll();
		Session s2 = sessions.get(0);
		compareSessions(s1,s2);

		Session s3 = new Session();
		s3.id = 3;
		sessionDAO.insertSession(s3);
		compareSessions(s3, sessionDAO.getAll().get(1));

		// Can duplicate primary keys be added
		try {
			Session s4 = new Session();
			s4.id = 1;
			sessionDAO.insertSession(s4);
			Assert.fail("Duplicate ID's");
		} catch (Exception e) {

		}
	}

	@Test
	public void insertWord() throws Exception {
		Session s1 = new Session();
		s1.id = 3;
		sessionDAO.insertSession(s1);

		Word w1 = new Word();
		Word w2 = new Word();
		w2.sessionID = 3;
		wordDAO.insertWords(w1, w2);
		// TODO Get foreign key working
//		try {
//			Word w3 = new Word();
//			w3.session = -5;
//			wordDAO.insertWords(w3);
//			Assert.fail("Expected to fail inserting word");
//		} catch (Exception e) {
//
//		}

		// Can duplicate primary keys be added
		try {
			Word w4 = new Word();
			w4.id = 1;
			wordDAO.insertWords(w4);
			Assert.fail("Duplicate ID's");
		} catch (Exception e) {

		}
	}

	private void compareWords(Word w1, Word w2) {
		if (w1.id == 0) {
			Assert.assertNotEquals("id", 0, w2.id);
		}
		else {
			Assert.assertEquals("id", w1.id, w2.id);
		}
		Assert.assertEquals("audio", w1.audio, w2.audio);
		Assert.assertEquals("picture", w1.picture, w2.picture);
		Assert.assertEquals("session", w1.sessionID, w2.sessionID);

	}

	private void compareSessions(Session s1, Session s2) {
		if (s1.id == 0) {
			Assert.assertNotEquals("id", s1.id, s2.id);
		}
		else {
			Assert.assertEquals("id", s1.id, s2.id);
		}
		Assert.assertEquals("date",s1.date, s2.date);
		Assert.assertEquals(s1.location, s2.location);
		Assert.assertEquals(s1.vernacular, s2.vernacular);
		Assert.assertEquals(s1.listLanguages, s2.listLanguages);
	}
}