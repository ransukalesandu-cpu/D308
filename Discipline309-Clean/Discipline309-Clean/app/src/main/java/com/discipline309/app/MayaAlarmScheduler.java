package com.discipline309.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Voice-friendly alarm parser/scheduler used by Maya. */
public final class MayaAlarmScheduler {
    private MayaAlarmScheduler(){}

    public static String scheduleFromVoice(Context context, String spoken){
        Parsed p=parse(spoken);
        if(p==null) return null;
        try{
            AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            if(am==null)return "Alarm service එක available නැහැ.";
            long when=p.time.getTimeInMillis();
            if(when<=System.currentTimeMillis())return "ඒ වෙලාව පහු වෙලා. Future time එකක් කියන්න.";
            int requestCode=(int)(when/60000L)%1000000;
            Intent i=new Intent(context,AlarmReceiver.class);
            i.putExtra("title","Maya • Voice Alarm");
            i.putExtra("msg",p.label);
            PendingIntent pi=PendingIntent.getBroadcast(context,requestCode,i,
                    PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            boolean exact=false;
            if(Build.VERSION.SDK_INT>=31){
                try{ exact=am.canScheduleExactAlarms(); }catch(Exception ignored){}
            }else exact=true;
            if(exact){
                try{ am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); }
                catch(SecurityException e){ exact=false; }
            }
            if(!exact) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            String date=new SimpleDateFormat("MMM d",Locale.ENGLISH).format(p.time.getTime());
            String time=new SimpleDateFormat("h:mm a",Locale.ENGLISH).format(p.time.getTime());
            return "හරි 🔔 "+date+" at "+time+"ට alarm එක set කළා: "+p.label;
        }catch(Exception e){
            return "Alarm එක set කරන්න බැරි වුණා. Phone alarm permission එක check කරන්න.";
        }
    }

    public static String preview(Context context,String spoken){
        Parsed p=parse(spoken);
        if(p==null)return null;
        return "හරි 🔔 "+new SimpleDateFormat("MMM d, h:mm a",Locale.ENGLISH).format(p.time.getTime())
                +"ට ""+p.label+"" alarm එක දාන්නද?";
    }

    private static Parsed parse(String spoken){
        if(spoken==null)return null;
        String s=spoken.toLowerCase(Locale.ROOT).trim();
        if(s.isEmpty())return null;
        if(!isAlarmCommand(s))return null;

        Calendar now=Calendar.getInstance();
        Calendar target=(Calendar)now.clone();
        if(s.contains("tomorrow")||s.contains("හෙට")||s.contains("heta")) target.add(Calendar.DAY_OF_YEAR,1);

        Matcher dm=Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").matcher(s);
        if(dm.find()){
            try{
                target.set(Calendar.YEAR,Integer.parseInt(dm.group(1)));
                target.set(Calendar.MONTH,Integer.parseInt(dm.group(2))-1);
                target.set(Calendar.DAY_OF_MONTH,Integer.parseInt(dm.group(3)));
            }catch(Exception ignored){}
        }

        Matcher tm=Pattern.compile("(?i)(?:at|@|\u0da7|\u0da7\u0db8|\u0dcf)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").matcher(s);
        if(!tm.find()) return null;
        int hour=Integer.parseInt(tm.group(1));
        int minute=tm.group(2)==null?0:Integer.parseInt(tm.group(2));
        String ap=tm.group(3);
        if(ap!=null){
            if("pm".equalsIgnoreCase(ap)&&hour<12)hour+=12;
            if("am".equalsIgnoreCase(ap)&&hour==12)hour=0;
        }else{
            if(s.contains("evening")||s.contains("night")||s.contains("හවස")||s.contains("රෑ")){ if(hour<12)hour+=12; }
            else if((s.contains("morning")||s.contains("උදේ"))&&hour==12)hour=0;
        }
        if(hour>23||minute>59)return null;
        target.set(Calendar.HOUR_OF_DAY,hour);
        target.set(Calendar.MINUTE,minute);
        target.set(Calendar.SECOND,0);
        target.set(Calendar.MILLISECOND,0);
        if(!s.contains("tomorrow")&&!s.contains("හෙට")&&!s.contains("heta")&&target.getTimeInMillis()<=now.getTimeInMillis())
            target.add(Calendar.DAY_OF_YEAR,1);

        String label=s;
        label=label.replaceAll("(?i)\\b(?:set|add|create|make|schedule|an|a|the|alarm|reminder|for|on|today|tomorrow|at|@|morning|evening|night|උදේ|හවස|රෑ|හෙට|heta|alarm එකක්|alarm එක|දාලා|දාන්න|දන්න|කරන්න)\\b"," ");
        label=label.replaceAll("(?i)\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?\\b"," ");
        label=label.replaceAll("\\s+"," ").trim();
        if(label.isEmpty())label="Maya alarm";
        return new Parsed(target,label);
    }

    private static boolean isAlarmCommand(String s){
        return s.contains("alarm")||s.contains("reminder")||s.contains("alarm එක")
                ||s.contains("alarm ekak")||s.contains("alarm eka")
                ||s.contains("ඇලම්")||s.contains("එලාම්");
    }

    private static final class Parsed{
        final Calendar time; final String label;
        Parsed(Calendar time,String label){this.time=time;this.label=label;}
    }
}