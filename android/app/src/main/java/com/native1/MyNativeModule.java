package com.native1;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.app.Activity;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.common.GoogleApiAvailability;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import io.heraldprox.herald.sensor.service.ForegroundService;
import io.heraldprox.herald.sensor.service.NotificationService;

interface MyCallback {
    void onFound(String message);
    void onStop();
}


public  class MyNativeModule extends ReactContextBaseJavaModule  implements PermissionListener, MyCallback {
    private static ReactApplicationContext reactContext;
    // Se il servizio è attivo
    private boolean isRunning = false;
    // Gestione errori
    private boolean connectionsFailed= false;
    private boolean heraldFailed = false;

    // Messaggi trovati
    private ArrayList<String> foundMessages = new ArrayList<String>();
    // Messaggio da inviare
    private String message;

    // NEARBY CONNECTIONS
    private ConnectionsClient mConnectionsClient;
    // HERALD
    HeraldService heraldService;
    // PERMESSI
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            // Herald:
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.FOREGROUND_SERVICE,

                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            // Herald:
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.FOREGROUND_SERVICE
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            // Herald:
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.FOREGROUND_SERVICE
                    };
        }
    }

    MyNativeModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
    }

    @Override
    public String getName() {
        return "MyNativeModule";
    }

    // REACT-NATIVE: AVVIA
    @ReactMethod
    public void start(String message) {
        Activity context = getCurrentActivity();
        // imposta messaggio
        this.message = message;

        // Controlla se ci sono i play services TODO

        // Controlla se ha i permessi
        if (!hasPermissions(context, getRequiredPermissions())) {
            PermissionAwareActivity permissionActivity = (PermissionAwareActivity) getCurrentActivity();
            permissionActivity.requestPermissions( getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS,this);

            Log.d("ReactNative","PERMISSION NOT GRANTED");
            emitMessageEvent("onPermissionsRejected", "Permissions rejected");

        } else {
            Log.d("ReactNative","PERMISSION GRANTED");
            startAll();
        }
    }

    // REACT-NATIVE: STOP
    @ReactMethod
    public void stop() {
        Log.d("ReactNative","STOP");
        // Herald
        if(heraldService != null) {
            heraldService.stop();
            heraldService = null;
        }
        // Nearby
        stop_connections();
        // Distrugge notifica foreground service
        NotificationService.shared(getCurrentActivity().getApplication()).stopForegroundService();
        this.onStop();

    }

    // REACT-NATIVE: se sono stati garantiti i permessi
    @ReactMethod
    public void arePermissionsGranted(Callback callBack) {
        Boolean res = hasPermissions(getCurrentActivity(), getRequiredPermissions());
        callBack.invoke(res);
    }

    // REACT-NATIVE: SE IL SERVIZIO E' ATTIVO
    @ReactMethod
    public void isActivityRunning(Callback callBack) {
        Boolean running = isServiceRunning();
        callBack.invoke(running);
    }


    // AVVIA TUTTO
    private void startAll(){
        if(this.message == null) return;
        isRunning = true;
        // Invia evento react-native
        emitMessageEvent("onActivityStart","started");
        // Herald
        heraldService = new HeraldService();
        heraldService.init(reactContext.getCurrentActivity(), message,this);
        heraldService.start();
        // nearby connections
        startAdvertising_connections(this.message);
        startDiscovering_connections();
    }


    // FERMA NEARBY CONNECTIONS
    void stop_connections(){
        if(mConnectionsClient != null) {
            mConnectionsClient.stopAdvertising();
            mConnectionsClient.stopDiscovery();
        }
    }

    // AVVIA RICERCA CON NEARBY CONNECTIONS
     void startDiscovering_connections() {
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(Strategy.P2P_CLUSTER);
        mConnectionsClient
                .startDiscovery(
                        getConnectionsServiceId(),
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                Log.d("ReactNative","Found - connections " + info.getEndpointName());
                                if (getConnectionsServiceId().equals(info.getServiceId())) {
                                    onFound(info.getEndpointName() + "- connections");
                                }
                            }
                            @Override
                            public void onEndpointLost(String endpointId) {
                               Log.d("ReactNative","Lost - Connections");
                            }
                        },
                        discoveryOptions.build()).addOnSuccessListener(
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void a) {
                                    Log.d("ReactNative","connections startDiscovering() success.");
                                }
                            })
                        .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                   Log.d("ReactNative","startDiscovering() failed.");
                                }
                        });
    }


    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d("ReactNative","onConnectionInitiated(endpointId=" + endpointId + ", endpointName=" + connectionInfo.getEndpointName() + ")");

                }
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {

                    Log.d("ReactNative","onConnectionResult");
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d("ReactNative","Disconnected from endpoint " + endpointId);
                }
            };


    // RESTITUISCE L'ID DEL SERVIZIO DI NEARBY CONNECTIONS
    private String getConnectionsServiceId(){
        return "SPOTLIVE";
    }

    // AVVIA ADVERTISING CON NEARBY CONNECTIONS
    private void startAdvertising_connections(String message) {
        Context context = getCurrentActivity();
        mConnectionsClient = Nearby.getConnectionsClient(context);

        final String localEndpointName = message;

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(Strategy.P2P_CLUSTER);

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        getConnectionsServiceId(),
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d("ReactNative","Now advertising endpoint " + localEndpointName);
                                isRunning = true;
                                emitMessageEvent("onAdvertisingStarted","Advertising started");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // mIsAdvertising = false;
                                Log.d("ReactNative","startAdvertising() failed.", e);
                                // onAdvertisingFailed();

                            }
                        });
    }


    // QUANDO VENGONO AGGIORNATI I PERMESSI
    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("ReactNative","onRequestPermissionsResult");
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            int i = 0;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Log.d("ReactNative","PERMISSION_DENIED: " + permissions[i]);
                    emitMessageEvent("onPermissionsRejected", "Permissions rejected");
                    return false;
                }
                i++;
            }

        }

        // Controlla se il GPS è attivo
        boolean gpsStatus = checkGPSStatus();
        Log.d("ReactNative", "GPS STATUS: " + gpsStatus);
        if(gpsStatus == false) {
            Log.d("Permissions", "MISSING GPS");
            emitMessageEvent("gpsOff", "gpsOff");
            return false;
        }

        Log.d("ReactNative","PERMISSION_SUCCESS");
        startAll();

        return true;
    }

    // VERIFICA SE SONO STATI CONCESSI I PERMESSI NECESSARI
    public boolean hasPermissions(Context context, String... permissions) {
        // Controlla se sono stati concessi i permessi necessari
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("ReactNative", "MANCA IL PERMESSO: " + permission);
                return false;
            }
        }
        // Controlla se il GPS è attivo
        boolean gpsStatus = checkGPSStatus();
        Log.d("ReactNative", "GPS STATUS: " + gpsStatus);
        if(gpsStatus == false) {
            Log.d("Permissions", "MISSING GPS");
            emitMessageEvent("gpsOff", "gpsOff");
            return false;
        }

        return true;
    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    // CALLBACK
    @Override
    public void onFound(String messageText) {
        if(!isRunning) return;
        // Controlla se il messaggio è già stato trovato
        if(foundMessages.contains(messageText)) return;
        // Aggiunge il messaggio all'array
        foundMessages.add(messageText);
        // Invia messaggio a react-native
        emitMessageEvent("onMessageFound", messageText);
    }

    @Override
    public void onStop() {
        isRunning = false;
        emitMessageEvent("onActivityStop", "stopped");
        // Ferma connections (necessario se l'app viene chiusa dalla notifica)
        stop_connections();
        // Svuota l'array dei messaggi trovati
        foundMessages.clear();

        // Gestione errori
        heraldFailed = false;
        connectionsFailed = false;
    }


    // SE SONO DISPONIBILI I PLAY SERVICES
    private boolean isGooglePlayServicesAvailable() {
        Activity currentActivity = getCurrentActivity();

        final GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        final int availability = googleApi.isGooglePlayServicesAvailable(currentActivity);
        final boolean result = availability == ConnectionResult.SUCCESS;
        if (!result && googleApi.isUserResolvableError(availability)) {
            googleApi.getErrorDialog(currentActivity, availability, 9000).show();
        }
        return result;
    }

    // INVIA MESSAGGIO A REACT-NATIVE
    private void emitMessageEvent(String eventName, String message) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, message);
    }

    // SE IL SERVIZIO E' ATTIVO
    private boolean isServiceRunning() {
        Activity context = getCurrentActivity();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && context!=null) {
            ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                Log.d("ReactNative","Service name: "+ service.service.getClassName());
                Log.d("ReactNative","Active service is in foreground: "+ service.foreground);
                if (ForegroundService.class.getName().equals(service.service.getClassName()) ||
                        MainActivity.class.getName().equals(service.service.getClassName())) {
                        return service.foreground;
                }
            }
            return false;
        }
        return false;
    }


    // OTTIENE LO STATUS DEL GPS
    public static boolean checkGPSStatus(){
        Context context = reactContext.getCurrentActivity();
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}