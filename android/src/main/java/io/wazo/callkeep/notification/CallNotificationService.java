package io.wazo.callkeep.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import io.wazo.callkeep.CallKeepModule;

public class CallNotificationService extends Service {
    static MediaPlayer ringtonePlayer;
    static NotificationManager notificationManager;
    private static boolean isCallAccepted = false;
    private static String TAG_IS_CALL_ACCEPTED = "isCallAccepted";
    private static String callerName="Name",number="Number",uuid;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callerName = intent.getStringExtra("callerName");
        number = intent.getStringExtra("number");
        uuid = intent.getStringExtra("uuid");
        showNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    public static void callAnswer() {
        ringtonePlayer.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotification() {

        NotificationChannel channel = new NotificationChannel("channel01", "name",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("description");
        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Intent callAcceptedIntent = new Intent(getApplicationContext(), CallHandler.class);
        callAcceptedIntent.putExtra(TAG_IS_CALL_ACCEPTED, true);
        Intent callDeclinedIntent = new Intent(getApplicationContext(), CallHandler.class);
        callDeclinedIntent.putExtra(TAG_IS_CALL_ACCEPTED, false);

        PendingIntent callAcceptedPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, callAcceptedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent callDeclinedPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, callDeclinedIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean needRing = am.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
        if (needRing) {
            ringtonePlayer = new MediaPlayer();
            ringtonePlayer.setOnPreparedListener(mediaPlayer -> {
                try {
                    ringtonePlayer.start();
                } catch (Throwable e) {
                    Log.i("error", e.getMessage());
                }
            });
            ringtonePlayer.setLooping(true);
            ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
            am.requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN);
            try {
                ringtonePlayer.setDataSource(this, Uri.parse(Settings.System.DEFAULT_RINGTONE_URI.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            ringtonePlayer.prepareAsync();

        }

        Notification notification = new NotificationCompat.Builder(this, "channel01")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_dialog_info))
                .setContentTitle(callerName)
                .setContentText(number)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .addAction(android.R.drawable.ic_dialog_info, "Decline", callDeclinedPendingIntent)
                .addAction(android.R.drawable.ic_dialog_info, "Answer", callAcceptedPendingIntent)
                .setOngoing(true)
                .setAutoCancel(true)
                .setFullScreenIntent(callAcceptedPendingIntent, true)
                .build();

        notificationManager.notify(0, notification);
    }


    static void stopRingtone() {
        ringtonePlayer.stop();
        ringtonePlayer.release();
        notificationManager.cancelAll();

    }

    static void handleNotificationAction(Intent intent) {
        isCallAccepted = intent.getBooleanExtra(TAG_IS_CALL_ACCEPTED, false);

        if (isCallAccepted) {
//            Log.i("log", "handleNotificationAction: Call accepted");
            CallKeepModule.answerIncomingCall(uuid);
        } else {
            CallKeepModule.rejectCall(uuid);
//            Log.i("log", "handleNotificationAction: Call declined");
        }

        stopRingtone();

    }

}
