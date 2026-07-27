import { registerPlugin } from '@capacitor/core';
import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * @typedef {Object} HotwordStartOptions
 * @property {string} keyword - Palavra-chave a ser detectada, ex: "atena".
 * @property {string} [modelAssetName] - Nome da pasta do modelo Vosk em assets/ (padrão: "vosk-model-small-pt-0.3").
 * @property {string} [notificationTitle] - Título da notificação persistente (Android).
 * @property {string} [notificationText] - Texto da notificação persistente (Android).
 */

/**
 * @typedef {Object} HotwordDetectedEvent
 * @property {string} keyword
 * @property {string} transcript
 */

/**
 * @typedef {Object} HotwordErrorEvent
 * @property {string} message
 * @property {number} [code]
 */

/**
 * @typedef {Object} HotwordPlugin
 * @property {(options: HotwordStartOptions) => Promise<{success: boolean}>} start
 * @property {() => Promise<{success: boolean}>} stop
 * @property {() => Promise<{listening: boolean}>} isListening
 * @property {() => Promise<{microphone: 'granted'|'denied'|'prompt'}>} checkPermissions
 * @property {() => Promise<{microphone: 'granted'|'denied'|'prompt'}>} requestPermissions
 * @property {(eventName: string, listenerFunc: Function) => Promise<import('@capacitor/core').PluginListenerHandle>} addListener
 * @property {() => Promise<void>} removeAllListeners
 */

/** @type {HotwordPlugin} */
const Hotword = registerPlugin('Hotword', {
  web: () => import('./web.js').then((m) => new m.HotwordWeb()),
});

export { Hotword };

/**
 * Hook React que encapsula o plugin Hotword: registra os listeners uma
 * única vez e expõe estado reativo (listening/error) + start()/stop().
 *
 * @param {Object} options
 * @param {string} options.keyword
 * @param {string} [options.modelAssetName]
 * @param {string} [options.notificationTitle]
 * @param {string} [options.notificationText]
 * @param {(event: HotwordDetectedEvent) => void} [options.onDetected]
 */
export function useHotword(options) {
  const [listening, setListening] = useState(false);
  const [error, setError] = useState(null);

  // guarda a versão mais recente do callback sem precisar re-registrar
  // o listener nativo a cada render
  const onDetectedRef = useRef(options.onDetected);
  onDetectedRef.current = options.onDetected;

  useEffect(() => {
    const detectedHandle = Hotword.addListener('hotwordDetected', (data) => {
      onDetectedRef.current?.(data);
    });

    const errorHandle = Hotword.addListener('hotwordError', (data) => {
      setError(data.message);
    });

    return () => {
      detectedHandle.then((h) => h.remove());
      errorHandle.then((h) => h.remove());
    };
  }, []);

  const start = useCallback(async () => {
    setError(null);

    const perm = await Hotword.requestPermissions();
    if (perm.microphone !== 'granted') {
      setError('Permissão de microfone negada');
      return false;
    }

    await Hotword.start({
      keyword: options.keyword,
      modelAssetName: options.modelAssetName,
      notificationTitle: options.notificationTitle,
      notificationText: options.notificationText,
    });

    setListening(true);
    return true;
  }, [
    options.keyword,
    options.modelAssetName,
    options.notificationTitle,
    options.notificationText,
  ]);

  const stop = useCallback(async () => {
    await Hotword.stop();
    setListening(false);
  }, []);

  return { listening, error, start, stop };
}
