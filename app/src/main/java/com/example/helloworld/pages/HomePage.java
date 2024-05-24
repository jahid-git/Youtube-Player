package com.example.helloworld.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.helloworld.Constants;
import com.example.helloworld.R;
import com.example.helloworld.api.YoutubeApi;
import com.example.helloworld.receivers.CallbackReceiver;
import com.example.helloworld.utils.AppUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class HomePage extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;
    public static HomePage newInstance() {
        return new HomePage();
    }
    private ImageView homePageBanner;
    private ImageView homePageLogo;
    private TextView homePageTitle;
    private TextView homePageChannelInfo;
    private TextView homePageDescription;
    private Button homePageSubscribeBtn;
    private YouTubePlayer youTubePlayer;
    private YouTubePlayerView youTubePlayerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.home_page, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        swipeRefreshLayout.setRefreshing(true);

        homePageBanner = rootView.findViewById(R.id.home_page_banner);
        homePageLogo = rootView.findViewById(R.id.home_page_logo);
        homePageTitle = rootView.findViewById(R.id.home_page_title);
        homePageChannelInfo = rootView.findViewById(R.id.home_page_channel_info);
        homePageDescription = rootView.findViewById(R.id.home_page_description);
        homePageSubscribeBtn = rootView.findViewById(R.id.home_page_subscribe_btn);

        youTubePlayerView = rootView.findViewById(R.id.home_page_youtube_player_view);

        refreshData();

        return rootView;
    }

    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);
        YoutubeApi.getChannelInfoById(getContext(), Constants.DEFAULT_CHANNEL_ID, new YoutubeApi.ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONObject statistics = response.getJSONArray("items").getJSONObject(0).getJSONObject("statistics");
                    JSONObject snippet = response.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                    homePageTitle.setText(snippet.getString("title"));
                    homePageDescription.setText(snippet.getString("description"));
                    Picasso.get().load(snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url")).into(homePageLogo);

                    String customUrl = snippet.getString("customUrl");
                    float subscribers = Float.parseFloat(statistics.getString("subscriberCount")) / 1000;
                    int videos = Integer.parseInt(statistics.getString("videoCount"));
                    String info = customUrl + " ‧ " + subscribers + "K subscribers ‧ " + videos + " videos";
                    homePageChannelInfo.setText(info);
                    homePageSubscribeBtn.setOnClickListener(v -> AppUtils.openLink(v.getContext(), "https://www.youtube.com/" + customUrl));

                } catch (JSONException error){
                    showErrorDialog(error.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                showErrorDialog(error);
            }
        });

        YoutubeApi.getChannelBanner(getContext(), Constants.DEFAULT_CHANNEL_ID, new YoutubeApi.ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject bannerResponse) {
                try {
                    String bannerUrl = bannerResponse.getString("bannerUrl");
                    Picasso.get().load(bannerUrl).into(homePageBanner);
                } catch (JSONException error) {
                    showErrorDialog(error.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                showErrorDialog(error);
            }
        });

        YoutubeApi.getLatestVideoByChannelId(getContext(), Constants.DEFAULT_CHANNEL_ID, new YoutubeApi.ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String videoId = new JSONObject(response.getString("latestVideoId")).getString("videoId");
                    if (youTubePlayer != null){
                        youTubePlayer.cueVideo(videoId, 0);
                    } else {
                        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
                            @Override
                            public void onReady(@NonNull YouTubePlayer player) {
                                super.onReady(player);
                                youTubePlayer = player;
                                youTubePlayer.cueVideo(videoId, CallbackReceiver.currentPlayerTime);
                            }
                        }, new IFramePlayerOptions.Builder().controls(1).build());
                        getLifecycle().addObserver(youTubePlayerView);
                        youTubePlayerView.enableBackgroundPlayback(false);
                    }
                } catch (JSONException error) {
                    showErrorDialog(error.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                showErrorDialog(error);
            }
        });

        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }

    private void showErrorDialog(String error){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Error")
                .setMessage(error)
                .setPositiveButton("Try again", (dialog, which) -> {
                    dialog.dismiss();
                    refreshData();
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
