package com.atena.agent;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;

@CapacitorPlugin(name = "TermuxExec")
public class TermuxExecPlugin extends Plugin {

    private static final String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";
    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_SERVICE = "com.termux.app.RunCommandService";
    private static final String RESULT_ACTION = "com.atena.agent.TERMUX_RESULT";

    @PluginMethod
    public void execute(PluginCall call) {
        String executable = call.getString("executable", "/data/data/com.termux/files/usr/bin/bash");
        ArrayList<String> args = new ArrayList<>();

        String command = call.getString("command");
        if (command == null) {
            call.reject("Parametro 'command' é obrigatório");
            return;
        }
        args.add("-c");
        args.add(command);

        String workdir = call.getString("workdir", "/data/data/com.termux/files/home");
        boolean background = call.getBoolean("background", true);

        Context context = getContext();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                Bundle result = intent.getBundleExtra("result");
                JSObject ret = new JSObject();

                if (result != null) {
                    ret.put("stdout", result.getString("stdout", ""));
                    ret.put("stderr", result.getString("stderr", ""));
                    ret.put("exitCode", result.getInt("exitCode", -1));
                    ret.put("errmsg", result.getString("errmsg", ""));
                } else {
                    ret.put("stdout", "");
                    ret.put("stderr", "Sem resultado retornado pelo Termux");
                    ret.put("exitCode", -1);
                }

                call.resolve(ret);
                try {
                    context.unregisterReceiver(this);
                } catch (IllegalArgumentException ignored) {}
            }
        };

        IntentFilter filter = new IntentFilter(RESULT_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }

        Intent resultIntent = new Intent(RESULT_ACTION);
        resultIntent.setPackage(context.getPackageName());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, (int) System.currentTimeMillis(), resultIntent, flags
        );

        Intent execIntent = new Intent();
        execIntent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
        execIntent.setAction(ACTION_RUN_COMMAND);
        execIntent.putExtra("com.termux.RUN_COMMAND_PATH", executable);
        execIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toArray(new String[0]));
        execIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", workdir);
        execIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", background);
        execIntent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent);

        try {
            context.startForegroundService(execIntent);
        } catch (Exception e) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {}
            call.reject("Falha ao chamar o Termux. Ele está instalado e com 'allow-external-apps=true'? " + e.getMessage());
        }
    }
}