package org.sil.gatherwords;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;

/**
 * Joke Activity class.  Remove at some point further in development before shipping final product.
 */
public class ShipItActivity extends AppCompatActivity {

    MediaPlayer mp;
    AppCompatImageView iv;
    Handler handler;

    private Runnable runnable = new Runnable() {
        boolean rev = false;
        @Override
        public void run() {
            if ( rev ) {
                iv.setImageResource(R.drawable.cambell);
                rev = false;
            } else {
                iv.setImageResource(R.drawable.cambell_rev);
                rev = true;
            }
            handler.postDelayed(runnable, 500);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ship_it);
        // Music royalty free courtesy of bensound.com
        mp = MediaPlayer.create(this, R.raw.music);
        mp.setLooping(true);
        mp.setVolume(100, 100);

        iv = findViewById(R.id.cambell);
        handler = new Handler();

        handler.post(runnable);
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
        handler.removeCallbacks(runnable);
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
