package com.discipline309.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Temporary provider-independent wake-word adapter.
 * It detects Maya from Android speech recognition while keeping the engine
 * behind WakeWordEngine so a true offline engine can replace it later.
 */
public class AndroidSpeechWakeWordEngine implements WakeWordEngine {
    private SpeechRecognizer recognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private Runnable restartRunnable;

    @Override public void start(Context context, Listener listener) {
        stop();
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition is unavailable.");
            return;
        }
        running = true;
        try{ recognizer = SpeechRecognizer.createSpeechRecognizer(context); }
        catch(Exception e){ running=false; listener.onError("Speech recognizer could not start."); return; }
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(android.os.Bundle b) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(android.os.Bundle b) {}
            public void onEvent(int t, android.os.Bundle b) {}
            public void onError(int e) { if(running) restart(context, listener); }
            public void onResults(android.os.Bundle b) {
                ArrayList<String> results = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = results == null || results.isEmpty() ? "" : results.get(0);
                String lower = text.toLowerCase(Locale.ROOT);
                if (lower.contains("maya") || lower.contains("මායා") || lower.contains("මයා")) {
                    listener.onWakeWord();
                }
                restart(context, listener);
            }
        });
        listen();
    }

    private void listen() {
        if (!running || recognizer == null) return;
        IntentFactory.start(recognizer, context);
    }

    private void restart(Context context, Listener listener) {
        if(!running)return;
        if(restartRunnable!=null)handler.removeCallbacks(restartRunnable);
        restartRunnable=()->{
            if(running){
                stopRecognizerOnly();
                try{start(context,listener);}catch(Exception e){running=false;listener.onError("Speech recognition stopped.");}
            }
        };
        handler.postDelayed(restartRunnable,700);
    }

    private void stopRecognizerOnly() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    @Override public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        restartRunnable=null;
        stopRecognizerOnly();
    }

    private static final class IntentFactory {
        static void start(SpeechRecognizer recognizer, Context context) {
            android.content.Intent i = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            android.content.SharedPreferences p = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
            String selected = p.getString("maya_language", "auto");
            String country = java.util.Locale.getDefault().getCountry();
            boolean sinhala = "si".equals(selected) || ("auto".equals(selected) && "LK".equalsIgnoreCase(country));
            String locale = sinhala ? "si-LK" : "en-LK";
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale);
            try{recognizer.startListening(i);}catch(Exception e){throw new IllegalStateException("Speech recognition could not listen.",e);}
        }
    }
}
