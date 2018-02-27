package org.sil.gatherwords;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.Context.MODE_PRIVATE;

public class Util {
    private static String TAG = Util.class.getSimpleName();

    @Nullable
    public static File getNewCacheFile(Context context, String suffix) {
        File baseDir = context.getExternalCacheDir();
        if (baseDir == null) {
            baseDir = context.getExternalFilesDir("external_cache");
        }
        try {
            return File.createTempFile("gatherwords", suffix, baseDir);
        } catch (IOException e) {
            Log.e(TAG, "Unable to create temp cache file", e);
            return null;
        }
    }

    @Nullable
    public static File getNewDataFile(Context context, String suffix) {
        File baseDir = context.getDir("data", MODE_PRIVATE);
        try {
            return File.createTempFile("gatherwords", suffix, baseDir);
        } catch (IOException e) {
            Log.e(TAG, "Unable to create temp data file", e);
            return null;
        }
    }

    public static File getDataFile(Context context, String filename) {
        File baseDir = context.getDir("data", MODE_PRIVATE);
        return new File(baseDir.getAbsolutePath() + '/' + filename);
    }

    /**
     * Assumes `from` and `to` exist.
     * Returns true on success; if successful, `from` is deleted.
     *
     * Since this does a lot of I/O, please use in an AsyncTask.
     */
    public static boolean moveFile(File from, File to) {
        // File.renameTo() does not work across mount points.
        // Files.copy() requires a higher api level.
        boolean success = false;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(from);
            os = new FileOutputStream(to);
            byte[] buffer = new byte[1024];

            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            success = from.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error while moving file", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }

        return success;
    }

    public static void sendGetTokenRequest(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccounts();
        Bundle options = new Bundle();
//        accountManager.getAuthToken()
    }
}
