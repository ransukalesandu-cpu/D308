package com.discipline309.app;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class MayaVoiceInteractionSession extends VoiceInteractionSession {
    public MayaVoiceInteractionSession(android.content.Context context) { super(context); }
    @Override public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        try {
            Intent i=new Intent(getContext(),MayaAssistantService.class);
            if(android.os.Build.VERSION.SDK_INT>=26) getContext().startForegroundService(i); else getContext().startService(i);
        } catch(Exception ignored) {}
    }
}