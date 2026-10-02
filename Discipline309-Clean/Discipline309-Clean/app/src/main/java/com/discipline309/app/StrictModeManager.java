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
        SharedPreferences sp=p(c);
        if(on){
            if(!sp.contains("saved_maya_voice")){
                sp.edit().putInt("saved_maya_voice",c.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("maya_voice",0))
                  .putString("saved_maya_mode",c.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("maya_mode","motivative")).apply();
            }
            sp.edit().putBoolean("enabled",true).apply();
            c.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putInt("maya_voice",0).apply();
        }else{
            sp.edit().putBoolean("enabled",false).apply();
            SharedPreferences settings=c.getSharedPreferences("settings",Context.MODE_PRIVATE);
            int voice=sp.getInt("saved_maya_voice",settings.getInt("maya_voice",0));
            String mode=sp.getString("saved_maya_mode",settings.getString("maya_mode","motivative"));
            settings.edit().putInt("maya_voice",voice).putString("maya_mode",mode).apply();
            sp.edit().remove("saved_maya_voice").remove("saved_maya_mode").apply();
        }
        if(on && sp.getBoolean("auto_dnd",false)) setDnd(c,true);
        if(on) StrictAlarmManager.rescheduleAll(c);
        if(!on) { setDnd(c,false); StrictAlarmManager.rescheduleAll(c); }
    }
    public static String strictMayaMode(Context c){
        return strictMayaMode(c,"","");
    }

    public static String strictMayaMode(Context c,String userText,String emotionalTone){
        if(!isEnabled(c)) return null;
        try{
            SharedPreferences sp=p(c);
            Calendar cal=Calendar.getInstance();
            int hour=cal.get(Calendar.HOUR_OF_DAY);
            String key=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(cal.getTime());
            int planCount=sp.getInt("plan_count_"+key,0);
            int planDone=0;
            String nextTask="";
            for(int i=0;i<planCount;i++){
                if(sp.getBoolean("plan_"+key+"_"+i+"_done",false)) planDone++;
                else if(nextTask.isEmpty()) nextTask=sp.getString("plan_"+key+"_"+i+"_name","");
            }
            boolean missionDone=sp.getBoolean("mission_"+key+"_done",false);
            boolean pendingTasks=planCount>0 && planDone<planCount;
            String q=userText==null?"":userText.toLowerCase(Locale.ROOT);

            // Strict Mode stays within its two tones, but chooses the tone from live context.
            if(q.contains("skip")||q.contains("skipped")||q.contains("avoid")||q.contains("ignored")
                    ||q.contains("delay")||q.contains("refuse")||q.contains("excuse")
                    ||q.contains("skip කළ")||q.contains("නොකර")||q.contains("පස්සේ කර")
                    ||q.contains("අද නෑ")||q.contains("බැහැ"))
                return "angry";
            if("frustrated".equals(emotionalTone)) return "motivative";
            if("stressed".equals(emotionalTone)||"tired".equals(emotionalTone)) return "motivative";
            if(pendingTasks && hour>=18) return "angry";
            if(!missionDone && hour>=18) return "angry";
            if(pendingTasks || !missionDone) return "motivative";
            if(hour>=21 || hour<7) return "motivative";
            return "motivative";
        }catch(Exception ignored){
            return "motivative";
        }
    }
    public static int strictEscalationLevel(Context c,String userText){
        if(!isEnabled(c)||userText==null)return 0;
        SharedPreferences sp=p(c);
        String today=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());
        String storedDay=sp.getString("strict_skip_day","");
        if(!today.equals(storedDay)){
            sp.edit().putString("strict_skip_day",today)
                    .putInt("strict_skip_level",0).apply();
        }
        String q=userText.toLowerCase(Locale.ROOT);
        boolean skip=q.contains("skip")||q.contains("skipped")||q.contains("avoid")||q.contains("ignored")
                ||q.contains("delay")||q.contains("refuse")||q.contains("excuse")
                ||q.contains("skip කළ")||q.contains("නොකර")||q.contains("පස්සේ කර")
                ||q.contains("අද නෑ")||q.contains("බැහැ");
        if(!skip)return sp.getInt("strict_skip_level",0);
        String countKey="strict_skip_"+today;
        int count=Math.min(3,sp.getInt(countKey,0)+1);
        sp.edit().putString("strict_skip_day",today)
                .putInt(countKey,count).putInt("strict_skip_level",count).apply();
        return count;
    }

    public static void resetStrictEscalation(Context c){
        p(c).edit().putInt("strict_skip_level",0).apply();
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
    public static int blockedAttempts(Context c,String pkg){return p(c).getInt("blocked_"+dayKey()+"_"+pkg,0);}
    public static void addBlockedAttempt(Context c,String pkg){if(pkg!=null&&!pkg.isEmpty()){String k="blocked_"+dayKey()+"_"+pkg;p(c).edit().putInt(k,blockedAttempts(c,pkg)+1).apply();}}
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