import { WebPlugin } from '@capacitor/core';

export class HotwordWeb extends WebPlugin {
  constructor() {
    super();
    this.recognition = null;
    this.keyword = '';
    this.listening = false;
    this.shouldRestart = false;
  }

  async start(options) {
    const SpeechRecognitionCtor =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognitionCtor) {
      this.notifyListeners('hotwordError', {
        message: 'Web Speech API não suportada neste navegador.',
      });
      return { success: false };
    }

    this.keyword = this.normalize(options.keyword);
    this.shouldRestart = true;

    this.recognition = new SpeechRecognitionCtor();
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    this.recognition.lang = options.language ?? 'pt-BR';

    this.recognition.onresult = (event) => {
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (this.normalize(transcript).includes(this.keyword)) {
          this.notifyListeners('hotwordDetected', {
            keyword: options.keyword,
            transcript,
          });
        }
      }
    };

    this.recognition.onerror = (event) => {
      this.notifyListeners('hotwordError', { message: event.error });
    };

    // O reconhecimento do navegador para sozinho após um tempo; reinicia automaticamente.
    this.recognition.onend = () => {
      if (this.shouldRestart) {
        this.recognition.start();
      }
    };

    this.recognition.start();
    this.listening = true;
    return { success: true };
  }

  async stop() {
    this.shouldRestart = false;
    this.listening = false;
    this.recognition?.stop();
    return { success: true };
  }

  async isListening() {
    return { listening: this.listening };
  }

  async checkPermissions() {
    return { microphone: 'prompt' };
  }

  async requestPermissions() {
    try {
      await navigator.mediaDevices.getUserMedia({ audio: true });
      return { microphone: 'granted' };
    } catch {
      return { microphone: 'denied' };
    }
  }

  normalize(text) {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, ''); // remove acentos
  }
}
