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
import com.example.helloworld.activities.PlaylistActivity;
import com.example.helloworld.adapters.VideoListAdapter;
import com.example.helloworld.api.YoutubeApi;
import com.example.helloworld.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPage extends Fragment implements VideoListAdapter.OnVideoItemClickListener {
    private SwipeRefreshLayout playlistSwipeRefreshLayout;
    private RecyclerView playlistListRecyclerView;
    private List<VideoItem> playlistItemList;
    public static PlaylistPage newInstance() {
        return new PlaylistPage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.playlist_page, container, false);
        playlistSwipeRefreshLayout = rootView.findViewById(R.id.playlist_swipe_refresh_layout);

        playlistSwipeRefreshLayout.setOnRefreshListener(this::loadVideos);

        playlistSwipeRefreshLayout.setRefreshing(true);

        playlistListRecyclerView = rootView.findViewById(R.id.playlist_list_recycler_view);
        playlistListRecyclerView.setHasFixedSize(true);

        loadVideos();

        return rootView;
    }

    @Override
    public void onVideoItemClick(int position) {
        VideoItem playlistItem = playlistItemList.get(position);
        Intent intent = new Intent(getContext(), PlaylistActivity.class);
        intent.putExtra(Constants.INTENT_PLAYLIST_ID_KEY, playlistItem.getId());
        startActivity(intent);
    }

    private void loadVideos(){
        YoutubeApi.getAllPlaylistsInfoByChannelId(getContext(), Constants.DEFAULT_CHANNEL_ID, new YoutubeApi.ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                playlistItemList = new ArrayList<>();
                try {
                    JSONArray videoListJSONArray = response.getJSONArray("items");
                    for (int i = 0;i < videoListJSONArray.length();i++){
                        JSONObject videoSnippet = videoListJSONArray.getJSONObject(i).getJSONObject("snippet");
                        VideoItem videoItem = new VideoItem();
                        videoItem.setId(videoListJSONArray.getJSONObject(i).getString("id"));
                        videoItem.setTitle(videoSnippet.getString("title"));
                        videoItem.setDescription(videoSnippet.getString("description"));
                        videoItem.setPublishedAt(videoSnippet.getString("publishedAt"));
                        videoItem.setThumbnail(videoSnippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                        videoItem.setVideoSnippet(videoSnippet.toString());
                        videoItem.setListSize(videoListJSONArray.getJSONObject(i).getJSONObject("contentDetails").getInt("itemCount"));
                        playlistItemList.add(videoItem);
                    }
                } catch (Exception e) {
                    playlistSwipeRefreshLayout.setRefreshing(false);
                    showErrorDialog(e.getMessage());
                }

                VideoListAdapter adapter = new VideoListAdapter(getContext(), playlistItemList);
                adapter.setVideoItemClickListener(PlaylistPage.this);
                playlistListRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
                playlistListRecyclerView.setAdapter(adapter);
                playlistSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                playlistSwipeRefreshLayout.setRefreshing(false);
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
