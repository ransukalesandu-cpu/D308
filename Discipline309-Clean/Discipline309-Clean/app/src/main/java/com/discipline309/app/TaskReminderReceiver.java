package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TaskReminderReceiver extends BroadcastReceiver {
    public static final String ACTION="com.discipline309.TASK_REMINDER";
    @Override public void onReceive(Context context, Intent intent) {
        String task=intent.getStringExtra("task");
        if(task==null||task.trim().isEmpty()) return;
        String msg="Maya reminder 🔔: දැන් \""+task+"\" කරන්න වෙලාවයි.";
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null) return;
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel("maya_tasks","Maya Task Reminders",NotificationManager.IMPORTANCE_HIGH));
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(context,(int)(System.currentTimeMillis()%100000),open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,"maya_tasks"):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Maya • Task Reminder").setContentText(msg).setStyle(new Notification.BigTextStyle().bigText(msg)).setAutoCancel(true).setContentIntent(pi).setPriority(Notification.PRIORITY_HIGH);
        nm.notify((int)(System.currentTimeMillis()%100000),b.build());
        try {
            final TextToSpeech[] holder=new TextToSpeech[1];
            holder[0]=new TextToSpeech(context.getApplicationContext(),s->{if(s==TextToSpeech.SUCCESS){holder[0].setLanguage(new Locale("si","LK"));holder[0].setSpeechRate(.95f);holder[0].speak(msg,TextToSpeech.QUEUE_FLUSH,null,"maya-task");}});
        } catch(Exception ignored){}
    }
}
