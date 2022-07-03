package at.fhooe.mc.mtproject.speechRecognition;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;

public class PorcupineService extends Service {
    private static final String ACCESS_KEY = "5pf9Vllc43fWd1nhBEvWamMCDIUBGvCDNPuI5Xcdjsgopj658W1tsg==";

    private final IBinder mBinder = new LocalBinder();

    private PorcupineManager porcupineManager;
    private ServiceCallbacks mCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath("picovoice_files/move_it_wake_word.ppn")
                    .setSensitivity(0.5f)
                    .build(getApplicationContext(),
                            (keywordIndex) -> {
                                if (mCallback != null) {
                                    mCallback.startSession();
                                }
                            });
            porcupineManager.start();
        } catch (PorcupineException e) {
            Log.e("PORCUPINE_SERVICE", e.toString());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        try {
            porcupineManager.stop();
            porcupineManager.delete();
        } catch (PorcupineException e) {
            Log.e("PORCUPINE", e.toString());
        }
        super.onDestroy();
    }

    public void setCallback(ServiceCallbacks callback) {
        mCallback = callback;
    }

    public class LocalBinder extends Binder {
        public PorcupineService getService() {
            return PorcupineService.this;
        }
    }
}