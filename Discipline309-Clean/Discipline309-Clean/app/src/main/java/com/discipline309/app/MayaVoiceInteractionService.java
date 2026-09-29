package com.discipline309.app;

import android.service.voice.VoiceInteractionService;

/**
 * Android system-assistant entry point for Maya.
 *
 * The actual microphone foreground service is started by
 * MayaVoiceInteractionSession only when the user invokes the assistant.
 * Starting a microphone FGS from onReady() is unsafe on newer Android
 * versions because the service may be initialized while the app is in the
 * background.
 */
public class MayaVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() {
        super.onReady();
    }

    @Override public void onShutdown() {
        super.onShutdown();
    }
}
