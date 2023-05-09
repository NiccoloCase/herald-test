import React, {useEffect, useState} from 'react';
import {
  Button,
  NativeModules,
  SafeAreaView,
  View,
  PermissionsAndroid,
  NativeEventEmitter,
  Text,
  ScrollView,
} from 'react-native';
import Nearby from './Nearby';

import {getDeviceName} from 'react-native-device-info';

const App = () => {
  const [deviceName, setDeviceName] = useState('');
  const [devices, setDevices] = useState([]);
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    NativeModules.MyNativeModule.isActivityRunning(res => {
      setIsRunning(res);
    });
  }, []);

  useEffect(() => {
    getDeviceName().then(setDeviceName);

    const emitters = [];

    const eventEmitter = new NativeEventEmitter();
    emitters.push(
      eventEmitter.addListener('onMessageFound', event => {
        console.log(
          `[${deviceName}]: messaggio ricevuto dal dispositivo "${event.message}"`,
        );
        setDevices(d => [...d, event.message]);
      }),
      eventEmitter.addListener('onMessageLost', event => {
        console.log(
          `[${deviceName}]: messaggio perso dal dispositivo "${event.message}"`,
        );
      }),
      eventEmitter.addListener('onActivityStart', () => setIsRunning(true)),
      eventEmitter.addListener('onActivityStop', () => setIsRunning(false)),
    );

    return () => {
      emitters.forEach(emitter => emitter.remove());
    };
  }, []);

  function onPressScan() {
    PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
      PermissionsAndroid.PERMISSIONS.ACCESS_BACKGROUND_LOCATION,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
      PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES,
      PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
    ]).then(result => {
      const isGranted =
        result['android.permission.BLUETOOTH_CONNECT'] ===
          PermissionsAndroid.RESULTS.GRANTED &&
        result['android.permission.BLUETOOTH_SCAN'] ===
          PermissionsAndroid.RESULTS.GRANTED &&
        result['android.permission.ACCESS_FINE_LOCATION'] ===
          PermissionsAndroid.RESULTS.GRANTED;

      console.log({isGranted});

      NativeModules.MyNativeModule.initNearby(isPlayServicesAvailable => {
        console.log({isPlayServicesAvailable});

        if (isPlayServicesAvailable) {
          NativeModules.MyNativeModule.start();
          console.log('started');
        }
      });
    });
  }

  const onPressStop = () => {
    NativeModules.MyNativeModule.stop();
  };

  const onPressActivity = () => {
    Nearby.start(deviceName);
  };

  return (
    <SafeAreaView style={{flex: 1}}>
      <Text
        style={{
          fontSize: 30,
          backgroundColor: 'green',
          margin: 20,
          padding: 10,
        }}>
        Nome del dispositivo: {deviceName}
      </Text>
      <View style={{marginVertical: 10}}>
        <Button title="Start Scanning" onPress={onPressScan} />
      </View>
      <View style={{marginVertical: 10}}>
        <Button title="Stop" onPress={onPressStop} />
      </View>
      <View style={{marginVertical: 10}}>
        <Button title="Start Activity" onPress={onPressActivity} />
      </View>
      <Text>Running: {isRunning ? 'TRUE' : 'FALSE'}</Text>
      <ScrollView style={{marginTop: 60}}>
        {devices.map(x => {
          return (
            <View style={{backgroundColor: 'blue', marginVertical: 10}}>
              <Text style={{fontSize: 30}}>{x}</Text>
            </View>
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
};

export default App;
