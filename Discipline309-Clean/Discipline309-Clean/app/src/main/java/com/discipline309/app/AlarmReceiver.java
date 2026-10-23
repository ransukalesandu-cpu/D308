package com.discipline309.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "discipline";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
        if("com.discipline309.DISMISS".equals(intent.getAction())) { dismissNotification(context,intent); return; }
        if(!context.getSharedPreferences("settings",Context.MODE_PRIVATE).getBoolean("notifications",true)) return;
        String title = intent.getStringExtra("title");
        if (title == null || title.trim().isEmpty()) title = "Discipline reminder";

        String message = intent.getStringExtra("msg");
        if (message == null || message.trim().isEmpty()) {
            message = "නැගිටින්න! දැන්ම වැඩේ පටන් ගන්න. Do it! Do it! 🔥";
        }
        final String finalMessage = message;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Discipline alarms", NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            } catch (Exception ignored) { return; }
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        Intent done = new Intent(context, AlarmReceiver.class).setAction("com.discipline309.DISMISS").putExtra("notification_id", (int) (System.currentTimeMillis() % 100000));
        PendingIntent doneIntent = PendingIntent.getBroadcast(context, (int) (System.currentTimeMillis() % 100000), done, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(finalMessage)
                .setStyle(new Notification.BigTextStyle().bigText(finalMessage))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "Open 309", pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", doneIntent)
                .setPriority(Notification.PRIORITY_HIGH);

        if (manager != null) {
            manager.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        }

        speak(context, finalMessage);
        } catch (Exception ignored) {
            // Never let an alarm callback crash the app/process.
        }
    }

    private void dismissNotification(Context context, Intent intent) {
        int id=intent.getIntExtra("notification_id",-1);
        if(id>=0){ NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE); if(manager!=null) manager.cancel(id); }
    }

    private void speak(Context context, final String message) {
        try {
            android.content.SharedPreferences settings =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            if (!settings.getBoolean("auto_speak", true)) return;

            final TextToSpeech[] holder = new TextToSpeech[1];
            holder[0] = new TextToSpeech(context.getApplicationContext(), status -> {
                TextToSpeech engine = holder[0];
                if (engine == null) return;
                try {
                    if (status != TextToSpeech.SUCCESS) {
                        engine.shutdown();
                        return;
                    }

                    String selected = settings.getString("maya_language", "auto");
                    Locale target = ("si".equals(selected)
                            || ("auto".equals(selected)
                            && "LK".equalsIgnoreCase(Locale.getDefault().getCountry())))
                            ? new Locale("si", "LK") : Locale.ENGLISH;
                    int languageStatus = engine.setLanguage(target);
                    if (languageStatus == TextToSpeech.LANG_MISSING_DATA
                            || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                        target = Locale.ENGLISH;
                        engine.setLanguage(target);
                    }
                    // Match alarm voice to Maya's selected local voice where available.
                    if (Build.VERSION.SDK_INT >= 21) {
                        try {
                            int choice = Math.max(0, Math.min(2,
                                    settings.getInt("maya_voice", 0)));
                            java.util.ArrayList<android.speech.tts.Voice> localVoices =
                                    new java.util.ArrayList<>();
                            java.util.ArrayList<android.speech.tts.Voice> femaleVoices =
                                    new java.util.ArrayList<>();
                            java.util.Set<android.speech.tts.Voice> available = engine.getVoices();
                            if (available != null) {
                                for (android.speech.tts.Voice voice : available) {
                                    if (voice == null || voice.getLocale() == null
                                            || !target.getLanguage().equals(
                                                    voice.getLocale().getLanguage())
                                            || voice.isNetworkConnectionRequired()) continue;
                                    localVoices.add(voice);
                                    String name = voice.getName() == null ? ""
                                            : voice.getName().toLowerCase(Locale.ROOT);
                                    if (name.contains("female") || name.contains("fem")
                                            || name.contains("woman") || name.contains("girl")) {
                                        femaleVoices.add(voice);
                                    }
                                }
                            }
                            java.util.ArrayList<android.speech.tts.Voice> pool =
                                    femaleVoices.size() >= 3 ? femaleVoices : localVoices;
                            if (!pool.isEmpty()) {
                                java.util.Collections.sort(pool, (a, b) -> {
                                    int quality = Integer.compare(b.getQuality(), a.getQuality());
                                    return quality != 0 ? quality
                                            : a.getName().compareToIgnoreCase(b.getName());
                                });
                                int index = pool.size() >= 3
                                        ? (choice == 0 ? 0
                                        : choice == 1 ? pool.size() / 2 : pool.size() - 1)
                                        : Math.min(choice, pool.size() - 1);
                                engine.setVoice(pool.get(index));
                            }
                        } catch (Exception ignored) {}
                    }
                    int voiceChoice = Math.max(0, Math.min(2, settings.getInt("maya_voice", 0)));
                    float[] pitches = {1.08f, 1.02f, 0.98f};
                    engine.setPitch(pitches[voiceChoice]);
                    engine.setSpeechRate(0.95f);
                    if (Build.VERSION.SDK_INT >= 15) {
                        engine.setOnUtteranceProgressListener(
                                new android.speech.tts.UtteranceProgressListener() {
                            @Override public void onStart(String utteranceId) {}
                            @Override public void onDone(String utteranceId) {
                                try { engine.shutdown(); } catch (Exception ignored) {}
                            }
                            @Override public void onError(String utteranceId) {
                                try { engine.shutdown(); } catch (Exception ignored) {}
                            }
                        });
                    }
                    if (Build.VERSION.SDK_INT >= 21) {
                        engine.speak(message, TextToSpeech.QUEUE_FLUSH, null,
                                "discipline_" + System.currentTimeMillis());
                    } else {
                        engine.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                    }
                } catch (Exception ignored) {
                    try { engine.shutdown(); } catch (Exception ignoredAgain) {}
                }
            });
        } catch (Exception ignored) {
        }
    }
}
