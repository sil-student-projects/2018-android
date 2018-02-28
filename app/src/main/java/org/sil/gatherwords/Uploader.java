package org.sil.gatherwords;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.AccountPicker;

import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Uploader {
    private static final String TAG = Uploader.class.getSimpleName();

    private static final int SELECT_ACCOUNT_REQUEST = 202;
    private static final int REQUEST_GET_ACCOUNTS_PERMISSION = 203;

    private Activity m_activity;
    private int m_sessionID;

    public Uploader(Activity activity) {
        m_activity = activity;
    }

    protected void upload(int sessionID) {
        m_sessionID = sessionID;

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
        if (requestCode == SELECT_ACCOUNT_REQUEST) {
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "Select account failed");
                return;
            }

            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Log.d(TAG, "Account: " + accountName);

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

            manager.getAuthToken(
                account,
                "oauth2:openid",
                null,
                m_activity,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> result) {
                        Log.d(TAG, "getAuthToken()");
                        try {
                            Bundle bundle = result.getResult();
                            String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            Log.d(TAG, "Token: " + token);

                            /*
                            // Evict token from cache so we revalidate next time.
                            AccountManager.get(getApplicationContext())
                                .invalidateAuthToken(
                                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE),
                                    token
                                );
                            */

                            // TODO: Authenticate with LF.

                            Log.d(TAG, "Launching UploadService for session " + m_sessionID);
                            Intent uploadIntent = new Intent(m_activity, UploadService.class);
                            uploadIntent.putExtra(SessionActivity.ARG_SESSION_ID, m_sessionID);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                m_activity.startForegroundService(uploadIntent);
                            }
                            else {
                                m_activity.startService(uploadIntent);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "getAuthToken() failure", e);
                        }
                    }
                },
                null
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
}
