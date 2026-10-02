package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class StrictModeManager {
    private static final String PREF="strict_mode";
    private StrictModeManager(){}
    static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public static boolean isEnabled(Context c){return p(c).getBoolean("enabled",false);}
    public static void setEnabled(Context c,boolean on){
        p(c).edit().putBoolean("enabled",on).apply();
        if(on && p(c).getBoolean("auto_dnd",false)) setDnd(c,true);
        if(!on) setDnd(c,false);
    }
    public static int limitMinutes(Context c){return Math.max(1,p(c).getInt("limit_minutes",30));}
    public static Set<String> blocked(Context c){return new HashSet<>(p(c).getStringSet("blocked",new HashSet<>()));}
    public static void setBlocked(Context c,Set<String> apps){p(c).edit().putStringSet("blocked",new HashSet<>(apps)).apply();}
    public static boolean isSelected(Context c,String pkg){return blocked(c).contains(pkg)&&!whitelisted(c).contains(pkg);}
    public static Set<String> whitelisted(Context c){return new HashSet<>(p(c).getStringSet("whitelist",new HashSet<>()));}
    public static void setWhitelist(Context c,Set<String> apps){p(c).edit().putStringSet("whitelist",new HashSet<>(apps)).apply();}
    public static String dayKey(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    public static int usedMinutes(Context c,String pkg){
        return p(c).getInt("used_"+dayKey()+"_"+pkg,0);
    }
    public static void addMinute(Context c,String pkg){
        if(pkg==null||pkg.isEmpty())return;
        String key="used_"+dayKey()+"_"+pkg;
        p(c).edit().putInt(key,usedMinutes(c,pkg)+1).apply();
    }
    public static void resetOldUsage(Context c){
        String today=dayKey();
        Map<String,?> all=p(c).getAll();
        SharedPreferences.Editor e=p(c).edit();
        for(String k:all.keySet()) if(k.startsWith("used_")&&!k.startsWith("used_"+today+"_")) e.remove(k);
        e.apply();
    }
    public static boolean limitReached(Context c,String pkg){return isSelected(c,pkg)&&usedMinutes(c,pkg)>=limitMinutes(c);}
    public static boolean noSkip(Context c){return isEnabled(c)&&p(c).getBoolean("no_skip",true);}
    public static boolean focusActive(Context c){return p(c).getLong("focus_until",0L)>System.currentTimeMillis();}
    public static long focusUntil(Context c){return p(c).getLong("focus_until",0L);}
    public static void startFocus(Context c,int minutes){
        int m=Math.max(1,Math.min(240,minutes));
        p(c).edit().putLong("focus_until",System.currentTimeMillis()+m*60000L).apply();
        setDnd(c,true);
        scheduleFocusEnd(c,m);
    }
    public static void stopFocus(Context c){
        p(c).edit().remove("focus_until").apply();
        if(isEnabled(c)&&p(c).getBoolean("auto_dnd",false))setDnd(c,true);else setDnd(c,false);
    }
    private static void scheduleFocusEnd(Context c,int minutes){
        Intent i=new Intent(c,StrictFocusReceiver.class).setAction(StrictFocusReceiver.ACTION);
        PendingIntent pi=PendingIntent.getBroadcast(c,30978,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        long when=System.currentTimeMillis()+minutes*60000L;
        try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setExact(AlarmManager.RTC_WAKEUP,when,pi);}catch(Exception ignored){}
    }
    public static void scheduleStrict(Context c,int startHour,int startMinute,int endHour,int endMinute){
        p(c).edit().putInt("schedule_start_h",startHour).putInt("schedule_start_m",startMinute).putInt("schedule_end_h",endHour).putInt("schedule_end_m",endMinute).putBoolean("schedule_enabled",true).apply();
        scheduleAlarm(c,true,startHour,startMinute,30979);
        scheduleAlarm(c,false,endHour,endMinute,30980);
    }
    public static void clearSchedule(Context c){p(c).edit().putBoolean("schedule_enabled",false).apply();}
    private static void scheduleAlarm(Context c,boolean on,int h,int m,int req){
        Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,h);d.set(Calendar.MINUTE,m);d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);
        if(d.getTimeInMillis()<=System.currentTimeMillis())d.add(Calendar.DAY_OF_YEAR,1);
        Intent i=new Intent(c,StrictScheduleReceiver.class).setAction(on?StrictScheduleReceiver.ON:StrictScheduleReceiver.OFF);
        PendingIntent pi=PendingIntent.getBroadcast(c,req,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,d.getTimeInMillis(),pi);}catch(Exception ignored){}
    }
    public static void reschedule(Context c){
        if(!p(c).getBoolean("schedule_enabled",false))return;
        scheduleStrict(c,p(c).getInt("schedule_start_h",6),p(c).getInt("schedule_start_m",0),p(c).getInt("schedule_end_h",22),p(c).getInt("schedule_end_m",0));
    }
    public static boolean canUseDnd(Context c){
        if(Build.VERSION.SDK_INT<23)return false;
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm!=null&&nm.isNotificationPolicyAccessGranted();
    }
    public static void setDnd(Context c,boolean on){
        if(!canUseDnd(c))return;
        try{
            NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.setInterruptionFilter(on?NotificationManager.INTERRUPTION_FILTER_PRIORITY:NotificationManager.INTERRUPTION_FILTER_ALL);
        }catch(Exception ignored){}
    }
    public static void openDndAccess(Context c){
        try{c.startActivity(new Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}
    }
    public static void disableDndAfter(Context c,int minutes){
        if(!canUseDnd(c))return;
        int m=Math.max(1,Math.min(240,minutes));
        Intent i=new Intent(c,StrictDndReceiver.class).setAction(StrictDndReceiver.ACTION);
        PendingIntent pi=PendingIntent.getBroadcast(c,30977,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        long when=System.currentTimeMillis()+m*60000L;
        try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setExact(AlarmManager.RTC_WAKEUP,when,pi);}catch(Exception ignored){}
    }
}