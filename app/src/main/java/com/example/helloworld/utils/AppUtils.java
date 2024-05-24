package com.example.helloworld.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class AppUtils {
    public static void openLink(Context context, String link){
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }
    public static void shareLink(Context context, String link){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, link);
        context.startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }
    public static void openYoutube(Context context, String videoUrl){
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            intent.setPackage("com.google.android.youtube");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                context.startActivity(browserIntent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void openFacebook(Context context, String facebookUrl) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)));
        } catch (Exception e) {
            showToast(context, "Unable to open Facebook.");
        }
    }

    public static void rate(Context context) {
        try {
            Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(goToMarket);
        }
    }

    public static void feedback(Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "graphsee@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on Your App");
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send feedback via email"));
        } catch (ActivityNotFoundException e) {
            showToast(context, "No email app installed");
        }
    }

    public static String extractYouTubeVideoId(String youtubeUrl) {
        int start = youtubeUrl.indexOf("v=");
        int startLive = youtubeUrl.indexOf("live/");
        if(start > 0){
            youtubeUrl = youtubeUrl.substring(start + 2, youtubeUrl.indexOf("&"));
        } else if(startLive > 0){
            youtubeUrl = youtubeUrl.substring(startLive + 5,youtubeUrl.indexOf("?"));
        } else {
            youtubeUrl = youtubeUrl.substring(youtubeUrl.indexOf(".be/") + 4, youtubeUrl.indexOf("?"));
        }
        return youtubeUrl;
    }

    private static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
