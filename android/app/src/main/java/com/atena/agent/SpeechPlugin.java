package com.atena.agent;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;

@CapacitorPlugin(name = "NativeSpeech")
public class SpeechPlugin extends Plugin {

    private static final String TAG = "NativeSpeech";

    @PluginMethod
    public void listen(PluginCall call) {

        Log.d(TAG, "Iniciando reconhecimento");

        Intent intent = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "pt-BR"
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Fale agora..."
        );

        startActivityForResult(
            call,
            intent,
            "handleSpeechResult"
        );
    }

    @ActivityCallback
    private void handleSpeechResult(
        PluginCall call,
        ActivityResult result
    ) {

        Log.d(TAG, "Callback executado");

        if (call == null) {
            Log.e(TAG, "PluginCall nulo");
            return;
        }

        if (result == null) {
            call.reject("Resultado nulo");
            return;
        }

        int resultCode = result.getResultCode();

        Intent data = result.getData();

        Log.d(
            TAG,
            "ResultCode: " + resultCode
        );

        if (resultCode != -1) {
            call.reject(
                "Reconhecimento cancelado"
            );
            return;
        }

        if (data == null) {
            call.reject(
                "Nenhum dado retornado"
            );
            return;
        }

        ArrayList<String> results =
            data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            );

        if (
            results == null ||
            results.isEmpty()
        ) {
            call.reject(
                "Nenhum texto reconhecido"
            );
            return;
        }

        String text = results.get(0);

        Log.d(
            TAG,
            "Texto reconhecido: " + text
        );

        JSObject response =
            new JSObject();

        response.put(
            "text",
            text
        );

        call.resolve(response);
    }
}