package com.discipline309.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistant {
    public static final String ASSISTANT_NAME = "Maya";
    private final Activity activity;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean listening = false;

    public VoiceAssistant(Activity activity) {
        this.activity = activity;
        tts = new TextToSpeech(activity, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("si", "LK"));
                tts.setSpeechRate(.92f);
            }
        });
    }

    public void start() {
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3099);
            Toast.makeText(activity, "Allow microphone access, then tap Maya again.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition is not available on this phone.", Toast.LENGTH_LONG).show();
            return;
        }

        if (listening) return;
        listening = true;
        Toast.makeText(activity, "🎙️ Say “Maya” and then talk…", Toast.LENGTH_SHORT).show();

        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(Bundle p) {}
            public void onEvent(int t, Bundle p) {}
            public void onError(int e) {
                listening = false;
                Toast.makeText(activity, "I didn't catch that. Tap Maya and try again.", Toast.LENGTH_SHORT).show();
            }
            public void onResults(Bundle results) {
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (matches == null || matches.isEmpty()) ? "" : matches.get(0);
                handle(text);
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "si-LK");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Maya, then your question");
        recognizer.startListening(intent);
    }

    private void handle(String spoken) {
        String q = spoken == null ? "" : spoken.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        boolean called = lower.contains("maya") || lower.contains("මායා");

        if (!called) {
            speak("මට කතා කරන්න නම් මුලින් Maya කියලා කතා කරන්න. 😄");
            return;
        }

        String question = lower.replace("maya", "").replace("මායා", "").trim();
        String reply;
        if (question.isEmpty()) {
            reply = "ඔව්, මං මෙතන. කියන්න, මොකද වෙන්නේ? 😄";
        } else if (question.contains("hello") || question.contains("hi") || question.contains("හෙලෝ")) {
            reply = "හෙලෝ! මං Maya. අද වැඩේ පටන් ගමුද? 🔥";
        } else if (question.contains("motivat") || question.contains("වැඩ") || question.contains("බැහැ")) {
            reply = "Excuses පස්සේ. පොඩි step එකක් දැන්ම කරමු. ඔයාට මේක පුළුවන්! 🔥";
        } else if (question.contains("sleep") || question.contains("නින්ද")) {
            reply = "හරි, phone එක පැත්තකින් තියලා හොඳට rest ගන්න. 🌙";
        } else if (question.contains("thank") || question.contains("ස්තුති")) {
            reply = "Anytime! දැන් වැඩේ continue කරමු. 😄🔥";
        } else {
            reply = "හරි, මං අහගෙන ඉන්නේ. තව ටිකක් පැහැදිලිව කියන්න. 😄";
        }
        speak(reply);
    }

    private void speak(String text) {
        if (tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_" + System.currentTimeMillis());
    }

    public void destroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}