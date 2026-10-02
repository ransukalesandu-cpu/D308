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
    public static boolean isSelected(Context c,String pkg){return blocked(c).contains(pkg);}
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