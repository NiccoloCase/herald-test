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

    private final String tag = "ReactNative"; //HeraldService.class.getName();
    public static SensorArray sensor = null;

    private final static String NOTIFICATION_CHANNEL_ID = "HERALD_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private final static String NOTIFICATION_CHANNEL_NAME = "Background activity";
    private static MyCallback myCallback;


    public void init(Activity activity, String message, MyCallback myCallback) {
        this.myCallback = myCallback;
        Log.d("ReactNative", "INT HERALD SERVICE");

        // Foreground activity
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

        sensor = new SensorArray(activity, supplier);
        sensor.add(this);
    }

    public void start() {
        sensor.start();
    }

    public static void stop() {
        sensor.stop();
        sensor = null;
        myCallback.onStop();
    }


    // CREA IL CANALE DI NOTIFICA
    private void createNotificationChannel(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                   NOTIFICATION_CHANNEL_NAME, importance);

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // CREA LA NOTIFICA PER IL FOREGROUND SERVICE
    private Notification getForegroundNotification(Activity activity) {
        // Bottone FERMA
        Intent stopIntent = new Intent(activity, StopServiceReceiver.class);
        stopIntent.setAction("com.example.app.STOP_ACTION");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(activity, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        //
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
                .setUsesChronometer(true).addAction(R.mipmap.ic_launcher_round, "FERMA", stopPendingIntent);


        final Notification notification = builder.build();
        return notification;
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        Log.i(tag, sensor.name() + ",didDetect=" + didDetect + "value: " + didDetect.value);

    }

    @Override
    public void sensor(@NonNull SensorType sensor, boolean available, @NonNull TargetIdentifier didDeleteOrDetect) {
        Log.i(tag, sensor.name() + ",didDeleteOrDetect=" + didDeleteOrDetect +
                ",available=" +available + "value:"+ new String(didDeleteOrDetect.value));
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);

        Log.d("HERALD1", new String(didRead.value));

        if(myCallback!=null) myCallback.onFound(new String(didRead.value) + "- herald1");
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didReceive=" + didReceive.data.base64EncodedString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
       final List<String> payloads = new ArrayList<>(didShare.size());
        for (PayloadData payloadData : didShare) {
            Log.d("HERALD3", new String(payloadData.value));
        }
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
        Log.d("HERALD2", new String(withPayload.value));
        if(myCallback!=null) myCallback.onFound(new String(withPayload.value)+ "- herald2");
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        Log.i(tag, sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }
}