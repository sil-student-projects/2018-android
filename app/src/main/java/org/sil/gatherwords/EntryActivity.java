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
import org.sil.gatherwords.room.Converters;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class EntryActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private long sessionID;
    private ViewPager pager;

    // Audio recording features
    // adapted from https://developer.android.com/guide/topics/media/mediarecorder.html
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;
    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;
    private PlayButton   mPlayButton = null;
    private MediaPlayer   mPlayer = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private File audioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        configureItemUpdateControls();

        sessionID = getIntent().getLongExtra(SessionActivity.ARG_ID, 0);

        pager = findViewById(R.id.viewpager);
        pager.setAdapter(new EntryPagerAdapter(getSupportFragmentManager()));
    }

    /**
     * This function configures the audio recording buttons and temp storage.
     */
    private void configureItemUpdateControls() {
        // Record to the external cache directory for visibility
        File cache = getExternalCacheDir();
        if (cache == null) {
            cache = getCacheDir();
            if (cache == null) {
                Log.e(this.getClass().getSimpleName(), "No cache directories available");

                // Just leave, and don't show the record button
                return;
            }
        }
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

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
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
        new SaveRecordingTask(this).execute(mFileName);
    }

    private void onPlay(boolean start) {
        if (start) {
            new ReadRecordingTask(this).execute();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFile.getAbsolutePath());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;

        // Just checking
        if (audioFile.exists()) {
            if (!audioFile.delete()) {
                Log.e(this.getClass().getSimpleName(), "Failed to delete the temporary playback file");
            }
        }
    }

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

    public Fragment getCurrentFragment() {
        int currentItem = pager.getCurrentItem();
        EntryPagerAdapter adapter = (EntryPagerAdapter) pager.getAdapter();
        return adapter.getItem(currentItem);
    }

    public String getFileName() {
        return mFileName;
    }

    private static class LoadWordIDsTask extends AsyncTask<Void, Void, List<Long>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        long sessionID;
        WordDao wDAO;

        LoadWordIDsTask(EntryPagerAdapter pagerAdapter, long sID, WordDao dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Long> doInBackground(Void... v) {
            return wDAO.getIDsForSession(sessionID);
        }

        @Override
        protected void onPostExecute(List<Long> wordIDs) {
            EntryPagerAdapter pagerAdapter = pagerAdapterRef.get();
            if (pagerAdapter == null || wordIDs == null) {
                return;
            }

            pagerAdapter.wordIDs = wordIDs;
            // Publish results.
            pagerAdapter.notifyDataSetChanged();
        }
    }


    // Camera/Image processing
    // based on https://developer.android.com/training/camera/photobasics.html

    private class EntryPagerAdapter extends FragmentPagerAdapter {
        // Position to ID map.
        List<Long> wordIDs = new ArrayList<>();

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
            long wordID = 0; // Invalid ID, should never yield results.
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

    private static class SaveRecordingTask extends AsyncTask<String, Void, Void>{
        private AppDatabase db;
        private WordDao wd;
        private WeakReference<EntryActivity> entryActivityRef;
        private long wordId;

        SaveRecordingTask(EntryActivity activity) {
            entryActivityRef = new WeakReference<>(activity);
            db = AppDatabase.get(activity.getApplicationContext());
            wd = db.wordDao();
            EntryFragment fragment = (EntryFragment) activity.getCurrentFragment();
            wordId = fragment.getWordId();
        }

        @Override
        protected Void doInBackground(String... strings) {
            EntryActivity activity = entryActivityRef.get();
            if (activity == null) {
                return null;
            }
            String filename = activity.getFileName();
            if (filename == null) {
                Log.e(this.getClass().getSimpleName(), "There is no file name");
                return null;
            }
            byte[] audio = null;
            try {
                InputStream is = new FileInputStream(filename);
                audio = new byte[is.available()];
                int remainder = is.read(audio);
                if (remainder != -1) {
                    Log.e(SaveRecordingTask.class.getSimpleName(), "Not all the file was read");
                    // Continue or exit?
                }
            } catch (IOException e) {
                Log.e(SaveRecordingTask.class.getSimpleName(), "IOException while reading the file", e);
            }
            // Get the word
            Word word = wd.get(wordId);
            word.audio = audio;

            wd.updateWords(word);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            EntryActivity activity = entryActivityRef.get();
            if (activity == null) {
                return;
            }
            String filename = activity.getFileName();
            if (filename == null) {
                Log.e(this.getClass().getSimpleName(), "There is no file name");
                return;
            }
            File file = new File(filename);
            if (file.exists()) {
                if (!file.delete()) {
                    Log.e(this.getClass().getSimpleName(), "Failed to delete the temporary recording file");
                }
            }
        }
    }

    private static class ReadRecordingTask extends AsyncTask<Void, Void, Word> {
        private WeakReference<EntryActivity> entryActivityRef;
        private AppDatabase db;
        private WordDao wordDao;
        private long wordId;

        ReadRecordingTask(EntryActivity entryActivity){
            entryActivityRef = new WeakReference<>(entryActivity);
            db = AppDatabase.get(entryActivity);
            wordDao = db.wordDao();

            EntryFragment fragment = (EntryFragment) entryActivity.getCurrentFragment();
            wordId = fragment.getWordId();
        }

        @Override
        protected Word doInBackground(Void... voids) {
            return wordDao.get(wordId);
        }

        @Override
        protected void onPostExecute(Word word) {
            EntryActivity activity = entryActivityRef.get();
            if (activity == null || word == null) {
                return;
            }

            // Create the path string for the temporary file, preferring external
            File cache = activity.getExternalCacheDir();
            if (cache == null) {
                cache = activity.getCacheDir();
                if (cache == null) {
                    Log.e(ReadRecordingTask.class.getSimpleName(), "No locations available for caching");
                    return;
                }
            }

            String playbackFile = cache.getAbsolutePath();
            playbackFile += activity.getString(R.string.audio_playback_file);

            // Create the File from the byte[] and file path and store it
            activity.audioFile = Converters.byteArrayToFile(word.audio, playbackFile);

            // Tell the media player to start
            activity.startPlaying();
        }
    }

}
