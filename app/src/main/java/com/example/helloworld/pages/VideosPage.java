package com.example.helloworld.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.activities.YoutubePlayerActivity;
import com.example.helloworld.adapters.VideoListAdapter;
import com.example.helloworld.api.YoutubeApi;
import com.example.helloworld.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideosPage extends Fragment implements VideoListAdapter.OnVideoItemClickListener {
    private SwipeRefreshLayout videoSwipeRefreshLayout;
    private RecyclerView videoListRecyclerView;
    private List<VideoItem> videoItemList;
    public static VideosPage newInstance() {
        return new VideosPage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.videos_page, container, false);
        videoSwipeRefreshLayout = rootView.findViewById(R.id.video_swipe_refresh_layout);

        videoSwipeRefreshLayout.setOnRefreshListener(this::loadVideos);

        videoSwipeRefreshLayout.setRefreshing(true);

        videoListRecyclerView = rootView.findViewById(R.id.video_list_recycler_view);
        videoListRecyclerView.setHasFixedSize(true);

        loadVideos();

        return rootView;
    }

    @Override
    public void onVideoItemClick(int position) {
        VideoItem videoItem = videoItemList.get(position);
        Intent intent = new Intent(getContext(), YoutubePlayerActivity.class);
        intent.putExtra(Constants.INTENT_VIDEO_SNIPPET_KEY, videoItem.getVideoSnippet());
        startActivity(intent);
    }

    private void loadVideos(){
        YoutubeApi.getAllVideosByChannelID(getContext(), Constants.DEFAULT_CHANNEL_ID, new YoutubeApi.ApiResponseListener() {
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
                } catch (Exception e) {
                    videoSwipeRefreshLayout.setRefreshing(false);
                    showErrorDialog(e.getMessage());
                }

                VideoListAdapter adapter = new VideoListAdapter(getContext(), videoItemList);
                adapter.setVideoItemClickListener(VideosPage.this);
                videoListRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
                videoListRecyclerView.setAdapter(adapter);
                videoSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                videoSwipeRefreshLayout.setRefreshing(false);
                showErrorDialog(error);
            }
        });
    }

    private void showErrorDialog(String error){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
