package com.native1;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.heraldprox.herald.sensor.service.NotificationService;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("ReactNative", "CIAO STOP SERVICE FROM NOTIFICATION");

        Application app = (Application) context.getApplicationContext();
        NotificationService.shared(app).stopForegroundService();

        HeraldService.stop();
    }
}
