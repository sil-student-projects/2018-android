package org.sil.gatherwords;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Uploader {
    private static final String TAG = Uploader.class.getSimpleName();

    private static final int SELECT_ACCOUNT_REQUEST = 202;
    private static final int REQUEST_GET_ACCOUNTS_PERMISSION = 203;

    private Activity m_activity;

    // Not synchronized; only access from main thread.
    private List<Integer> m_queuedUploads;

    public Uploader(Activity activity) {
        m_activity = activity;
        m_queuedUploads = new ArrayList<>();
    }

    public void upload(int sessionID) {
        new StartUploadTask(this, sessionID, true).execute();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_GET_ACCOUNTS_PERMISSION) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                getAuth();
            }
            else {
                Log.d(TAG, "Lacks GET_ACCOUNTS permission.");
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != SELECT_ACCOUNT_REQUEST) {
            return;
        }

        if (resultCode != RESULT_OK) {
            Log.d(TAG, "Select account failed.");
            return;
        }

        String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        AccountManager manager = AccountManager.get(m_activity);
        Account account = null;
        for (Account ac : manager.getAccountsByType("com.google")) {
            if (ac.name.equals(accountName)) {
                account = ac;
                break;
            }
        }

        if (account == null) {
            Log.d(TAG, "Failed to access 'Account' object");
            return;
        }

        final Uploader that = this;
        manager.getAuthToken(
            account,
            "oauth2:email openid",
            null,
            m_activity,
            new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> result) {
                    try {
                        Bundle bundle = result.getResult();
                        String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                        // Evict token from cache so we revalidate next time.
                        AccountManager.get(m_activity)
                            .invalidateAuthToken(
                                bundle.getString(AccountManager.KEY_ACCOUNT_TYPE),
                                token
                            );

                        new LoginTask(that, token).execute();
                    } catch (Exception e) {
                        Log.d(TAG, "getAuthToken() failure", e);
                    }
                }
            },
            null
        );

    }

    private void login() {
        if (ContextCompat.checkSelfPermission(m_activity, Manifest.permission.GET_ACCOUNTS) == PERMISSION_GRANTED) {
            getAuth();
        }
        else {
            ActivityCompat.requestPermissions(
                m_activity,
                new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_GET_ACCOUNTS_PERMISSION
            );
        }
    }

    private void getAuth() {
        Intent pickerIntent = AccountPicker.newChooseAccountIntent(
            null, null, new String[]{"com.google"},
            false, null, null, null, null
        );
        m_activity.startActivityForResult(pickerIntent, SELECT_ACCOUNT_REQUEST);
    }

    private Context getContext() {
        return m_activity;
    }

    private static final class LoginTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<Uploader> uploaderRef;
        private String accessToken;

        LoginTask(Uploader uploader, String token) {
            uploaderRef = new WeakReference<>(uploader);
            accessToken = token;

            Toast.makeText(
                uploader.getContext(),
                R.string.logging_in,
                Toast.LENGTH_SHORT
            ).show();
        }

        // This function is already asynchronous, so using commit() instead
        // of apply() is okay.
        @SuppressLint("ApplySharedPref")
        @NonNull
        @Override
        protected Boolean doInBackground(Void... voids) {
            Uploader uploader = uploaderRef.get();
            if (uploader == null) {
                return false;
            }

            Context context = uploader.getContext();

            XForgeAPI api = new XForgeAPI(context);
            String jwt = api.login(accessToken);

            SharedPreferences sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            );

            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(context.getString(R.string.jwt_pref_key), jwt);
            editor.commit();

            return jwt != null;
        }

        @Override
        protected void onPostExecute(@NonNull Boolean success) {
            Uploader uploader = uploaderRef.get();
            if (uploader == null) {
                return;
            }

            if (success) {
                for (int sessionID : uploader.m_queuedUploads) {
                    new StartUploadTask(uploader, sessionID, false).execute();
                }
                uploader.m_queuedUploads.clear();
            }
            else {
                Toast.makeText(
                    uploader.getContext(),
                    R.string.login_failed,
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private static final class StartUploadTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<Uploader> uploaderRef;
        private int sessionID;
        private boolean attemptLogin;

        StartUploadTask(Uploader uploader, int sID, boolean doLogin) {
            uploaderRef = new WeakReference<>(uploader);
            sessionID = sID;
            attemptLogin = doLogin;
        }

        @Override
        @NonNull
        protected Boolean doInBackground(Void... voids) {
            Uploader uploader = uploaderRef.get();
            if (uploader == null) {
                return false;
            }

            Context context = uploader.getContext();

            XForgeAPI api = new XForgeAPI(context);

            SharedPreferences sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            );
            String jwt = sharedPrefs.getString(
                context.getString(R.string.jwt_pref_key),
                null
            );

            return jwt != null && api.isJwtAuthorized(jwt);
        }

        @Override
        protected void onPostExecute(@NonNull Boolean success) {
            Uploader uploader = uploaderRef.get();

            if (uploader == null) {
                return;
            }

            if (!success) {
                if (attemptLogin) {
                    uploader.m_queuedUploads.add(sessionID);
                    uploader.login();
                }
                return;
            }

            Context context = uploader.getContext();

            Log.d(TAG, "Launching UploadService for session " + sessionID);
            Intent uploadIntent = new Intent(context, UploadService.class);
            uploadIntent.putExtra(SessionActivity.ARG_SESSION_ID, sessionID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(uploadIntent);
            }
            else {
                context.startService(uploadIntent);
            }
        }
    }
}
