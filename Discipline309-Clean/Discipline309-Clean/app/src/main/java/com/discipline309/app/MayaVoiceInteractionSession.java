package com.discipline309.app;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.speech.RecognizerIntent;
import java.util.ArrayList;

/**
 * System-assistant bridge for Maya.
 * The selected Android assistant opens this session; Maya then hands voice
 * capture to its existing foreground assistant service.
 */
public class MayaVoiceInteractionSession extends VoiceInteractionSession {
    public MayaVoiceInteractionSession(android.content.Context context) { super(context); }

    @Override public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        startMayaService();
    }


    private void startMayaService() {
        try {
            Intent i=new Intent(getContext(),MayaAssistantService.class);
            i.setAction("com.discipline309.app.MAYA_ASSISTANT_INVOCATION");
            i.putExtra("assistant_invocation",true);
            if(android.os.Build.VERSION.SDK_INT>=26) getContext().startForegroundService(i);
            else getContext().startService(i);
        } catch(Exception ignored) {}
    }
}
