package com.discipline309.app;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
public final class MoodAlarm {
 private MoodAlarm(){}
 public static void scheduleNext(Context c){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,MoodDayReceiver.class).setAction(MoodDayReceiver.ACTION_FINALIZE);PendingIntent pi=PendingIntent.getBroadcast(c,30901,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Calendar n=Calendar.getInstance();n.add(Calendar.DAY_OF_YEAR,1);n.set(Calendar.HOUR_OF_DAY,0);n.set(Calendar.MINUTE,0);n.set(Calendar.SECOND,3);n.set(Calendar.MILLISECOND,0);try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),pi);}catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),pi);}}
}