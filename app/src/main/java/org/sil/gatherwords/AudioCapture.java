package org.sil.gatherwords;

import android.Manifest;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class AudioCapture {
    private static final String TAG = AudioCapture.class.getSimpleName();

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private Button m_recordBtn;
    private EntryActivity m_activity;
    private File m_audioFile;
    private MediaRecorder m_recorder;
    private boolean m_canRecord;
    private int m_wordID;

    AudioCapture(EntryActivity activity) {
        m_activity = activity;

        m_canRecord = ActivityCompat.checkSelfPermission(
            m_activity,
            Manifest.permission.RECORD_AUDIO
        ) == PERMISSION_GRANTED;

        if (!m_canRecord) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(
                m_activity,
                permissions,
                REQUEST_RECORD_AUDIO_PERMISSION
            );
        }

        m_recordBtn = m_activity.findViewById(R.id.record_button);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            m_canRecord = grantResults[0] == PERMISSION_GRANTED;
        }
    }

    public boolean onRecord() {
        if (!m_canRecord) {
            return false;
        }

        if (m_recorder != null) {
            // We are currently already.
            return endRecord();
        }

        // Start a new recording.

        if (m_audioFile != null) {
            if (!m_audioFile.delete()) {
                Log.e(TAG, "Failed to delete unconsumed audio: " + m_audioFile);
            }
        }

        m_audioFile = Util.getNewDataFile(m_activity, ".3gp");
        if (m_audioFile == null) {
            return false;
        }

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        m_recorder.setOutputFile(m_audioFile.getAbsolutePath());

        try {
            m_recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed", e);
            m_recorder = null;
            return false;
        }

        // Store which word we are recording for (this could change before
        // we are told to save).
        EntryFragment fragment = (EntryFragment)m_activity.getCurrentFragment();
        m_wordID = fragment.getWordID();

        m_recordBtn.setText(R.string.stop);

        m_recorder.start();
        return true;
    }

    public boolean endRecord() {
        if (m_recorder == null) {
            return false;
        }

        m_recordBtn.setText(R.string.record);

        m_recorder.stop();
        m_recorder.reset();
        m_recorder.release();
        m_recorder = null;

        new SaveAudioTask(m_activity, m_wordID).execute(m_audioFile);
        m_audioFile = null; // File control is taken by SaveAudioTask.

        return true;
    }

    private static class SaveAudioTask extends AsyncTask<File, Void, Boolean> {
        private AppDatabase db;
        private WeakReference<EntryFragment.EntryFieldAdapter> adapterRef;
        private int wordID;

        SaveAudioTask(EntryActivity activity, int wID) {
            db = AppDatabase.get(activity);
            wordID = wID;

            View wordView = activity.getCurrentFragment().getView();
            if (wordView != null) {
                ListView entryFields = wordView.findViewById(R.id.entry_fields);
                adapterRef = new WeakReference<>(
                    (EntryFragment.EntryFieldAdapter)entryFields.getAdapter()
                );
            }
        }

        @Override
        protected Boolean doInBackground(File... files) {
            if (files == null || files.length != 1) {
                Log.e(TAG, "Expected exactly 1 audio file");
                return false;
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

            return success;
        }

        @Override
        protected void onPostExecute(@NonNull Boolean success) {
            if (success && adapterRef != null) {
                EntryFragment.EntryFieldAdapter adapter = adapterRef.get();
                if (adapter != null && adapter.word.audio == null) {
                    // *Technically* not a valid path, but we are just
                    // triggering an update.
                    adapter.word.audio = "";
                    // Update, we now have recorded audio.
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
}
