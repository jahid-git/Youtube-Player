package com.example.helloworld.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.helloworld.Constants;

public class PrefsUtils {
    private static SharedPreferences sp;

    public static void init(Context context){
        sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }
    public static void setPrefs(String key, String value){
        sp.edit().putString(key, value).apply();
    }

    public static void setPrefs(String key, int value){
        sp.edit().putInt(key, value).apply();
    }

    public static void setPrefs(String key, Boolean value){
        sp.edit().putBoolean(key, value).apply();
    }

    public static String getPrefs(String key, String defaultValue){
        return sp.getString(key, defaultValue);
    }

    public static int getPrefs(String key, int defaultValue){
        return sp.getInt(key, defaultValue);
    }

    public static boolean getPrefs(String key, boolean defaultValue){
        return sp.getBoolean(key, defaultValue);
    }

    @SuppressLint("CommitPrefEdits")
    public static void clearPrefs(){
        sp.edit().clear();
        setPrefs(Constants.FIRST_TIME_APP_OPEN, false);
    }
}
