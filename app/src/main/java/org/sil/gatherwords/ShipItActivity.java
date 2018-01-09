package org.sil.gatherwords;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ShipItActivity extends AppCompatActivity {

    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ship_it);
        // Music royalty free courtesy of bensound.com
        mp = MediaPlayer.create(this, R.raw.music);
        mp.setLooping(true);
        mp.setVolume(100, 100);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            try {
                mp.stop();
                mp.release();
            } finally {
                mp = null;
            }
        }
    }

    public void onPause() {
        super.onPause();
        mp.pause();
    }

    public void onResume() {
        super.onResume();
        mp.start();
    }
}
