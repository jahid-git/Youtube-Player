package com.example.helloworld.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.receivers.CallbackReceiver;
import com.example.helloworld.utils.PrefsUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class FloatWindowService extends Service {

    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams params;
    private View removeArea;
    private ImageView removeIndicatorIcon;
    private YouTubePlayer youTubePlayer;
    private YouTubePlayerView youTubePlayerView;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        showFloatWindow();
    }

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    private void showFloatWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams paramsRemoveArea = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsRemoveArea.gravity = Gravity.BOTTOM | Gravity.CENTER;
        removeArea = LayoutInflater.from(this).inflate(R.layout.remove_area, null);
        windowManager.addView(removeArea, paramsRemoveArea);

        removeIndicatorIcon = removeArea.findViewById(R.id.remove_area_indicator_icon);

        floatView = LayoutInflater.from(this).inflate(R.layout.float_window_layout, null);

        // Set up the layout parameters for the floating window
        params = new WindowManager.LayoutParams(
                PrefsUtils.getPrefs(Constants.FLOAT_WINDOW_WIDTH_KEY, 360),
                PrefsUtils.getPrefs(Constants.FLOAT_WINDOW_HEIGHT_KEY, 230),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;


        windowManager.addView(floatView, params);

        youTubePlayerView = floatView.findViewById(R.id.float_youtube_player_view);


        // Set up touch listener to move the floating window
        floatView.findViewById(R.id.float_touch_area).setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatView, params);
                    updateRemoveAreaVisibility(params.y);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (isOverRemoveArea(params.y)) {
                        stopService();
                    }
                    removeArea.setVisibility(View.GONE);
                    return true;
            }
            return false;
        });

        // Set up the notification with the stop action
        Notification notification = createNotification();
        startForeground(1, notification);
    }
    private void updateRemoveAreaVisibility(int y) {
        removeArea.setVisibility(View.VISIBLE);
        if((y + floatView.getHeight()) > (windowManager.getDefaultDisplay().getHeight() * 0.8)){
            removeIndicatorIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        } else {
            removeIndicatorIcon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
    }

    private boolean isOverRemoveArea(int y) {
        return (y + floatView.getHeight()) > (windowManager.getDefaultDisplay().getHeight() * 0.8);
    }

    private void stopService() {
        if (windowManager != null && removeArea != null) {
            windowManager.removeView(removeArea);
        }

        // Remove the floating window
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
        }

        // Stop the service
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification() {
        int notificationColor = ContextCompat.getColor(this, R.color.colorPrimary);

        // Create an intent to handle the stop action
        Intent stopIntent = new Intent(this, FloatWindowService.class);
        stopIntent.setAction(Constants.ACTION_START_WINDOW_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create the notification with the stop action
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Floating Window Service")
                .setContentText("Running in the foreground")
                .setSmallIcon(R.drawable.ic_float_window)
                .setColor(notificationColor)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);

        // Create a notification channel if using Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "float_window_channel",
                    "Float Window Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }

        return builder.build();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Constants.ACTION_STOP_WINDOW_SERVICE.equals(intent.getAction())) {
            stopService();
        } else if (intent != null && Constants.ACTION_START_WINDOW_SERVICE.equals(intent.getAction())) {
            if(youTubePlayer != null){
                stopService();
                return START_STICKY;
            }
            youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer player) {
                    super.onReady(player);
                    player.addListener(new AbstractYouTubePlayerListener() {
                        @Override
                        public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                            super.onCurrentSecond(youTubePlayer, second);
                            Intent intent = new Intent(Constants.ACTION_AUDIO_CALLBACK_RECEIVER);
                            intent.putExtra(Constants.INTENT_PLAYER_CURRENT_TIME_KEY, second);
                            sendBroadcast(intent);
                        }
                    });

                    youTubePlayer = player;
                    String videoId = intent.getStringExtra(Constants.INTENT_VIDEO_ID_KEY);
                    if (videoId != null) {
                        youTubePlayer.loadVideo(videoId, CallbackReceiver.currentPlayerTime);
                    }
                }
            }, new IFramePlayerOptions.Builder().controls(0).build());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && floatView != null) {
            try {
                windowManager.removeView(floatView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (windowManager != null && removeArea != null) {
            try {
                windowManager.removeView(removeArea);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}