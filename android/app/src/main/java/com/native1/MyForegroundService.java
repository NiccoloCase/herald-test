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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class MyForegroundService extends Service implements SensorDelegate {
    private MyForegroundService context;
    private boolean isKilled;
    private String message;

    private String tag = "PayloadExample";
    // Sensor for proximity detection
    private SensorArray sensor = null;

    private final static String NOTIFICATION_CHANNEL_ID = "HERALD_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private final static String NOTIFICATION_CHANNEL_NAME = "HERALD_NOTIFICATION_CHANNEL_NAME";

    MyCallback myCallback;

    private MessagesClient messagesClient;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        isKilled = false;



        Log.d("ReactNative", "StartedForeground");







        this.myCallback = null;
        this.message = "iao";

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

        sensor = new SensorArray(this, supplier);

        // Add appDelegate as listener for detection events for logging and start sensor
        sensor.add(this);


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

            // Avvia l'attività
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
        sensor.stop();
        isKilled = true;
        stopForeground(true);
        stopSelf();
    }

    @Override
    // QUANDO L'APP VIENE CHIUSA
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("ReactNative", "KILLING SERVICE FROM BACKGROUND");
        this.stop();
        super.onTaskRemoved(rootIntent);
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





    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        //   Log.i(tag, sensor.name() + ",didDetect=" + didDetect);

    }

    @Override
    public void sensor(@NonNull SensorType sensor, boolean available, @NonNull TargetIdentifier didDeleteOrDetect) {
     /*   String avail = "N";
        if (available) {
            avail = "Y";
        }
        Log.i(tag, sensor.name() + ",didDeleteOrDetect=" + didDeleteOrDetect +
                ",available=" + avail);*/
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);

        Log.d("DIO1", didRead.shortName());
        Log.d("DIO1", new String(didRead.value));


        if(myCallback!=null) myCallback.onFound(new String(didRead.value));



        // parsePayload("didRead", sensor, didRead, fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didReceive=" + didReceive.data.base64EncodedString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
        final List<String> payloads = new ArrayList<>(didShare.size());
        for (PayloadData payloadData : didShare) {
            //payloads.add(payloadData.shortName());
            Log.d("DIO3", payloadData.shortName());
            Log.d("DIO3", new String(payloadData.value));
        }
       /*
       Log.i(tag, sensor.name() + ",didShare=" + payloads.toString() + ",fromTarget=" + fromTarget);
        for (PayloadData payloadData : didShare) {
           // parsePayload("didShare", sensor, payloadData, fromTarget);
        }*/
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        //  Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Location didVisit) {
        // Log.i(tag, sensor.name() + ",didVisit=" + ((null == didVisit) ? "" : didVisit.description()));
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        //  Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget + ",withPayload=" + withPayload.shortName());
        Log.d("DIO2", withPayload.shortName());
        Log.d("DIO2", new String(withPayload.value));

        if(myCallback!=null) myCallback.onFound(new String(withPayload.value));
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        //    Log.i(tag, sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }


}
