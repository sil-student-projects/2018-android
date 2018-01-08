package org.sil.gatherwords;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class SessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
    }

    public void save_settings_fab_pressed(View view) {
        EditText name = findViewById(R.id.session_create_name);
        EditText eliciter = findViewById(R.id.session_create_eliciter);
        EditText speaker = findViewById(R.id.session_create_speaker);
        EditText location = findViewById(R.id.session_create_location);
        EditText date = findViewById(R.id.session_create_date);

        Session session = new Session();
        session.setData("name", name.getText().toString());
        session.setData("elictier", eliciter.getText().toString());
        session.setData("speaker", speaker.getText().toString());
        session.setData("location", location.getText().toString());
        session.setData("date", date.getText().toString());

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
