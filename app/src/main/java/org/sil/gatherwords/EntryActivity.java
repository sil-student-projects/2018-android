package org.sil.gatherwords;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.WordDao;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class EntryActivity extends AppCompatActivity {
    private int sessionID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        configureItemUpdateControls();

        sessionID = getIntent().getIntExtra(SessionActivity.ARG_ID, 0);

        ViewPager pager = findViewById(R.id.viewpager);
        pager.setAdapter(new EntryPagerAdapter(getSupportFragmentManager()));
    }

    private class EntryPagerAdapter extends FragmentPagerAdapter {
        // Position to ID map.
        List<Integer> wordIDs = new ArrayList<>();

        EntryPagerAdapter(FragmentManager fm) {
            super(fm);

            // Task will trigger update after IDs are loaded.
            new LoadWordIDsTask(
                this,
                sessionID,
                AppDatabase.get(getApplicationContext()).wordDao()
            ).execute();
        }

        @Override
        public Fragment getItem(int position) {
            int wordID = 0; // Invalid ID, should never yield results.
            if (position < wordIDs.size()) {
                wordID = wordIDs.get(position);
            }
            return EntryFragment.newInstance(wordID, position, getCount());
        }

        @Override
        public int getCount() {
            // TODO: Is 0 okay?  Add special create-first-word page?
            return wordIDs.size();
        }
    }

    private static class LoadWordIDsTask extends AsyncTask<Void, Void, List<Integer>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        int sessionID;
        WordDao wDAO;

        LoadWordIDsTask(EntryPagerAdapter pagerAdapter, int sID, WordDao dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Integer> doInBackground(Void... v) {
            return wDAO.getIDsForSession(sessionID);
        }

        @Override
        protected void onPostExecute(List<Integer> wordIDs) {
            EntryPagerAdapter pagerAdapter = pagerAdapterRef.get();
            if (pagerAdapter == null || wordIDs == null) {
                return;
            }

            pagerAdapter.wordIDs = wordIDs;
            // Publish results.
            pagerAdapter.notifyDataSetChanged();
        }
    }

    // Audio recording features
    // adapted from https://developer.android.com/guide/topics/media/mediarecorder.html

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;
    private static final String LOG_TAG = "AudioRecordTest";

    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton   mPlayButton = null;
    private MediaPlayer   mPlayer = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    /**
     * This function configures the audio recording buttons and temp storage.
     */
    private void configureItemUpdateControls() {
        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += getString(R.string.audiorecordtest_3gp);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // TODO: Define these in XML instead of dynamically.
        LinearLayout ll = findViewById(R.id.footer_controls);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        // TODO: Store the audio file in the DB (or copy file to a permanent location and store a link).
    }

    class RecordButton extends AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText(R.string.STOP_RECORDING_LABEL);
                } else {
                    setText(R.string.START_RECORDING_LABEL);
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText(R.string.START_RECORDING_LABEL);
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends AppCompatButton {
        boolean mStartPlaying = true;

        OnClickListener clicker = new View.OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText(R.string.STOP_PLAYING_LABEL);
                } else {
                    setText(R.string.START_PLAYING_LABEL);
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText(R.string.START_PLAYING_LABEL);
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    // Camera/Image processing
    // based on https://developer.android.com/training/camera/photobasics.html

    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     * If there is an image capture activity out there, dispatch an intent to take a picture.
     *
     * @param view the view from which the photo request originated
     */
    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the result of a photo-taking activity.
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // TODO: Store the bitmap.
        }
    }

}
