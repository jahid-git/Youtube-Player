package com.example.helloworld.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.api.VideoMeta;
import com.example.helloworld.api.YouTubeExtractor;
import com.example.helloworld.api.YoutubeApi;
import com.example.helloworld.api.YtFile;
import com.example.helloworld.models.VideoItem;
import com.example.helloworld.receivers.CallbackReceiver;
import com.example.helloworld.services.FloatWindowService;
import com.example.helloworld.services.VideoDownloadService;
import com.example.helloworld.utils.AppUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class YoutubePlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private VideoItem videoItem;
    private YouTubePlayer youTubePlayer;
    private boolean isFullscreen = false;
    private int selectedQuality = 360;
    private String downloadPath;
    private YouTubePlayerView youTubePlayerView;
    private TextView videoTitleTextView;
    private TextView videoDescriptionTextView;
    private TextView videoPublishedAtTextView;

    @SuppressLint({"StaticFieldLeak", "UnspecifiedRegisterReceiverFlag", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);


        // Initialize UI components
        View shortScreenContainer = findViewById(R.id.short_screen_view_container);
        FrameLayout fullScreenContainer = findViewById(R.id.full_screen_view_container);
        youTubePlayerView = findViewById(R.id.youtube_player_view);

        videoTitleTextView = findViewById(R.id.video_title);
        videoDescriptionTextView = findViewById(R.id.video_description);
        videoPublishedAtTextView = findViewById(R.id.video_published_at);


        ImageView videoFloatWindowBtn = findViewById(R.id.video_float_window_btn);
        ImageView videoDownloadBtn = findViewById(R.id.video_download_btn);
        ImageView videoShareBtn = findViewById(R.id.video_share_btn);
        ImageView videoOpenYoutubeBtn = findViewById(R.id.video_open_youtube_btn);
        Button videoSubscribeBtn = findViewById(R.id.video_subscribe_btn);

        videoFloatWindowBtn.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);
        videoDownloadBtn.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);
        videoShareBtn.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);
        videoOpenYoutubeBtn.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);

        videoFloatWindowBtn.setOnClickListener(this);
        videoDownloadBtn.setOnClickListener(this);
        videoShareBtn.setOnClickListener(this);
        videoOpenYoutubeBtn.setOnClickListener(this);
        videoSubscribeBtn.setOnClickListener(this);

        youTubePlayerView.addFullscreenListener(new FullscreenListener() {
            @Override
            public void onEnterFullscreen(@NonNull View fullscreenView, @NonNull Function0<Unit> function0) {
                isFullscreen = true;
                shortScreenContainer.setVisibility(View.GONE);
                fullScreenContainer.setVisibility(View.VISIBLE);
                fullScreenContainer.addView(fullscreenView);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                toggleFullscreen();
            }

            @Override
            public void onExitFullscreen() {
                isFullscreen = false;
                fullScreenContainer.setVisibility(View.GONE);
                fullScreenContainer.removeAllViews();
                shortScreenContainer.setVisibility(View.VISIBLE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                toggleFullscreen();
            }
        });


        videoItem = new VideoItem();

        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            final String sharedLink = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedLink != null) {
                String videoId = AppUtils.extractYouTubeVideoId(sharedLink);
                Toast.makeText(this, "" + videoId, Toast.LENGTH_LONG).show();
                YoutubeApi.getVideoById(YoutubePlayerActivity.this, videoId , new YoutubeApi.ApiResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            JSONObject videoSnippet = response.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                            videoItem.setId(response.getJSONArray("items").getJSONObject(0).getString("id"));
                            videoItem.setTitle(videoSnippet.getString("title"));
                            videoItem.setDescription(videoSnippet.getString("description"));
                            videoItem.setPublishedAt(videoSnippet.getString("publishedAt"));
                            videoItem.setThumbnail(videoSnippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                            videoItem.setVideoSnippet(videoSnippet.toString());

                            initializePlayer();
                        } catch (JSONException error) {
                            showErrorDialog(error.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showErrorDialog(error);
                    }
                });
            }
        } else {
            try {
                JSONObject videoSnippet = new JSONObject(Objects.requireNonNull(getIntent().getStringExtra(Constants.INTENT_VIDEO_SNIPPET_KEY)));
                videoItem.setId(videoSnippet.getJSONObject("resourceId").getString("videoId"));
                videoItem.setTitle(videoSnippet.getString("title"));
                videoItem.setDescription(videoSnippet.getString("description"));
                videoItem.setPublishedAt(videoSnippet.getString("publishedAt"));
                videoItem.setThumbnail(videoSnippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                videoItem.setVideoSnippet(videoSnippet.toString());
                initializePlayer();
            } catch (JSONException error){
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void initializePlayer(){
        videoTitleTextView.setText(videoItem.getTitle());
        videoDescriptionTextView.setText(videoItem.getDescription());
        videoPublishedAtTextView.setText("Published at: " + videoItem.getPublishedAt().replace("T", "  "));

        // Set up YouTubePlayerView with listener for video loading and API data retrieval
        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer player) {
                super.onReady(player);
                player.addListener(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                        super.onCurrentSecond(youTubePlayer, second);
                        CallbackReceiver.currentPlayerTime = second;
                    }
                });

                youTubePlayer = player;
                youTubePlayer.loadVideo(videoItem.getId() + "", 0);
            }
        }, new IFramePlayerOptions.Builder().controls(1).modestBranding(0).fullscreen(1).build());

    }


    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            youTubePlayer.toggleFullscreen();
            return;
        }
        super.onBackPressed();
    }

    private void toggleFullscreen() {
        Window window = getWindow();
        if (!isFullscreen) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.video_float_window_btn){
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, Constants.FLOAT_WINDOW_PERMISSION_REQUEST);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(YoutubePlayerActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.STORAGE_PERMISSION_REQUEST);
                    } else {
                        showFloatWindow();
                    }
                } else {
                    showFloatWindow();
                }
            }
        } else if(id == R.id.video_download_btn){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(YoutubePlayerActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.WRITE_EXTERNAL_STORAGE},Constants.STORAGE_PERMISSION_REQUEST);
                } else {
                    requestDownload();
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(YoutubePlayerActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.STORAGE_PERMISSION_REQUEST);
                } else {
                    requestDownload();
                }
            }
        } else if(id == R.id.video_open_youtube_btn){
            AppUtils.openYoutube(this, "https://www.youtube.com/watch?v=" + videoItem.getId() + "&t=" + CallbackReceiver.currentPlayerTime);
        } else if(id == R.id.video_share_btn){
            AppUtils.shareLink(this, "https://www.youtube.com/watch?v=" + videoItem.getId() + "&t=" + CallbackReceiver.currentPlayerTime);
        } else if(id == R.id.video_subscribe_btn){
            AppUtils.openYoutube(this, "https://www.youtube.com/" + Constants.DEFAULT_CHANNEL_CUSTOM_URL);
        }
    }

    private void showFloatWindow(){
        Intent intent = new Intent(this, FloatWindowService.class);
        intent.setAction(Constants.ACTION_START_WINDOW_SERVICE);
        intent.putExtra(Constants.INTENT_VIDEO_ID_KEY, videoItem.getId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestDownload(){
        Toast.makeText(YoutubePlayerActivity.this, "Please wait...", Toast.LENGTH_LONG).show();
        @SuppressLint("StaticFieldLeak")
        YouTubeExtractor youTubeExtractor = new YouTubeExtractor(this) {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if(ytFiles != null){
                    showDownloadQualityDialog(ytFiles);
                }
            }
        };
        youTubeExtractor.extract("http://youtube.com/watch?v=" + videoItem.getId());
    }

    @SuppressLint("SetTextI18n")
    private void showDownloadQualityDialog(SparseArray<YtFile> ytFiles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Download Options");

        String title = videoItem.getTitle();

        if(title.length() > 30){
            title = title.substring(0, 30);
        }

        downloadPath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + title + getExtension(selectedQuality);

        final EditText pathInput = new EditText(this);
        pathInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        pathInput.setGravity(Gravity.TOP | Gravity.START);
        pathInput.setHint("Enter download path");
        pathInput.setText(downloadPath);

        final CharSequence[] qualityOptions = {"MP3", "360p", "720p"};
        int defaultIndex = getQualityIndex(selectedQuality);
        builder.setSingleChoiceItems(qualityOptions, defaultIndex, (dialog, which) -> {
            selectedQuality = getQualityValue(which);
            String path = pathInput.getText().toString();
            path = path.substring(0, path.lastIndexOf("."));
            pathInput.setText(path + getExtension(selectedQuality));
        });
        builder.setView(pathInput);
        builder.setPositiveButton("Download", (dialog, which) -> {
            downloadPath = pathInput.getText().toString();
            String path = downloadPath.substring(0, downloadPath.lastIndexOf("/"));
            File file = new File(path);
            if(!file.exists()){
                file.mkdirs();
            }
            if(new File(path).exists()) {
                YtFile videoDownloadYTFile = ytFiles.get(getQuality(selectedQuality));
                if (videoDownloadYTFile != null && videoDownloadYTFile.getUrl() != null) {
                    startDownload(videoDownloadYTFile.getUrl(), downloadPath);
                } else {
                    Toast.makeText(YoutubePlayerActivity.this, "Quality not found, try again...", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(YoutubePlayerActivity.this, "File path not found, try again...", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    private int getQualityValue(int index) {
        switch (index) {
            case 0:
                return 140;
            case 1:
                return 360;
            case 2:
                return 720;
            default:
                return -1;
        }
    }

    private int getQualityIndex(int value) {
        switch (value) {
            case 140:
                return 0;
            case 360:
                return 1;
            case 720:
                return 2;
            default:
                return -1;
        }
    }

    private int getQuality(int value) {
        switch (value) {
            case 140:
                return 140;
            case 360:
                return 18;
            case 720:
                return 22;
            default:
                return 0;
        }
    }

    private String getExtension(int value) {
        switch (value) {
            case 140:
                return ".mp3";
            case 360:
            case 720:
                return ".mp4";
            default:
                return "";
        }
    }

    private void startDownload(String videoDownloadUrl, String videoDownloadPAth){
        if(videoDownloadUrl != null) {
            Intent intent = new Intent(this, VideoDownloadService.class);
            intent.setAction(Constants.ACTION_START_DOWNLOAD);
            intent.putExtra(Constants.INTENT_DOWNLOAD_URL_KEY, videoDownloadUrl);
            intent.putExtra(Constants.INTENT_DOWNLOAD_FILE_PATH_KEY, videoDownloadPAth);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something is error! try again...", Toast.LENGTH_LONG).show();
        }
    }

    private void showErrorDialog(String error){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error")
                .setMessage(error)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}