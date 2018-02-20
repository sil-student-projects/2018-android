package org.sil.gatherwords;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Meaning;
import org.sil.gatherwords.room.MeaningDAO;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class EntryActivity extends AppCompatActivity {
    private static final String TAG = EntryActivity.class.getSimpleName();

    private long sessionID;
    ViewPager pager;

    private ImageCapture imageCapture;

    // Audio recording features
    // adapted from https://developer.android.com/guide/topics/media/mediarecorder.html
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private File mAudioFile = null;
    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;
    private PlayButton   mPlayButton = null;
    private MediaPlayer   mPlayer = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        configureItemUpdateControls();

        sessionID = getIntent().getLongExtra(SessionActivity.ARG_SESSION_ID, 0);

        pager = findViewById(R.id.viewpager);
        pager.setAdapter(new EntryPagerAdapter(getSupportFragmentManager()));

        imageCapture = new ImageCapture(this);

        findViewById(R.id.new_word_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddNewWordToDB(EntryActivity.this).execute();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.entry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.entry_delete:
                EntryFragment entryFragment = (EntryFragment)getCurrentFragment();

                new DeleteWordTask(
                    (EntryPagerAdapter)pager.getAdapter(),
                    sessionID,
                    AppDatabase.get(this).wordDAO()
                ).execute(entryFragment.getWordID());
                return true;

            case R.id.undo_entry_delete:
                new UndoLastDeleteWordTask(
                    (EntryPagerAdapter)pager.getAdapter(),
                    sessionID,
                    AppDatabase.get(this).wordDAO()
                ).execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function configures the audio recording buttons and temp storage.
     */
    private void configureItemUpdateControls() {
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
        mAudioFile = Util.getNewDataFile(this, ".3gp");
        if (mAudioFile == null) {
            Toast.makeText(this, R.string.record_failed, Toast.LENGTH_LONG).show();
            return;
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mAudioFile.getAbsolutePath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        new SaveAudioTask(this).execute(mAudioFile);
        mAudioFile = null; // File control is taken by SaveAudioTask.
    }

    private void onPlay(boolean start) {
        if (start) {
            new PlayAudioTask(this).execute();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying(String filename) {
        mPlayer = new MediaPlayer();
        try {
            File audioFile = Util.getDataFile(this, filename);
            mPlayer.setDataSource(audioFile.getAbsolutePath());
            mPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            );

            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    /**
     * If there is an image capture activity out there, dispatch an intent to take a picture.
     *
     * @param view the view from which the photo request originated
     */
    public void dispatchImageCaptureIntent(View view) {
        imageCapture.dispatchImageCaptureIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        imageCapture.onActivityResult(requestCode, resultCode);
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

    /**
     * Get the current fragment
     * @return The currently shown fragment
     */
    public Fragment getCurrentFragment() {
        int currentItem = pager.getCurrentItem();
        EntryPagerAdapter adapter = (EntryPagerAdapter) pager.getAdapter();
        return adapter.getItem(currentItem);
    }

    private static class LoadWordIDsTask extends AsyncTask<Void, Void, List<Long>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        long sessionID;
        WordDAO wDAO;

        LoadWordIDsTask(EntryPagerAdapter pagerAdapter, long sID, WordDAO dao) {
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

    private static class DeleteWordTask extends AsyncTask<Long, Void, List<Long>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        long sessionID;
        WordDAO wDAO;

        DeleteWordTask(EntryPagerAdapter pagerAdapter, long sID, WordDAO dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Long> doInBackground(Long... wordIDs) {
            wDAO.softDeleteWords(new Date(), wordIDs);
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

    private static class UndoLastDeleteWordTask extends AsyncTask<Void, Void, List<Long>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        long sessionID;
        WordDAO wDAO;

        UndoLastDeleteWordTask(EntryPagerAdapter pagerAdapter, long sID, WordDAO dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Long> doInBackground(Void... v) {
            long numUpdated = wDAO.undoLastDeleted(sessionID);
            if (numUpdated > 0) {
                return wDAO.getIDsForSession(sessionID);
            }

            // No need to refresh the display.
            return null;
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

    private static class SaveAudioTask extends AsyncTask<File, Void, Void> {
        private AppDatabase db;
        private long wordID;

        SaveAudioTask(EntryActivity activity) {
            db = AppDatabase.get(activity);
            EntryFragment fragment = (EntryFragment)activity.getCurrentFragment();
            wordID = fragment.getWordID();
        }

        @Override
        protected Void doInBackground(File... files) {
            if (files == null || files.length != 1) {
                Log.e(TAG, "Expected exactly 1 audio file");
                return null;
            }

            File audioFile = files[0];
            boolean success = false;

            db.beginTransaction();
            try {
                WordDAO wordDAO = db.wordDAO();

                Word currentWord = wordDAO.get(wordID);
                if (currentWord != null) {
                    File oldAudio = null;
                    if (currentWord.audio != null) {
                        oldAudio = new File(audioFile.getParent() + '/' + currentWord.audio);
                    }

                    // Save it.
                    currentWord.audio = audioFile.getName();
                    wordDAO.updateWords(currentWord);

                    db.setTransactionSuccessful();
                    success = true;

                    if (oldAudio != null) {
                        // Remove since we no longer hold reference to it.
                        if (!oldAudio.delete()) {
                            Log.e(TAG, "Failed to delete overridden audio: " + oldAudio.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while updating audio in the Word", e);
            } finally {
                db.endTransaction();
            }

            if (!success) {
                // Remove the file since we no longer hold reference to it.
                if (!audioFile.delete()) {
                    Log.e(TAG, "Failed to delete unsaved audio: " + audioFile.getAbsolutePath());
                }
            }

            return null;
        }
    }

    private static class PlayAudioTask extends AsyncTask<Void, Void, String> {
        private WordDAO wordDAO;
        WeakReference<EntryActivity> activityRef;
        long wordID;

        PlayAudioTask(EntryActivity activity) {
            wordDAO = AppDatabase.get(activity).wordDAO();
            activityRef = new WeakReference<>(activity);

            EntryFragment fragment = (EntryFragment)activity.getCurrentFragment();
            wordID = fragment.getWordID();
        }

        @Override
        protected String doInBackground(Void... v) {
            Word word = wordDAO.get(wordID);
            if (word == null) {
                return null;
            }
            return word.audio;
        }

        @Override
        protected void onPostExecute(String filename) {
            EntryActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            if (filename == null) {
                Toast.makeText(activity, R.string.no_audio, Toast.LENGTH_SHORT).show();
            }
            else {
                activity.startPlaying(filename);
            }
        }
    }

    class EntryPagerAdapter extends FragmentStatePagerAdapter {
        // Position to ID map.
        List<Long> wordIDs = new ArrayList<>();

        EntryPagerAdapter(FragmentManager fm) {
            super(fm);

            // Task will trigger update after IDs are loaded.
            new LoadWordIDsTask(
                this,
                sessionID,
                AppDatabase.get(getApplicationContext()).wordDAO()
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

        // Forces recreate of pages in order to update total number of pages
        @Override
        public int getItemPosition(Object obj) {
            return POSITION_NONE;
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

    private static class AddNewWordToDB extends AsyncTask<Void, Void, Void> {
        WeakReference<EntryActivity> entryActivityRef;
        WordDAO wDAO;
        MeaningDAO mDAO;
        Long sessionID;
        Set<String> sharedPrefs;
        String[] languages;

        AddNewWordToDB(EntryActivity entryActivity) {
            entryActivityRef = new WeakReference<>(entryActivity);
            wDAO = AppDatabase.get(entryActivity).wordDAO();
            mDAO = AppDatabase.get(entryActivity).meaningDAO();
            sessionID = entryActivity.sessionID;
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(entryActivity).
                    getStringSet(entryActivity.getString(R.string.language_options_key), null);
            languages = entryActivity.getResources().getStringArray(R.array.language_options_entries);
        }

        @Override
        protected Void doInBackground(Void... v) {
            // Insert new Word
            Word newWord = new Word();
            newWord.sessionID = sessionID;
            newWord.updatedAt = new Date();
            long wordID = wDAO.insertWord(newWord);

            // Insert blank Meanings for all preferences currently selected
            int idx = 0;
            Meaning[] meaningList = new Meaning[sharedPrefs.size()];
            for (String pref : sharedPrefs) {
                 meaningList[idx] = new Meaning(wordID, languages[Integer.parseInt(pref) - 1], "");
                 idx++;
            }
            mDAO.insertMeanings(meaningList);

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            final EntryActivity entryActivity = entryActivityRef.get();
            if (entryActivity == null) {
                return;
            }
            ViewPager pager = entryActivity.findViewById(R.id.viewpager);
            new LoadWordIDsTask((EntryPagerAdapter)pager.getAdapter(), sessionID, wDAO).execute();
        }
    }
}
