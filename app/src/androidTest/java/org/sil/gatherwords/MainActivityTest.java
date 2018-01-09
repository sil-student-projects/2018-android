package org.sil.gatherwords;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.SessionDao;
import org.sil.gatherwords.room.WordDao;

/**
 * Test the Main Activity
 */
public class MainActivityTest{
	private Context context;
	private AppDatabase db;
	private WordDao wordDao;
	private SessionDao sessionDao;

	@Before
	public void setUp() throws Exception {
		Looper.prepare();
		context = InstrumentationRegistry.getTargetContext();
		InstrumentationRegistry.getInstrumentation().callActivityOnCreate(new MainActivity(), null);
//		appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
//		sessionDao = appDatabase.sessionDao();
//		wordDao = appDatabase.wordDao();
	}

	@After
	public void tearDown() throws Exception {
	}

	// Test that the database is correctly written to
	@Test
	public void dbWrite() throws Exception {
		System.out.println(true);
	}

}