package org.sil.gatherwords;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.util.LruCache;
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

    private int sessionID;
    ViewPager pager;

    private AudioCapture audioCapture;
    private ImageCapture imageCapture;

    private PlayButton  mPlayButton = null;
    private MediaPlayer mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        configureItemUpdateControls();

        sessionID = getIntent().getIntExtra(SessionActivity.ARG_SESSION_ID, 0);

        pager = findViewById(R.id.viewpager);
        pager.setAdapter(new EntryPagerAdapter(getSupportFragmentManager()));
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                // Changed page, stop recording if necessary.
                audioCapture.endRecord();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        audioCapture = new AudioCapture(this);
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
        // TODO: Define these in XML instead of dynamically.
        LinearLayout ll = findViewById(R.id.footer_controls);
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
        audioCapture.endRecord();

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void onRecord(View view) {
        if (!audioCapture.onRecord()) {
            Toast.makeText(this, R.string.record_failed, Toast.LENGTH_LONG).show();
        }
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
        audioCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private static class LoadWordIDsTask extends AsyncTask<Void, Void, List<Integer>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        int sessionID;
        WordDAO wDAO;

        LoadWordIDsTask(EntryPagerAdapter pagerAdapter, int sID, WordDAO dao) {
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

    private static class DeleteWordTask extends AsyncTask<Integer, Void, List<Integer>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        int sessionID;
        WordDAO wDAO;

        DeleteWordTask(EntryPagerAdapter pagerAdapter, int sID, WordDAO dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Integer> doInBackground(Integer... wordIDs) {
            wDAO.softDeleteWords(new Date(), wordIDs);
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

    private static class UndoLastDeleteWordTask extends AsyncTask<Void, Void, List<Integer>> {
        WeakReference<EntryPagerAdapter> pagerAdapterRef;
        int sessionID;
        WordDAO wDAO;

        UndoLastDeleteWordTask(EntryPagerAdapter pagerAdapter, int sID, WordDAO dao) {
            pagerAdapterRef = new WeakReference<>(pagerAdapter);
            sessionID = sID;
            wDAO = dao;
        }

        @Override
        protected List<Integer> doInBackground(Void... v) {
            int numUpdated = wDAO.undoLastDeleted(sessionID);
            if (numUpdated > 0) {
                return wDAO.getIDsForSession(sessionID);
            }

            // No need to refresh the display.
            return null;
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

    private static class PlayAudioTask extends AsyncTask<Void, Void, String> {
        private WordDAO wordDAO;
        WeakReference<EntryActivity> activityRef;
        int wordID;

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
        List<Integer> wordIDs = new ArrayList<>();
        LruCache<Integer, WeakReference<Fragment>> pageCache;

        EntryPagerAdapter(FragmentManager fm) {
            super(fm);

            pageCache = new LruCache<>(5);

            // Task will trigger update after IDs are loaded.
            new LoadWordIDsTask(
                this,
                sessionID,
                AppDatabase.get(getApplicationContext()).wordDAO()
            ).execute();
        }

        @Override
        public void notifyDataSetChanged() {
            pageCache.evictAll();
            super.notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            WeakReference<Fragment> fragmentRef = pageCache.get(position);
            if (fragmentRef != null && fragmentRef.get() != null) {
                return fragmentRef.get();
            }

            int wordID = 0; // Invalid ID, should never yield results.
            if (position < wordIDs.size()) {
                wordID = wordIDs.get(position);
            }
            Fragment fragment = EntryFragment.newInstance(wordID, position, getCount());
            pageCache.put(position, new WeakReference<>(fragment));
            return fragment;
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
        int sessionID;
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
            int wordID = (int) wDAO.insertWord(newWord);

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
