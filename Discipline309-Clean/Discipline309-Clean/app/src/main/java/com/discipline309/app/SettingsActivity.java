package com.discipline309.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private TextToSpeech tts;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        buildUi();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView title(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(22);
        v.setTextColor(0xFFFFFFFF);
        v.setTypeface(null, 1);
        v.setPadding(dp(20), dp(18), dp(20), dp(12));
        return v;
    }

    private TextView label(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(16);
        v.setTextColor(0xFFE8EAF0);
        v.setPadding(dp(20), dp(14), dp(20), dp(6));
        return v;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B1020);

        TextView head = title("⚙  Settings");
        root.addView(head);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(label("AI VOICE"));
        Switch autoSpeak = new Switch(this);
        autoSpeak.setText("  Auto Speak AI Responses");
        autoSpeak.setTextSize(16);
        autoSpeak.setTextColor(0xFFFFFFFF);
        autoSpeak.setChecked(prefs.getBoolean("auto_speak", true));
        autoSpeak.setPadding(dp(20), dp(10), dp(20), dp(10));
        autoSpeak.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("auto_speak", checked).apply());
        content.addView(autoSpeak);

        content.addView(label("Speech speed"));
        SeekBar speed = new SeekBar(this);
        speed.setMax(100);
        speed.setProgress(prefs.getInt("speech_speed", 50));
        speed.setPadding(dp(20), 0, dp(20), dp(10));
        content.addView(speed);
        TextView speedText = label("Normal");
        content.addView(speedText);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean fromUser) {
                float rate = 0.65f + (p / 100f) * 0.85f;
                String text = rate < .9f ? "Slow" : rate > 1.15f ? "Fast" : "Normal";
                speedText.setText("Speech speed: " + text);
                if (fromUser) prefs.edit().putInt("speech_speed", p).apply();
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });

        content.addView(label("TEST VOICE"));
        Button test = new Button(this);
        test.setText("🔊  Test AI Voice");
        test.setOnClickListener(v -> speak("Hello. Your Discipline assistant is ready."));
        content.addView(test);

        content.addView(label("APP"));
        Switch notifications = new Switch(this);
        notifications.setText("  Notifications");
        notifications.setTextColor(0xFFFFFFFF);
        notifications.setTextSize(16);
        notifications.setChecked(prefs.getBoolean("notifications", true));
        notifications.setPadding(dp(20), dp(10), dp(20), dp(10));
        notifications.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("notifications", checked).apply());
        content.addView(notifications);

        Button theme = new Button(this);
        theme.setText("🎨  Appearance");
        theme.setOnClickListener(v -> Toast.makeText(this,
                "Theme settings are ready for the next UI update.", Toast.LENGTH_SHORT).show());
        content.addView(theme);

        Button reset = new Button(this);
        reset.setText("↻  Reset Progress");
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reset progress?")
                .setMessage("This will remove saved discipline progress.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d,w) ->
                        getSharedPreferences("discipline", MODE_PRIVATE).edit().clear().apply())
                .show());
        content.addView(reset);

        TextView version = label("Discipline • Settings\nVersion 1.0");
        version.setPadding(dp(20), dp(30), dp(20), dp(30));
        content.addView(version);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void speak(String text) {
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) speakNow(text);
            });
        } else speakNow(text);
    }

    private void speakNow(String text) {
        float rate = .65f + (prefs.getInt("speech_speed", 50) / 100f) * .85f;
        tts.setLanguage(Locale.US);
        tts.setSpeechRate(rate);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "discipline_settings");
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
