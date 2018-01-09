package org.sil.gatherwords;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.sil.gatherwords.room.AppDatabase;

public class SessionActivity extends AppCompatActivity {
	AppDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session);
	}

	@Override
	protected void onDestroy() {
		db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, getString(R.string.database_name_testing)).build();
		DatabaseAccess da = new DatabaseAccess(db);

		da.execute("insert");
		super.onDestroy();
	}
}


