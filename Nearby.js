import {NativeModules, Platform, NativeEventEmitter} from 'react-native';

const ID_LENGTH = 24;
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

/**
 *
 */
const arePermissionsGranted = () => {
  if (isAndroid) {
    return new Promise((resolve, reject) => {
      NativeModules.MyNativeModule.arePermissionsGranted(res => {
        resolve(res);
      });
    });
  } else {
    return Promise.resolve(true);
  }
};

/**
 * Restituisce l'ID dell'utente contenuto in un messaggio
 * Message template:
 * SPL::5fdedb7c25ab1352eef88f60
 * @param {String} message
 * @returns
 */
export const getIdFromMessage = message => {
  const splitted = message.split('::');
  if (splitted.length != 2) return null;
  const protocol = splitted[0];
  const id = splitted[1];
  if (protocol != 'SPL') return null;
  if (!id) return null;
  if (id.length != ID_LENGTH) return null;
  else return id;
};

/**
 * Generea un messaggui da un ID
 * @param {*} id
 * @returns
 */
export const getMessageFromId = id => {
  if (!id) return null;
  if (id.length != ID_LENGTH) return null;

  return `SPL::${id}`;
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
    eventEmitter.addListener('gpsOff', () => {
      alert('gpsOff');
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
  arePermissionsGranted,
};
