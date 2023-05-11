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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.BuildCompat;
import java.util.ArrayList;
import io.heraldprox.herald.sensor.service.ForegroundService;
import io.heraldprox.herald.sensor.service.NotificationService;

interface MyCallback {
    void onFound(String message);
    void onStop();
}


public  class MyNativeModule extends ReactContextBaseJavaModule  implements PermissionListener,MyCallback {
    private static ReactApplicationContext reactContext;
    private boolean isRunning = false;

    // Messaggi trovati
    private ArrayList<String> foundMessages = new ArrayList<String>();

    // NEARBY CONNECTIONS
    private ConnectionsClient mConnectionsClient;
    // HERALD
    HeraldService heraldService;
    // PERMESSI
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_CODE = 100;
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
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
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

        // Controlla se ci sono i play services TODO

        // Controlla se ha i permessi
        if (!hasPermissions(context, getRequiredPermissions())) {
            if (Build.VERSION.SDK_INT < 23) {
                ActivityCompat.requestPermissions(
                       context, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            } else {
                context.requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            }

            Log.d("ReactNative","PERMISSION NOT GRANTED");

        } else {
            Log.d("ReactNative","PERMISSION GRANTED");
            isRunning = true;
            // Invia evento react-native
            emitMessageEvent("onActivityStart","started");
            // Herald
            heraldService = new HeraldService();
            heraldService.init(reactContext.getCurrentActivity(), message,this);
            heraldService.start();
            // nearby connections
            startAdvertising_connections(message);
            startDiscovering_connections();


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
                                // onAdvertisingStarted();
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








    @Override
    public boolean onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            int i = 0;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                Log.d("ReactNative","PERMISSION_DENIED");
                    return false;
                }
                i++;
            }
            //recreate();
        }

        Log.d("ReactNative","PERMISSION_SUCCESS");


        return true;
    }




    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("ReactNative","Mi manca il permesso: " + permission);
                return false;
            }
        }
        return true;
    }



    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }



    // REACT-NATIVE: SE IL SERVIZIO E' ATTIVO
    @ReactMethod
    public void isActivityRunning(Callback callBack) {
        Boolean running = isServiceRunning();
        callBack.invoke(running);
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
    // OTTIENE LO STATUS DEI PERESSI
    private Boolean checkPermissions() {
        PermissionAwareActivity activity = (PermissionAwareActivity) getCurrentActivity();
        if (activity == null) return false;

        ArrayList<String> missingPermissionsList = new ArrayList<String>();

        // ACCESS_FINE_LOCATION
        if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.d("ReactNative", "Missing ACCESS_FINE_LOCATION");
        }
        // Android >= 12
        if (BuildCompat.isAtLeastS()) {
            // BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissionsList.add(Manifest.permission.BLUETOOTH_CONNECT);
                Log.d("ReactNative", "Missing BLUETOOTH_CONNECT");
            }
            // BLUETOOTH_SCAN
            if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissionsList.add(Manifest.permission.BLUETOOTH_SCAN);
                Log.d("ReactNative", "Missing BLUETOOTH_SCAN");
            }
            // BLUETOOTH_ADVERTISE
            if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                missingPermissionsList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                Log.d("ReactNative", "Missing BLUETOOTH_ADVERTISE");
            }
        }
        // Android < 12
        else {
            Log.d("ReactNative", "Android < 12");
            // BLUETOOTH
            if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                missingPermissionsList.add(Manifest.permission.BLUETOOTH);
                Log.d("ReactNative", "Missing BLUETOOTH");
            }
            // BLUETOOTH_ADMIN
            if (ActivityCompat.checkSelfPermission(reactContext.getCurrentActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissionsList.add(Manifest.permission.BLUETOOTH_ADMIN);
                Log.d("ReactNative", "Missing BLUETOOTH_ADMIN");
            }
        }

        int size = missingPermissionsList.size();
        if(size == 0 ) {
            // Controlla se il GPS è attivo
            boolean gpsStatus = checkGPSStatus();
            if(gpsStatus == false) {
                Log.d("Permissions", "MISSING GPS");

                emitMessageEvent("gpsOff",  "MISSING GPS");
                //  Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //   getCurrentActivity().startActivity(intent);
                return false;
            }
            return true;
        }

        String[] permissions = new String[size];
        permissions = missingPermissionsList.toArray(permissions);
        // Richiede i permessi
        activity.requestPermissions( permissions, MY_PERMISSIONS_REQUEST_CODE,this);

        return false;
    }
/*
    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_CODE) {
            Boolean success = true;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ReactNative", "Permission granted: " + permissions[i]);
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Log.d("ReactNative", "Permission NOT granted: " + permissions[i]);
                    success = false;
                }
            }
            if(success) {
                // START
                //startAll();
            } else {
                emitMessageEvent("onPermissionsRejected", "Permissions rejected");
            }
        }

        return true;
    }*/
}