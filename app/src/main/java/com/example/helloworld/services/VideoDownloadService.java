package com.example.helloworld.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.helloworld.Constants;
import com.example.helloworld.R;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoDownloadService extends Service {
    private static final int NOTIFICATION_ID = 2;
    private static final String CHANNEL_ID = "video_download_channel";

    private Handler handler;
    private int downloadProgress;
    private volatile boolean stopDownloadRequested = true;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        notificationManager = getSystemService(NotificationManager.class);
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        createNotificationChannel();

        int notificationColor = ContextCompat.getColor(this, R.color.colorPrimary);

        Intent stopIntent = new Intent(this, VideoDownloadService.class);
        stopIntent.setAction(Constants.ACTION_STOP_DOWNLOAD);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading...")
                .setContentText("Downloading in the background")
                .setSmallIcon(R.drawable.ic_download)
                .addAction(android.R.drawable.ic_media_pause, getResources().getString(R.string.cancel_btn), stopPendingIntent)
                .setColor(notificationColor)
                .setSound(null);

        return notificationBuilder.build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Video Download Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void startDownload(final String url, final String filePath) {
        stopDownloadRequested = false;

        new Thread(() -> downloadFile(url, filePath)).start();
    }

    private void downloadFile(String urlString, String filePath) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            int fileLength = urlConnection.getContentLength();
            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                filePath = getCacheDir().getPath() + "/Video.mp4";
            }

            FileOutputStream fileOutputStream = new FileOutputStream(filePath);

            byte[] buffer = new byte[1024];
            int bufferLength;
            int total = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bufferLength);
                total += bufferLength;

                downloadProgress = (int) ((total * 100) / fileLength);
                updateProgress();

                if (stopDownloadRequested) {
                    fileOutputStream.close();
                    urlConnection.disconnect();
                    break;
                }
            }

            fileOutputStream.close();
            urlConnection.disconnect();
            downloadComplete();
        } catch (IOException e) {
            downloadFailed();
        } finally {
            downloadFailed();
        }
    }

    private void updateProgress() {
        handler.post(() -> {
            notificationBuilder.setProgress(100, downloadProgress, false);
            notificationBuilder.setContentText("Downloading " + downloadProgress + "%");
            notificationBuilder.setSound(null);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        });
    }

    private void downloadComplete() {
        stopSelf();
    }

    private void downloadFailed() {
        stopSelf();
    }

    public void stopDownload() {
        stopDownloadRequested = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (Constants.ACTION_STOP_DOWNLOAD.equals(action)) {
                stopDownload();
            } else if (Constants.ACTION_START_DOWNLOAD.equals(action)) {
                if(stopDownloadRequested) {
                    String url = intent.getStringExtra(Constants.INTENT_DOWNLOAD_URL_KEY);
                    String filePath = intent.getStringExtra(Constants.INTENT_DOWNLOAD_FILE_PATH_KEY);
                    startDownload(url, filePath);
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
