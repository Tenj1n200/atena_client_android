package com.atena.agent;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

import com.atena.agent.HotwordPlugin;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TermuxExecPlugin.class);
        registerPlugin(SpeechPlugin.class);
        registerPlugin(HotwordPlugin.class);

        super.onCreate(savedInstanceState);
    }
}