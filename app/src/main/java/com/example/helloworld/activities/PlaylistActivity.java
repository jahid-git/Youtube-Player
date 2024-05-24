package com.example.helloworld.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.adapters.VideoListAdapter;
import com.example.helloworld.api.YoutubeApi;
import com.example.helloworld.models.VideoItem;
import com.example.helloworld.receivers.CallbackReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity implements VideoListAdapter.OnVideoItemClickListener {

    private SwipeRefreshLayout videoListSwipeRefreshLayout;
    private RecyclerView videoListRecyclerView;
    private List<VideoItem> videoItemList;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        videoListSwipeRefreshLayout = findViewById(R.id.video_swipe_refresh_layout);

        videoListSwipeRefreshLayout.setOnRefreshListener(this::loadVideos);

        videoListSwipeRefreshLayout.setRefreshing(true);

        videoListRecyclerView = findViewById(R.id.video_list_recycler_view);
        videoListRecyclerView.setHasFixedSize(true);

        loadVideos();

        IntentFilter filter = new IntentFilter(Constants.INTENT_PLAYER_CURRENT_TIME_KEY);
        registerReceiver(new CallbackReceiver(), filter);
    }

    @Override
    public void onVideoItemClick(int position) {
        VideoItem videoItem = videoItemList.get(position);
        Intent intent = new Intent(this, YoutubePlayerActivity.class);
        intent.putExtra(Constants.INTENT_VIDEO_SNIPPET_KEY, videoItem.getVideoSnippet());
        startActivity(intent);
    }

    private void loadVideos(){
        videoListSwipeRefreshLayout.setRefreshing(true);
        YoutubeApi.getPlaylistItems(this, getIntent().getStringExtra(Constants.INTENT_PLAYLIST_ID_KEY), new YoutubeApi.ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                videoItemList = new ArrayList<>();
                try {
                    JSONArray videoListJSONArray = response.getJSONArray("items");
                    for (int i = 0;i < videoListJSONArray.length();i++){
                        JSONObject videoSnippet = videoListJSONArray.getJSONObject(i).getJSONObject("snippet");
                        VideoItem videoItem = new VideoItem();
                        videoItem.setId(videoSnippet.getJSONObject("resourceId").getString("videoId"));
                        videoItem.setTitle(videoSnippet.getString("title"));
                        videoItem.setDescription(videoSnippet.getString("description"));
                        videoItem.setPublishedAt(videoSnippet.getString("publishedAt"));
                        videoItem.setThumbnail(videoSnippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                        videoItem.setVideoSnippet(videoSnippet.toString());
                        videoItemList.add(videoItem);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } finally {
                    videoListSwipeRefreshLayout.setRefreshing(false);
                }

                VideoListAdapter adapter = new VideoListAdapter(PlaylistActivity.this, videoItemList);
                adapter.setVideoItemClickListener(PlaylistActivity.this);
                videoListRecyclerView.setLayoutManager(new GridLayoutManager(PlaylistActivity.this, 1));
                videoListRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                videoListSwipeRefreshLayout.setRefreshing(false);
                showErrorDialog(error);
            }
        });
    }

    private void showErrorDialog(String error){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error")
                .setMessage(error)
                .setPositiveButton("Try again", (dialog, which) -> {
                    dialog.dismiss();
                    loadVideos();
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}