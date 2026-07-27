package com.atena.agent;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "Hotword",
    permissions = {
        @Permission(alias = "microphone", strings = { Manifest.permission.RECORD_AUDIO })
    }
)
public class HotwordPlugin extends Plugin {

    private BroadcastReceiver hotwordReceiver;
    private boolean receiverRegistered = false;

    @Override
    public void load() {
        super.load();

        hotwordReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (HotwordService.ACTION_HOTWORD_DETECTED.equals(intent.getAction())) {
                    JSObject data = new JSObject();
                    data.put("keyword", intent.getStringExtra(HotwordService.EXTRA_KEYWORD));
                    data.put("transcript", intent.getStringExtra(HotwordService.EXTRA_TRANSCRIPT));
                    notifyListeners("hotwordDetected", data);
                } else if (HotwordService.ACTION_HOTWORD_ERROR.equals(intent.getAction())) {
                    JSObject data = new JSObject();
                    data.put("message", intent.getStringExtra(HotwordService.EXTRA_ERROR_MESSAGE));
                    data.put("code", intent.getIntExtra(HotwordService.EXTRA_ERROR_CODE, -1));
                    notifyListeners("hotwordError", data);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(HotwordService.ACTION_HOTWORD_DETECTED);
        filter.addAction(HotwordService.ACTION_HOTWORD_ERROR);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(hotwordReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(hotwordReceiver, filter);
        }
        receiverRegistered = true;
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (getPermissionState("microphone") != com.getcapacitor.PermissionState.GRANTED) {
            requestPermissionForAlias("microphone", call, "microphonePermCallback");
            return;
        }
        startListening(call);
    }

    @PermissionCallback
    private void microphonePermCallback(PluginCall call) {
        if (getPermissionState("microphone") == com.getcapacitor.PermissionState.GRANTED) {
            startListening(call);
        } else {
            call.reject("Permissão de microfone negada");
        }
    }

    private void startListening(PluginCall call) {
        String keyword = call.getString("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            call.reject("Parâmetro 'keyword' é obrigatório");
            return;
        }
        String modelAssetName = call.getString("modelAssetName"); // null = usa default do service
        String notifTitle = call.getString("notificationTitle", "Assistente ativo");
        String notifText = call.getString("notificationText", "Escutando a palavra de ativação");

        Intent serviceIntent = new Intent(getContext(), HotwordService.class);
        serviceIntent.putExtra(HotwordService.EXTRA_KEYWORD, keyword);
        if (modelAssetName != null) serviceIntent.putExtra(HotwordService.EXTRA_MODEL_ASSET_NAME, modelAssetName);
        serviceIntent.putExtra(HotwordService.EXTRA_NOTIF_TITLE, notifTitle);
        serviceIntent.putExtra(HotwordService.EXTRA_NOTIF_TEXT, notifText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
        } else {
            getContext().startService(serviceIntent);
        }

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent serviceIntent = new Intent(getContext(), HotwordService.class);
        getContext().stopService(serviceIntent);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void isListening(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("listening", HotwordService.isRunning());
        call.resolve(ret);
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (receiverRegistered) {
            getContext().unregisterReceiver(hotwordReceiver);
            receiverRegistered = false;
        }
    }
}
