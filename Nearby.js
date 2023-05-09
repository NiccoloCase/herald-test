import {NativeModules, Platform, NativeEventEmitter} from 'react-native';

const isAndroid = Platform.OS == 'android';

/**
 * Inizzia la pubblicazione (anche in background) e lo scanning dei messaggi
 * @param {string} message
 */
const start = message => {
  if (isAndroid) {
    NativeModules.MyNativeModule.start(message);
  }
};

/**
 * Interrompe l'attività di pubblicazione e scanning
 */
const stop = () => {
  if (isAndroid) NativeModules.MyNativeModule.stop();
};

/**
 * Verifica se il servizizio di pubblicazione/scanning è attivo
 * @returns Restituisce una promessa
 */
const isActive = () => {
  return new Promise((resolve, reject) => {
    if (isAndroid) {
      NativeModules.MyNativeModule.isActivityRunning(res => {
        resolve(res);
      });
    } else {
      resolve(false);
    }
  });
};

const registerToEvents = (
  onMessageFound,
  onMessageLost,
  onActivityStart,
  onActivityStop,
) => {
  const emitters = [];

  const eventEmitter = new NativeEventEmitter();
  emitters.push(
    eventEmitter.addListener('onMessageFound', onMessageFound),
    eventEmitter.addListener('onMessageLost', onMessageLost),
    eventEmitter.addListener('onActivityStart', onActivityStart),
    eventEmitter.addListener('onActivityStop', onActivityStop),
    eventEmitter.addListener('onPermissionsRejected', () => {
      alert('Permessi non concessi');
    }),
    eventEmitter.addListener('onGooglePlayServicesNotAvailable', () => {
      alert('onGooglePlayServicesNotAvailable');
    }),
  );

  return () => {
    emitters.forEach(emitter => emitter.remove());
  };
};

export default {
  start,
  stop,
  isActive,
  registerToEvents,
};
