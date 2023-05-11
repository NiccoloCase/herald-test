package com.native1;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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


public class HeraldService implements SensorDelegate {
    String tag = "PayloadExample";
    // Sensor for proximity detection
    public static SensorArray sensor = null;

    private final static String NOTIFICATION_CHANNEL_ID = "HERALD_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private final static String NOTIFICATION_CHANNEL_NAME = "HERALD_NOTIFICATION_CHANNEL_NAME";

    private static MyCallback myCallback;


    public void init(Activity activity, String message, MyCallback myCallback) {
        this.myCallback = myCallback;

        Log.d("ReactNative", "INT HERALD SERVICE");

        // Initialise foreground service to keep application running in background
        this.createNotificationChannel(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationService.shared(activity.getApplication()).startForegroundService(this.getForegroundNotification(activity), NOTIFICATION_ID);
        }

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
        sensor.add(this);


    }

    public void start() {

        sensor.start();

        sensor.immediateSendAll(new Data("Hello World".getBytes()));
    }

    public static void stop() {
        sensor.stop();
        sensor = null;
        myCallback.onStop();
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

        Intent stopIntent = new Intent(activity, StopServiceReceiver.class);
        stopIntent.setAction("com.example.app.STOP_ACTION");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(activity, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);




        final Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // FLAG_MUTABLE was the default prior to Android 12. Required to be explicitly set since 12 (SDK 31)
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
              //  .setContentText("Ora sei visibile dagli altri utenti vicini a te")
                .setContentTitle("Sei online!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true).addAction(R.mipmap.ic_launcher_round, "FERMA", stopPendingIntent);




        final Notification notification = builder.build();
        return notification;
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
        }*/
        Log.i(tag, sensor.name() + ",didDeleteOrDetect=" + didDeleteOrDetect +
                ",available=" +available + "value:"+ new String(didDeleteOrDetect.value));


    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);

        Log.d("DIO1", didRead.shortName());
        Log.d("DIO1", new String(didRead.value));


        if(myCallback!=null) myCallback.onFound(new String(didRead.value) + "- herald1");



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

        if(myCallback!=null) myCallback.onFound(new String(withPayload.value)+ "- herald2");
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        Log.i(tag, sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }
}