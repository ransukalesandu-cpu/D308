package com.discipline309.app;

import android.content.Intent;
import android.service.voice.VoiceInteractionService;

public class MayaVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() {
        super.onReady();
        try {
            Intent i=new Intent(this,MayaAssistantService.class);
            if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        } catch(Exception ignored) {}
    }
    @Override public void onShutdown() {
        try { stopService(new Intent(this,MayaAssistantService.class)); } catch(Exception ignored) {}
        super.onShutdown();
    }
}