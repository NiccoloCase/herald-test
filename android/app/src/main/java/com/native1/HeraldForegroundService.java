package com.native1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.ArrayList;
import java.util.List;
import io.heraldprox.herald.sensor.Device;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.SensorArray;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.service.NotificationService;


import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.SensorDelegate;

public class HeraldForegroundService extends Service {
    private HeraldForegroundService context;
    private boolean isKilled;
    private Message message;

    private MessagesClient messagesClient;

    private HeraldService heraldService;

    String tag = "PayloadExample";
    // Sensor for proximity detection
    private SensorArray sensor = null;

    private final static String NOTIFICATION_CHANNEL_ID = "HERALD_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private final static String NOTIFICATION_CHANNEL_NAME = "HERALD_NOTIFICATION_CHANNEL_NAME";

    MyCallback myCallback;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        isKilled = false;



        Log.d("ReactNative", "INIZIATO FOREGROUND");


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Channel
            final String CHANNEL_ID = "Attività in Background";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            // Azioni
            Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            // Notifica
            Notification.Builder notification = new Notification.Builder(context, CHANNEL_ID)
                    .setContentText("Ora sei visibile dagli altri utenti vicini a te")
                    .setContentTitle("Sei online!")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setContentIntent(contentIntent)
                    .setWhen(System.currentTimeMillis())
                    .setUsesChronometer(true);

            // Avvia attività
            startForeground(1001, notification.build());
        }

        runClock();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private void stop(){
        isKilled = true;
        //if(heraldService != null) heraldService.stop(getCurrentActivity());
        heraldService = null;
        stopForeground(true);
        stopSelf();
    }

    @Override
    // QUANDO L'APP VIENE CHIUSA
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d("ReactNative", "KILLING SERVICE FROM BACKGROUND");
        this.stop();
    }

    @Override
    public void onDestroy() {
        Log.d("ReactNative", "ON DESTROY");
        this.stop();
        super.onDestroy();

    }

    private void runClock(){
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int clock = 0;
                        while (true && !isKilled) {
                            Log.d("ReactNative", "[" + clock + "]");
                            clock++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
    }



    public void init(Context activity, String message) {


        this.myCallback = null;

        Log.d("ReactNative", "INT CIAO");
        // final PayloadDataSupplier payloadDataSupplier = new TestPayloadDataSupplier(identifier());

        // Initialise foreground service to keep application running in background
        // this.createNotificationChannel(activity);
        //NotificationService.shared(activity.getApplication()).startForegroundService(this.getForegroundNotification(activity), NOTIFICATION_ID);



        PayloadDataSupplier supplier = new PayloadDataSupplier() {
            @Nullable
            @Override
            public LegacyPayloadData legacyPayload(@NonNull PayloadTimestamp timestamp, @Nullable Device device) {
                return null;
            }

            @NonNull
            @Override
            public PayloadData payload(@NonNull PayloadTimestamp timestamp, @Nullable Device device) {
                byte[] id= message.getBytes();
                Log.d("MY MESSAGE:", message);
                PayloadData newPayload = new PayloadData(id);
                return newPayload;
            }

            @NonNull
            @Override
            public List<PayloadData> payload(@NonNull Data data) {
                return null;
            }


        };

        // final PayloadDataSupplier payloadDataSupplier = new TestPayloadDataSupplier(identifier());

        sensor = new SensorArray(activity, supplier);

        // Add appDelegate as listener for detection events for logging and start sensor
      //  sensor.add(this);


    }



    private void createNotificationChannel(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME, importance);

            //channel.setDescription(this.getString(R.string.notification_channel_description));

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getForegroundNotification(Activity activity) {
        final Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // FLAG_MUTABLE was the default prior to Android 12. Required to be explicitly set since 12 (SDK 31)
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentText("Ora sei visibile dagli altri utenti vicini a te")
                .setContentTitle("Sei online!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true);


        final Notification notification = builder.build();
        return notification;
    }



}
