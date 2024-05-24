package com.example.helloworld.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.helloworld.Constants;

public class CallbackReceiver extends BroadcastReceiver {
    public static float currentPlayerTime = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Constants.ACTION_AUDIO_CALLBACK_RECEIVER)) {
            CallbackReceiver.currentPlayerTime = intent.getFloatExtra(Constants.INTENT_PLAYER_CURRENT_TIME_KEY, 0);
        }
    }
}