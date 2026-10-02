package com.discipline309.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Schedules simple one-shot timers from Maya voice commands. */
public final class MayaTimerScheduler {
    private MayaTimerScheduler(){}

    public static String preview(String spoken){
        long ms=parseMillis(spoken);
        if(ms<=0)return null;
        return "හරි ⏱️ "+format(ms)+" timer එකක් start කරන්නද?";
    }

    public static String start(Context context,String spoken){
        long ms=parseMillis(spoken);
        if(ms<=0)return null;
        try{
            AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            if(am==null)return "Timer service එක available නැහැ.";
            long when=System.currentTimeMillis()+ms;
            int requestCode=(int)(when/1000L)%1000000;
            Intent i=new Intent(context,AlarmReceiver.class);
            i.putExtra("title","Maya • Timer");
            i.putExtra("msg","⏱️ Timer ඉවරයි! "+format(ms)+" ඉවරයි. දැන් next step එකට යමු. 🔥");
            PendingIntent pi=PendingIntent.getBroadcast(context,requestCode,i,
                    PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            boolean exact=false;
            if(Build.VERSION.SDK_INT>=31){
                try{exact=am.canScheduleExactAlarms();}catch(Exception ignored){}
            }else exact=true;
            if(exact){
                try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}
                catch(SecurityException e){exact=false;}
            }
            if(!exact)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            return "හරි ⏱️ "+format(ms)+" timer එක start කළා. "+format(ms)+"කින් Maya remind කරනවා.";
        }catch(Exception e){
            return "Timer එක start කරන්න බැරි වුණා. Phone alarm permission එක check කරන්න.";
        }
    }

    private static long parseMillis(String spoken){
        if(spoken==null)return -1;
        String s=spoken.toLowerCase(Locale.ROOT).trim();
        if(!(s.contains("timer")||s.contains("ටයිමර්")))return -1;
        Matcher m=Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(seconds?|secs?|sec|s|minutes?|mins?|min|m|hours?|hrs?|hour|h|තත්පර|විනාඩි|පැය)").matcher(s);
        if(!m.find())return -1;
        double value;
        try{value=Double.parseDouble(m.group(1).replace(',','.'));}catch(Exception e){return -1;}
        String unit=m.group(2);
        long multiplier=(unit.startsWith("h")||unit.contains("පැය"))?3600000L:
                (unit.startsWith("s")||unit.contains("තත්පර"))?1000L:60000L;
        long ms=(long)(value*multiplier);
        return ms>0&&ms<=24L*60L*60L*1000L?ms:-1;
    }

    private static String format(long ms){
        long total=ms/1000L;
        long h=total/3600L;
        long m=(total%3600L)/60L;
        long s=total%60L;
        if(h>0)return h+"h "+m+"m";
        if(m>0)return s>0?m+"m "+s+"s":m+"m";
        return s+"s";
    }
}