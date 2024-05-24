package com.example.helloworld.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.receivers.CallbackReceiver;

import java.io.IOException;

public class AudioPlayerService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "audio_player_channel";
    private final Handler handler = new Handler();
    private static Runnable updateProgressTask;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        createNotificationChannel();

        // Set the color to your desired color resource or ARGB value
        int notificationColor = ContextCompat.getColor(this, R.color.colorPrimary);

        Intent stopIntent = new Intent(this, AudioPlayerService.class);
        stopIntent.setAction(Constants.ACTION_STOP_AUDIO);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Playing in the background")
                .setSmallIcon(R.drawable.ic_play_circle)
                .addAction(android.R.drawable.ic_media_pause, getResources().getString(R.string.stop_btn), stopPendingIntent)
                .setColor(notificationColor)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Player Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void startPlayback(String audioUrl, float time) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            try {
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.seekTo((int) (time * 1000));

                updateProgressTask = new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            float currentPosition = (mediaPlayer.getCurrentPosition() / 1000f);
                            Intent intent = new Intent(Constants.ACTION_AUDIO_CALLBACK_RECEIVER);
                            intent.putExtra(Constants.INTENT_PLAYER_CURRENT_TIME_KEY, currentPosition);
                            sendBroadcast(intent);
                            handler.postDelayed(this, 1000);
                        }
                    }
                };

                handler.postDelayed(updateProgressTask,1000);
            } catch (IOException ignored) {

            }
        }
    }

    // Stop audio playback
    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (updateProgressTask != null) {
            handler.removeCallbacks(updateProgressTask);
        }
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (Constants.ACTION_STOP_AUDIO.equals(action)) {
                stopPlayback();
            } else if (Constants.ACTION_START_AUDIO.equals(action)) {
                String audioUrl = intent.getStringExtra(Constants.INTENT_AUDIO_URL_KEY);
                startPlayback(audioUrl, CallbackReceiver.currentPlayerTime);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }
}
