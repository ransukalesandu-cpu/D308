package com.discipline309.app;
import android.app.*;import android.content.*;import java.util.*;
public final class StrictAlarmManager{
 private StrictAlarmManager(){}
 static String key(int i,String x){return "alarm_"+i+"_"+x;}
 public static int count(Context c){return c.getSharedPreferences("strict_mode",Context.MODE_PRIVATE).getInt("alarm_count",0);}
 public static String title(Context c,int i){return c.getSharedPreferences("strict_mode",0).getString(key(i,"title"),"Strict Alarm");}
 public static String time(Context c,int i){return c.getSharedPreferences("strict_mode",0).getString(key(i,"time"),"06:00");}
 public static void add(Context c,String title,int hour,int minute){
  android.content.SharedPreferences p=c.getSharedPreferences("strict_mode",0);int n=p.getInt("alarm_count",0);
  p.edit().putInt("alarm_count",n+1).putString(key(n,"title"),title).putString(key(n,"time"),String.format(Locale.US,"%02d:%02d",hour,minute)).apply();
  schedule(c,n);
 }
 public static void remove(Context c,int index){
  android.content.SharedPreferences p=c.getSharedPreferences("strict_mode",0);int n=p.getInt("alarm_count",0);if(index<0||index>=n)return;
  cancel(c,index);
  android.content.SharedPreferences.Editor e=p.edit();for(int i=index;i<n-1;i++){e.putString(key(i,"title"),p.getString(key(i+1,"title"),"Strict Alarm"));e.putString(key(i,"time"),p.getString(key(i+1,"time"),"06:00"));}e.remove(key(n-1,"title")).remove(key(n-1,"time")).putInt("alarm_count",n-1).apply();
 }
 public static void rescheduleAll(Context c){for(int i=0;i<count(c);i++)schedule(c,i);}
 private static void schedule(Context c,int i){
  if(!StrictModeManager.isEnabled(c))return;
  String[] x=time(c,i).split(":");int h=Integer.parseInt(x[0]),m=Integer.parseInt(x[1]);
  Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,h);d.set(Calendar.MINUTE,m);d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);
  if(d.getTimeInMillis()<=System.currentTimeMillis())d.add(Calendar.DAY_OF_YEAR,1);
  Intent in=new Intent(c,StrictAlarmReceiver.class).setAction(StrictAlarmReceiver.ACTION).putExtra("title",title(c,i)).putExtra("index",i);
  PendingIntent pi=PendingIntent.getBroadcast(c,50000+i,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
  try{if(android.os.Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,d.getTimeInMillis(),pi);}catch(Exception ignored){}
 }
 private static void cancel(Context c,int i){Intent in=new Intent(c,StrictAlarmReceiver.class).setAction(StrictAlarmReceiver.ACTION);PendingIntent pi=PendingIntent.getBroadcast(c,50000+i,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(pi);}
}