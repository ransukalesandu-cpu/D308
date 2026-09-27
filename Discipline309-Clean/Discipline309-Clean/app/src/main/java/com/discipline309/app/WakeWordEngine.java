package com.discipline309.app;

import android.content.Context;

/**
 * Provider-independent wake-word interface for Maya.
 * A real offline engine such as Porcupine/openWakeWord can implement this later.
 */
public interface WakeWordEngine {
    void start(Context context, Listener listener);
    void stop();

    interface Listener {
        void onWakeWord();
        void onError(String message);
    }
}
