package com.example.helloworld.adapters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Environment;
import android.text.InputType;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.api.VideoMeta;
import com.example.helloworld.api.YouTubeExtractor;
import com.example.helloworld.api.YtFile;
import com.example.helloworld.models.VideoItem;
import com.example.helloworld.receivers.CallbackReceiver;
import com.example.helloworld.services.FloatWindowService;
import com.example.helloworld.services.VideoDownloadService;
import com.example.helloworld.utils.AppUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.CardViewHolder> {
    private final Context context;
    private final List<VideoItem> videoList;
    private OnVideoItemClickListener clickListener;
    private int selectedQuality = 360;
    private String downloadPath;

    public VideoListAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    public void setVideoItemClickListener(OnVideoItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnVideoItemClickListener {
        void onVideoItemClick(int position);
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final VideoItem video = videoList.get(position);
        String title = video.getTitle().replaceAll("\n", "");
        if (title.length() > 45) {
            title = title.substring(0, 45) + "...";
        }
        String description = video.getDescription().replaceAll("\n", "");
        if (description.length() > 30) {
            description = description.substring(0, 30) + "...";
        }
        holder.videoTitle.setText(title);
        holder.videoDescription.setText(description);
        Picasso.get().load(video.getThumbnail()).into(holder.videoThumbnail);

        holder.videoItemMoreBtn.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_IN);
        holder.playlistSize.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));

        if(video.getListSize() < 0){
            holder.playlistSize.setVisibility(View.GONE);
            holder.videoItemMoreBtn.setVisibility(View.VISIBLE);
            holder.videoItemMoreBtn.setOnClickListener(v -> showListDialog(v.getContext(), video));
        } else {
            holder.videoItemMoreBtn.setVisibility(View.GONE);
            holder.playlistSize.setVisibility(View.VISIBLE);
            holder.playlistSize.setText("" + video.getListSize());
        }


        holder.videoItemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onVideoItemClick(position);
            }
        });
    }
    private void showListDialog(Context context, VideoItem videoItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose option");
        final CharSequence[] items = {"Open via float window", "Open via YouTube", "Share link", "Download"};
        builder.setItems(items, (dialog, which) -> {
            if(which == 0){
                Intent intent = new Intent(context, FloatWindowService.class);
                intent.setAction(Constants.ACTION_START_WINDOW_SERVICE);
                intent.putExtra(Constants.INTENT_VIDEO_ID_KEY, videoItem.getId());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } else if (which == 1){
                AppUtils.openYoutube(context, "https://www.youtube.com/watch?v=" + videoItem.getId() + "&t=" + CallbackReceiver.currentPlayerTime);
            } else if (which == 2){
                AppUtils.shareLink(context, "https://www.youtube.com/watch?v=" + videoItem.getId() + "&t=" + CallbackReceiver.currentPlayerTime);
            } else {
                requestDownload(context, videoItem);
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        CardView videoItemView;
        ImageView videoThumbnail;
        TextView videoTitle;
        TextView videoDescription;
        ImageButton videoItemMoreBtn;
        TextView playlistSize;

        public CardViewHolder(View itemView) {
            super(itemView);
            videoItemView = itemView.findViewById(R.id.video_item_view);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoTitle = itemView.findViewById(R.id.video_title);
            videoDescription = itemView.findViewById(R.id.video_description);
            videoItemMoreBtn = itemView.findViewById(R.id.video_item_more_btn);
            playlistSize = itemView.findViewById(R.id.video_item_playlist_size);
        }
    }


    private void requestDownload(Context context, VideoItem videoItem){
        Toast.makeText(context, "Please wait...", Toast.LENGTH_LONG).show();
        @SuppressLint("StaticFieldLeak")
        YouTubeExtractor youTubeExtractor = new YouTubeExtractor(context) {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if(ytFiles != null){
                    showDownloadQualityDialog(ytFiles, videoItem);
                }
            }
        };
        youTubeExtractor.extract("http://youtube.com/watch?v=" + videoItem.getId());
    }

    @SuppressLint("SetTextI18n")
    private void showDownloadQualityDialog(SparseArray<YtFile> ytFiles, VideoItem videoItem) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("Download Options");

        String title = videoItem.getTitle();

        if(title.length() > 30){
            title = title.substring(0, 30);
        }

        downloadPath = getExternalSdCardDirectory() + "/" + title + getExtension(selectedQuality);

        final EditText pathInput = new EditText(context);
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
            if(!file.exists()) file.mkdirs();
            if(new File(path).exists()) {
                YtFile videoDownloadYTFile = ytFiles.get(getQuality(selectedQuality));
                if (videoDownloadYTFile != null && videoDownloadYTFile.getUrl() != null) {
                    startDownload(videoDownloadYTFile.getUrl(), downloadPath);
                } else {
                    Toast.makeText(context, "Quality not found, try again...", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "File path not found, try again...", Toast.LENGTH_SHORT).show();
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
            Intent intent = new Intent(context, VideoDownloadService.class);
            intent.setAction(Constants.ACTION_START_DOWNLOAD);
            intent.putExtra(Constants.INTENT_DOWNLOAD_URL_KEY, videoDownloadUrl);
            intent.putExtra(Constants.INTENT_DOWNLOAD_FILE_PATH_KEY, videoDownloadPAth);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Toast.makeText(context, "Downloading...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Something is error! try again...", Toast.LENGTH_LONG).show();
        }
    }

    private String getExternalSdCardDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}