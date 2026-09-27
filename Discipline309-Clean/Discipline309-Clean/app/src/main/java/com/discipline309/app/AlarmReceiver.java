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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Discipline alarms", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
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
    }

    private void dismissNotification(Context context, Intent intent) {
        int id=intent.getIntExtra("notification_id",-1);
        if(id>=0){ NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE); if(manager!=null) manager.cancel(id); }
    }

    private void speak(Context context, final String message) {
        try {
            final TextToSpeech[] holder = new TextToSpeech[1];
            holder[0] = new TextToSpeech(context.getApplicationContext(), status -> {
                try {
                    TextToSpeech tts = holder[0];
                    if (tts == null) return;
                    if (status == TextToSpeech.SUCCESS) {
                        tts.setLanguage(new Locale("si", "LK"));
                        tts.setSpeechRate(0.95f);
                        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "discipline");
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
