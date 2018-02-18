package org.sil.gatherwords;

import android.content.Intent;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.io.File;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;

public class ImageCapture {
    private static final String TAG = ImageCapture.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private EntryActivity m_activity;
    private File m_imageFile;

    ImageCapture(EntryActivity activity) {
        m_activity = activity;
    }

    public void dispatchImageCaptureIntent() {
        if (m_imageFile != null) {
            if (!m_imageFile.delete()) {
                Log.e(TAG, "Failed to delete unconsumed image: " + m_imageFile);
            }
        }

        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Since the camera is a separate app, we cannot write directly
        // to internal storage. So, put it in the cache and move it later.
        m_imageFile = Util.getNewCacheFile(m_activity, ".jpg");
        imageCaptureIntent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            FileProvider.getUriForFile(
                m_activity,
                BuildConfig.APPLICATION_ID + ".provider",
                m_imageFile
            )
        );

        if (imageCaptureIntent.resolveActivity(m_activity.getPackageManager()) != null) {
            m_activity.startActivityForResult(
                imageCaptureIntent,
                REQUEST_IMAGE_CAPTURE
            );
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != REQUEST_IMAGE_CAPTURE) {
            // Not our job.
            return;
        }

        if (resultCode == RESULT_OK) {
            File internalFile = Util.getNewDataFile(m_activity, ".jpg");
            if (internalFile != null) {
                new StoreImageTask(m_activity).execute(m_imageFile, internalFile);
            }
            else if (!m_imageFile.delete()) {
                // Failed to pass along; clean up.
                Log.e(TAG, "Failed to delete cached image: " + m_imageFile);
            }

            m_imageFile = null;
        }
    }

    private static class StoreImageTask extends AsyncTask<File, Void, Boolean> {
        private AppDatabase db;
        private WordDAO wordDAO;
        private long wordID;
        WeakReference<EntryActivity.EntryPagerAdapter> pagerAdapterRef;

        StoreImageTask(EntryActivity activity) {
            db = AppDatabase.get(activity);
            wordDAO = db.wordDAO();
            EntryFragment fragment = (EntryFragment)activity.getCurrentFragment();
            wordID = fragment.getWordID();
            pagerAdapterRef = new WeakReference<>(
                (EntryActivity.EntryPagerAdapter)activity.pager.getAdapter()
            );
        }

        @Override
        protected Boolean doInBackground(File... files) {
            if (files == null || files.length != 2) {
                Log.e(TAG, "Expected exactly 2 image file");
                return false;
            }

            File imageSourceFile = files[0];
            File imageDestFile = files[1];

            if (!Util.moveFile(imageSourceFile, imageDestFile)) {
                // Save failed, clean up.
                Log.e(TAG, "Failed to move to internal storage: " + imageSourceFile);
                if (!imageSourceFile.delete()) {
                    Log.e(TAG, "Failed to delete cached image: " + imageSourceFile);
                }
                if (imageDestFile != null && !imageDestFile.delete()) {
                    Log.e(TAG, "Failed to delete placeholder: " + imageDestFile);
                }

                return false;
            }

            boolean success = false;
            db.beginTransaction();
            try {
                Word currentWord = wordDAO.get(wordID);
                if (currentWord != null) {
                    File oldImage = null;
                    if (currentWord.picture != null) {
                        oldImage = new File(
                            imageDestFile.getParent() + '/' + currentWord.picture
                        );
                    }

                    // Save it.
                    currentWord.picture = imageDestFile.getName();
                    wordDAO.updateWords(currentWord);

                    db.setTransactionSuccessful();
                    success = true;

                    if (oldImage != null) {
                        // Remove since we no longer hold reference to it.
                        if (!oldImage.delete()) {
                            Log.e(TAG, "Failed to delete overridden image: " + oldImage);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while updating the Word", e);
            } finally {
                db.endTransaction();
            }

            if (!success) {
                // Remove the file since we no longer hold reference to it.
                if (!imageDestFile.delete()) {
                    Log.e(TAG, "Failed to delete unsaved image: " + imageDestFile);
                }
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            EntryActivity.EntryPagerAdapter pagerAdapter = pagerAdapterRef.get();
            if (pagerAdapter == null || !success) {
                return;
            }

            pagerAdapter.notifyDataSetChanged();
        }
    }
}
