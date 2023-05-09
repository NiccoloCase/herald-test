import React, {useEffect, useState} from 'react';
import {
  Button,
  SafeAreaView,
  View,
  Text,
  ScrollView,
  NativeModules,
} from 'react-native';
import Nearby from './Nearby';

import {getDeviceName} from 'react-native-device-info';

const App = () => {
  const [deviceName, setDeviceName] = useState('devicename');
  const [devices, setDevices] = useState([]);
  const [isRunning, setIsRunning] = useState(false);
  const [startDate, setStartDate] = useState(null);

  useEffect(() => {
    Nearby.isActive().then(res => setIsRunning(res));
  }, []);

  useEffect(() => {
    getDeviceName().then(setDeviceName);
    // Eventi:
    const removeEvents = Nearby.registerToEvents(
      // MESSAGE FOUND
      event => {
        const diff = new Date().getTime() - (startDate || new Date()).getTime();

        console.log(
          `[${deviceName}]: messaggio ricevuto dal dispositivo "${event.message}"`,
        );
        setDevices(d => [...d, event.message + ' - ' + diff / 1000]);
      },
      // MESSAGE LOST
      event => {
        console.log(
          `[${deviceName}]: messaggio perso dal dispositivo "${event.message}"`,
        );
      },
      // ACTIVITY START
      () => setIsRunning(true),
      // ACTIVITY STOP
      () => setIsRunning(false),
    );

    return () => removeEvents();
  }, [startDate, deviceName]);

  function onPressStart() {
    Nearby.start(deviceName);
    setStartDate(new Date());
  }

  const onPressStop = () => {
    Nearby.stop();
    setDevices([]);
    setStartDate(null);
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
      <View style={{marginBottom: 60}}>
        <Button
          title={isRunning ? 'Stop' : 'Start'}
          onPress={isRunning ? onPressStop : onPressStart}
        />
      </View>
      <View style={{marginVertical: 10}}>
        <Button title="Clear logs" onPress={() => setDevices([])} />
      </View>
      <Button
        title="send (solo per test)"
        onPress={() => {
          NativeModules.MyNativeModule.send('active screen');
        }}
      />

      <ScrollView style={{marginTop: 60}}>
        {devices.map((x, i) => {
          return (
            <View key={i} style={{backgroundColor: 'blue', marginVertical: 10}}>
              <Text style={{fontSize: 30}}>{x}</Text>
            </View>
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
};

export default App;
