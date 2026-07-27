package com.atena.agent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Service em foreground que faz reconhecimento contínuo e 100% local com
 * Vosk. Não manda áudio pra servidores externos, não precisa de internet
 * nem de conta/API key.
 *
 * Pré-requisito: um modelo Vosk (pasta descompactada, ex. "vosk-model-small-pt-0.3")
 * precisa estar em android/app/src/main/assets/ com esse mesmo nome, pois
 * é carregado via StorageService.unpack().
 */
public class HotwordService extends Service {

    private static final String TAG = "HotwordService";

    public static final String ACTION_HOTWORD_DETECTED = "com.exemplo.hotword.HOTWORD_DETECTED";
    public static final String ACTION_HOTWORD_ERROR = "com.exemplo.hotword.HOTWORD_ERROR";

    public static final String EXTRA_KEYWORD = "keyword";
    public static final String EXTRA_MODEL_ASSET_NAME = "modelAssetName";
    public static final String EXTRA_TRANSCRIPT = "transcript";
    public static final String EXTRA_ERROR_MESSAGE = "errorMessage";
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_NOTIF_TITLE = "notifTitle";
    public static final String EXTRA_NOTIF_TEXT = "notifText";

    private static final String CHANNEL_ID = "hotword_service_channel";
    private static final int NOTIFICATION_ID = 4821;
    private static final float SAMPLE_RATE = 16000.0f;

    private static volatile boolean running = false;

    private Model model;
    private SpeechService speechService;
    private String keyword;
    // Nome da pasta do modelo dentro de assets/ (sem barra no final)
    private String modelAssetName = "vosk-model-small-pt-0.3";

    public static boolean isRunning() {
        return running;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        keyword = normalize(intent.getStringExtra(EXTRA_KEYWORD));

        String modelExtra = intent.getStringExtra(EXTRA_MODEL_ASSET_NAME);
        if (modelExtra != null) modelAssetName = modelExtra;

        String notifTitle = intent.getStringExtra(EXTRA_NOTIF_TITLE);
        String notifText = intent.getStringExtra(EXTRA_NOTIF_TEXT);

        startForegroundWithNotification(
            notifTitle != null ? notifTitle : "Assistente ativo",
            notifText != null ? notifText : "Escutando a palavra de ativação"
        );

        running = true;

        // Descompacta/carrega o modelo de forma assíncrona (é uma operação
        // de I/O, não pode travar a main thread).
        StorageService.unpack(
            this,
            modelAssetName,
            "model",
            (unpackedModel) -> {
                model = unpackedModel;
                startRecognition();
            },
            (exception) -> {
                Log.e(TAG, "Falha ao carregar modelo Vosk", exception);
                broadcastError("Falha ao carregar modelo de voz: " + exception.getMessage(), -1);
                stopSelf();
            }
        );

        return START_STICKY;
    }

    private void startRecognition() {
        try {
            Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
            speechService = new SpeechService(recognizer, SAMPLE_RATE);
            speechService.startListening(recognitionListener);
        } catch (IOException e) {
            Log.e(TAG, "Falha ao iniciar reconhecimento", e);
            broadcastError("Falha ao iniciar reconhecimento: " + e.getMessage(), -1);
            stopSelf();
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onPartialResult(String hypothesis) {
            checkForKeyword(hypothesis, "partial");
        }

        @Override
        public void onResult(String hypothesis) {
            checkForKeyword(hypothesis, "text");
        }

        @Override
        public void onFinalResult(String hypothesis) {
            checkForKeyword(hypothesis, "text");
        }

        @Override
        public void onError(Exception exception) {
            Log.e(TAG, "Erro no reconhecimento Vosk", exception);
            broadcastError(exception.getMessage(), -1);
        }

        @Override
        public void onTimeout() {
            // Escuta contínua: reinicia automaticamente.
            if (speechService != null) {
                speechService.startListening(recognitionListener);
            }
        }
    };

    private void checkForKeyword(String hypothesisJson, String field) {
        if (hypothesisJson == null) return;
        try {
            JSONObject json = new JSONObject(hypothesisJson);
            String text = json.optString(field, "");
            if (text.isEmpty()) return;

            if (normalize(text).contains(keyword)) {
                broadcastHotwordDetected(text);
            }
        } catch (JSONException e) {
            Log.w(TAG, "Não foi possível parsear resultado do Vosk", e);
        }
    }

    private String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.forLanguageTag("pt-BR")).trim();
        String normalizedForm = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalizedForm.replaceAll("\\p{M}", "");
    }

    private void broadcastHotwordDetected(String transcript) {
        Intent intent = new Intent(ACTION_HOTWORD_DETECTED);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_KEYWORD, keyword);
        intent.putExtra(EXTRA_TRANSCRIPT, transcript);
        sendBroadcast(intent);
    }

    private void broadcastError(String message, int code) {
        Intent intent = new Intent(ACTION_HOTWORD_ERROR);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_ERROR_MESSAGE, message);
        intent.putExtra(EXTRA_ERROR_CODE, code);
        sendBroadcast(intent);
    }

    private void startForegroundWithNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Detecção de palavra de ativação",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
        super.onDestroy();
    }
}
