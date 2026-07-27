import React, { useState } from 'react';
import { useHotword } from '../plugins/Hotword';

export function HotwordButton() {
  const [activatedMessage, setActivatedMessage] = useState(null);

  const { listening, error, heardText, start, stop } = useHotword({
    keyword: 'atena', 
    modelAssetName: 'vosk-model-small-pt-0.3',
    notificationTitle: 'Atena ativa',
    notificationText: 'Diga "Atena" para chamar o assistente',
    onDetected: (data) => {
      setActivatedMessage(`🎉 Palavra de ativação detectada: "${data.transcript}"`);
      setTimeout(() => setActivatedMessage(null), 2000);

    },
  });

  return (
    <div>
      <button onClick={listening ? stop : start}>
        {listening ? 'Desativar Atena' : 'Ativar Atena'}
      </button>

      <p>Status: {listening ? 'escutando...' : 'parado'}</p>

      {listening && (
        <p style={{ color: '#666' }}>
          Ouvindo: "{heardText || '...'}"
        </p>
      )}

      {activatedMessage && (
        <p style={{ color: 'green', fontWeight: 'bold' }}>
          {activatedMessage}
        </p>
      )}

      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}
    </div>
  );
}