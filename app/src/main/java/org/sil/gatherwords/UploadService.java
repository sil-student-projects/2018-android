package org.sil.gatherwords;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Session;
import org.sil.gatherwords.room.SessionDAO;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.util.List;

public class UploadService extends Service implements Handler.Callback {
    private static final String TAG = UploadService.class.getSimpleName();
    // TODO: Define this channel.
    private static final String NOTIFICATION_CHANNEL = "gather_words";
    private static final int NOTIFICATION_ID = 1;

    Handler m_workerHandler;
    NotificationManager m_notificationManager;
    NotificationCompat.Builder m_notification;

    @Override
    public void onCreate()
    {
        HandlerThread worker = new HandlerThread(
            "UploadServiceWorker",
            Process.THREAD_PRIORITY_BACKGROUND
        );
        worker.start();

        m_workerHandler = new Handler(worker.getLooper(), this);

        m_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m_notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher_custom)
            .setContentTitle(getString(R.string.session_upload));

        Intent notificationIntent = new Intent(this, UploadService.class);
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher_custom)
            .setContentTitle(getString(R.string.session_upload))
            .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
            .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Message msg = m_workerHandler.obtainMessage();
        msg.arg1 = startID;
        msg.arg2 = intent.getIntExtra(SessionActivity.ARG_SESSION_ID, 0);

        m_workerHandler.sendMessage(msg);

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("This service cannot be bound");
    }

    @Override
    public boolean handleMessage(Message msg) {
        int uploadServiceStartID = msg.arg1;
        int sessionID = msg.arg2;

        AppDatabase db = AppDatabase.get(this);
        SessionDAO sDAO = db.sessionDAO();
        Session session = sDAO.get(sessionID);

        if (session == null) {
            // Invalid data sent? ... Should we warn the user?
            Log.e(TAG, "Failed to find session " + sessionID + ", skipping upload");
        }
        else if (session.state == Session.Status.Uploaded) {
            Log.w(TAG, "Skipping already uploaded session " + session.label);
        }
        else {
            if (session.state == Session.Status.Uploading) {
                Log.d(TAG, "Resuming upload for " + session.label);
            }
            uploadSession(db, session);
        }

        // TODO: This notification is immediately hidden.
        m_notification.setContentText(getString(R.string.upload_complete))
            .setProgress(0, 0, false);
        m_notificationManager.notify(NOTIFICATION_ID,  m_notification.build());

        // Will only kill if no more requests have been sent to this service.
        stopSelf(uploadServiceStartID);

        // Message processed.
        return true;
    }

    private void uploadSession(AppDatabase db, Session session) {
        m_notification.setContentText(
            getString(R.string.uploading_session_name, session.label)
        );

        SessionDAO sDAO = db.sessionDAO();

        // Begin upload.
        session.state = Session.Status.Uploading;
        sDAO.updateSession(session);

        // TODO: xx.post(session);

        WordDAO wDAO = db.wordDAO();
        List<Integer> wordIDs = wDAO.getIDsForSession(session.id);

        int numWords = wordIDs.size();
        for (int i = 0; i < numWords; ++i) {
            Word word = wDAO.get(wordIDs.get(i));

            notifyProgress((int) (i * 100.0 / numWords));

            // TODO: Replace fake work with actual work.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep failed", e);
            }

            switch (word.state) {
                case Editing:
                    // Fall through.

                case UploadingMeanings:
                    if (word.state == Word.Status.UploadingMeanings) {
                        Log.d(TAG, "Resuming meaning upload for " + word.id);
                    }
                    else {
                        word.state = Word.Status.UploadingMeanings;
                        wDAO.updateWords(word);
                    }

                    // TODO: xx.post(word -> meanings);

                    // Fall through.

                case UploadingAudio:
                    if (word.state == Word.Status.UploadingAudio) {
                        Log.d(TAG, "Resuming audio upload for " + word.id);
                    }
                    else {
                        word.state = Word.Status.UploadingAudio;
                        wDAO.updateWords(word);
                    }

                    // TODO: xx.post(word -> audio);
                    // TODO: xx.delete(word -> local_audio_file);

                    // Fall through.

                case UploadingPicture:
                    if (word.state == Word.Status.UploadingPicture) {
                        Log.d(TAG, "Resuming picture upload for " + word.id);
                    }
                    else {
                        word.state = Word.Status.UploadingPicture;
                        wDAO.updateWords(word);
                    }

                    // TODO: xx.post(word -> picture);
                    // TODO: xx.delete(word -> local_picture_file);

                    word.state = Word.Status.Uploaded;
                    wDAO.updateWords(word);
                    break;

                case Uploaded:
                    Log.d(TAG, "Skipping uploaded word " + word.id);
                    break;
            }
        }

        // Mark as complete.
        session.state = Session.Status.Uploaded;
        sDAO.updateSession(session);

        // TODO: Should we delete it now?
    }

    private void notifyProgress(int progress) {
        m_notification.setProgress(100, progress, false);
        m_notificationManager.notify(NOTIFICATION_ID,  m_notification.build());
    }
}
