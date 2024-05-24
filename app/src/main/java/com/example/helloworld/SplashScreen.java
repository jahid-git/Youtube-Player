package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.helloworld.activities.HomeActivity;
import com.example.helloworld.utils.PrefsUtils;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {
    private static final long SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        PrefsUtils.init(this);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, HomeActivity.class));
            finish();
        }, SPLASH_DURATION);
    }

}