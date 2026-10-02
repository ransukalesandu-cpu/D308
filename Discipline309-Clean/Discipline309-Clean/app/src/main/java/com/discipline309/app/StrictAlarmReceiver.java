package com.discipline309.app;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;
public class StrictAlarmReceiver extends BroadcastReceiver{
 public static final String ACTION="com.discipline309.STRICT_ALARM";
 @Override public void onReceive(Context c,Intent i){
  if(!ACTION.equals(i.getAction())||!StrictModeManager.isEnabled(c))return;
  String title=i.getStringExtra("title"); if(title==null||title.trim().isEmpty())title="Strict Alarm";
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("strict_alarm","Strict Alarms",NotificationManager.IMPORTANCE_HIGH);if(nm!=null)nm.createNotificationChannel(ch);}
  Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pi=PendingIntent.getActivity(c,Math.abs(title.hashCode()),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"strict_alarm"):new Notification.Builder(c);
  b.setSmallIcon(com.discipline309.app.R.drawable.ic_launcher_309).setContentTitle("🔒 Maya • Strict Alarm").setContentText("දැන්: "+title).setAutoCancel(true).setContentIntent(pi);
  if(nm!=null)nm.notify(Math.abs(title.hashCode()),b.build());
  try{android.media.ToneGenerator t=new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM,90);t.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,700);new Handler().postDelayed(t::release,1000);}catch(Exception ignored){}
 }
}