/**
 * YoutubeApi - A class for interacting with the YouTube Data API.
 * This class provides methods to retrieve playlist items and video details using the YouTube API.
 * Requires an API key for authentication.
 *
 * @author MD. Jahid Hasan
 * @date November 21, 2023
 */

package com.example.helloworld.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class YoutubeApi {

    // Base URL for the YouTube Data API
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    // YouTube API key for authentication
    private static final String YOUTUBE_API_KEY = "AIzaSyBVkCtypBb5XwrqG3mwlr1DjaHZpd1yEUI";

    // Retrieves playlist items from YouTube based on the provided playlist ID.
    public static void getPlaylistItems(Context context, String playlistId, final ApiResponseListener listener) {
        String url = BASE_URL + "playlistItems?part=snippet&playlistId=" + playlistId + "&key=" + YOUTUBE_API_KEY + "&maxResults=50";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                listener::onSuccess,
                error -> listener.onError(error.toString()));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    // Retrieves details of a YouTube video based on the provided video ID.
    public static void getVideoById(Context context, String videoId, final ApiResponseListener listener) {
        String url = BASE_URL + "videos?part=snippet&id=" + videoId + "&key=" + YOUTUBE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                listener::onSuccess,
                error -> listener.onError(error.getMessage()));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void getChannelInfoById(Context context, String channelId, final ApiResponseListener listener) {
        String url = BASE_URL + "channels?part=snippet,contentDetails,statistics&id=" + channelId + "&key=" + YOUTUBE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                listener::onSuccess,
                error -> listener.onError(error.toString()));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }
    public static void getChannelBanner(Context context, String channelId, final ApiResponseListener listener) {
        // Fetch the channel information, including branding settings
        String url = BASE_URL + "channels?part=brandingSettings&id=" + channelId + "&key=" + YOUTUBE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject items = response.getJSONArray("items").getJSONObject(0);
                        JSONObject brandingSettings = items.getJSONObject("brandingSettings");
                        if (brandingSettings.has("image")) {
                            JSONObject imageSettings = brandingSettings.getJSONObject("image");
                            String bannerUrl = imageSettings.getString("bannerExternalUrl");
                            listener.onSuccess(new JSONObject().put("bannerUrl", bannerUrl));
                        } else {
                            listener.onError("No banner found for the channel");
                        }
                    } catch (JSONException e) {
                        listener.onError(e.getMessage());
                    }
                },
                error -> listener.onError(error.toString()));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void getAllVideosByChannelID(Context context, String channelId, final ApiResponseListener listener) {
        getChannelInfoById(context, channelId, new ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject channelInfoResponse) {
                try {
                    JSONArray items = channelInfoResponse.getJSONArray("items");
                    if (items.length() > 0) {
                        JSONObject contentDetails = items.getJSONObject(0).getJSONObject("contentDetails");
                        String uploadsPlaylistId = contentDetails.getJSONObject("relatedPlaylists").getString("uploads");
                        getPlaylistItems(context, uploadsPlaylistId, listener);
                    } else {
                        listener.onError("No channel information found");
                    }
                } catch (JSONException e) {
                    listener.onError(e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    public static void getAllPlaylistsInfoByChannelId(Context context, String channelId, final ApiResponseListener listener) {
        // Construct the URL to get all playlists by channel ID
        String url = BASE_URL + "playlists?part=snippet,contentDetails&channelId=" + channelId + "&maxResults=50&key=" + YOUTUBE_API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                listener::onSuccess,
                error -> listener.onError(error.toString()));

        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void getLatestVideoByChannelId(Context context, String channelId, final ApiResponseListener listener) {
        // Fetch all videos from the uploads playlist
        getAllVideosByChannelID(context, channelId, new ApiResponseListener() {
            @Override
            public void onSuccess(JSONObject videosResponse) {
                try {
                    JSONArray items = videosResponse.getJSONArray("items");

                    if (items.length() > 0) {
                        // Initialize with the first video as the latest
                        JSONObject latestVideo = items.getJSONObject(0);

                        // Iterate through the rest of the videos to find the latest
                        for (int i = 1; i < items.length(); i++) {
                            JSONObject currentVideo = items.getJSONObject(i);
                            String dateCurrent = currentVideo.getJSONObject("snippet").getString("publishedAt");
                            String dateLatest = latestVideo.getJSONObject("snippet").getString("publishedAt");

                            if (dateCurrent.compareTo(dateLatest) > 0) {
                                latestVideo = currentVideo;
                            }
                        }

                        // Get the information of the latest video
                        String latestVideoId = latestVideo.getJSONObject("snippet").getString("resourceId");

                        // Return the information of the latest video
                        listener.onSuccess(new JSONObject().put("latestVideoId", latestVideoId));
                    } else {
                        listener.onError("No videos found for the channel");
                    }
                } catch (JSONException e) {
                    listener.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    /**
     * Interface for handling YouTube API response callbacks.
     */
    public interface ApiResponseListener {
        void onSuccess(JSONObject response);

        void onError(String error);
    }
}
