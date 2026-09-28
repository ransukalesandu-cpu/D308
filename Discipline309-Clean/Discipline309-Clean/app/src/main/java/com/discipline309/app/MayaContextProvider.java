package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MayaContextProvider {
    private static final int PROGRAM_DAYS=309;
    private static final String[] DEFAULT_TASKS={
        "Wake up on time","Study / learning","Workout or active recovery",
        "Eat planned meals","No-phone block","Night review + prepare tomorrow"
    };

    private MayaContextProvider(){}

    public static String build(Context context){
        SharedPreferences p=context.getSharedPreferences("discipline",Context.MODE_PRIVATE);
        long startMillis=p.getLong("program_start",System.currentTimeMillis());
        Calendar start=Calendar.getInstance();
        start.setTimeInMillis(startMillis);
        Calendar target=(Calendar)start.clone();
        target.add(Calendar.DAY_OF_YEAR,PROGRAM_DAYS-1);
        Calendar now=Calendar.getInstance();

        int day=Math.max(0,Math.min(PROGRAM_DAYS,daysFromStart(startMillis)));
        int remaining=Math.max(0,PROGRAM_DAYS-day);
        String todayKey=key(now);
        int customCount=Math.max(0,Math.min(100,p.getInt("custom_count",0)));
        int totalTasks=DEFAULT_TASKS.length+customCount;
        int todayTasks=countFor(p,todayKey,totalTasks);
        boolean missionDone=p.getBoolean("mission_"+todayKey+"_done",false);

        String mission=p.getString("mission_"+todayKey,"");
        if(mission.isEmpty()){
            String[] missions={
                "Complete every planned task today",
                "Finish one focused study session",
                "Do your routine before entertainment",
                "Write a 3-line evening reflection",
                "Complete today without skipping a habit"
            };
            mission=missions[Math.abs(todayKey.hashCode())%missions.length];
        }

        int completedDays=completedDays(p,start,now,target);
        int totalCompletedTasks=totalCompletedTasks(p,start,now,target,totalTasks);
        long xpLong=(long)completedDays*100L+(long)totalCompletedTasks*20L+Math.max(0,Math.min(1000000,p.getInt("xp_bonus",0)));
        int xp=(int)Math.min(Integer.MAX_VALUE,xpLong);
        int level=xp/500+1;
        int xpToNext=500-(xp%500);
        int currentStreak=currentStreak(p,start,now);
        int bestStreak=bestStreak(p,start,now,target);

        int unlocked=0;
        int[] milestones={1,3,7,14,30,50,100,150,200,309};
        for(int m:milestones)if(completedDays>=m)unlocked++;

        int rewardedMissions=0;
        int memoryGuard=0;
        for(String k:p.getAll().keySet()){
            if(memoryGuard++>=1000)break;
            if(k.startsWith("mission_")&&k.endsWith("_rewarded")&&p.getBoolean(k,false)) rewardedMissions++;
        }

        String journal=p.getString("journal_"+todayKey,"none");
        int shortPlanCount=Math.max(0,Math.min(50,p.getInt("plan_count_"+todayKey,0)));
        int shortPlanDone=0;
        String focusGoal=p.getString("plan_"+todayKey+"_goal","");
        StringBuilder shortPlan=new StringBuilder();
        for(int i=0;i<shortPlanCount;i++){
            boolean done=p.getBoolean("plan_"+todayKey+"_"+i+"_done",false);
            if(done) shortPlanDone++;
            String name=p.getString("plan_"+todayKey+"_"+i+"_name","Task");
            String time=p.getString("plan_"+todayKey+"_"+i+"_time","Anytime");
            String priority=p.getString("plan_"+todayKey+"_"+i+"_priority","Medium");
            if(shortPlan.length()>0) shortPlan.append(" | ");
            shortPlan.append(name).append(" [").append(time).append(", ").append(priority).append(done?", done":", pending").append("]");
        }
        return "309 DAY DISCIPLINE LIVE APP STATE: "+
                "day="+day+"/"+PROGRAM_DAYS+
                "; daysRemaining="+remaining+
                "; todayTasks="+todayTasks+"/"+totalTasks+
                "; todayMission=\""+safe(mission)+"\""+
                "; missionCompleted="+missionDone+
                "; currentStreak="+currentStreak+
                "; bestStreak="+bestStreak+
                "; completedDays="+completedDays+
                "; totalCompletedTasks="+totalCompletedTasks+
                "; XP="+xp+
                "; level="+level+
                "; XPToNextLevel="+xpToNext+
                "; dayMilestonesUnlocked="+unlocked+"/10"+
                "; rewardedMissions="+rewardedMissions+
                "; shortPlan="+shortPlanDone+"/"+shortPlanCount+" completed"+
                "; focusGoal=\""+safe(focusGoal)+"\""+
                "; shortPlanDetails=\""+safe(shortPlan.toString())+"\""+
                "; journalToday=\""+safe(journal)+"\".";
    }

    public static String quickStatus(Context context,String type){
        String full=build(context);
        if("mission".equals(type)) return extract(full,"todayMission=")+"; completed="+extract(full,"missionCompleted=");
        if("xp".equals(type)) return extract(full,"XP=")+"; "+extract(full,"level=")+"; "+extract(full,"XPToNextLevel=");
        if("streak".equals(type)) return extract(full,"currentStreak=")+"; "+extract(full,"bestStreak=");
        if("day".equals(type)) return extract(full,"day=")+"; "+extract(full,"daysRemaining=");
        return extract(full,"day=")+"; "+extract(full,"todayTasks=")+"; "+extract(full,"currentStreak=")+"; "+extract(full,"XP=");
    }

    private static String extract(String s,String key){
        int i=s.indexOf(key); if(i<0)return "";
        int j=s.indexOf(';',i); if(j<0)j=s.length();
        return s.substring(i,j).trim();
    }
    private static String safe(String s){if(s==null)return "";return s.replace("\\","/").replace("\"","\'").replace("\n"," ").replace("\r"," ");}

    private static int daysFromStart(long startMillis){
        long diff=System.currentTimeMillis()-startMillis;
        return (int)(diff/86400000L)+1;
    }
    private static String key(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());}
    private static int countFor(SharedPreferences p,String dayKey,int totalTasks){
        int n=0;
        for(int i=0;i<totalTasks;i++)if(p.getBoolean("task_"+i+"_"+dayKey,false))n++;
        return n;
    }
    private static int completedDays(SharedPreferences p,Calendar start,Calendar now,Calendar target){
        int n=0; Calendar c=(Calendar)start.clone();
        while(!c.after(now)&&!c.after(target)){if(p.getBoolean("done_"+key(c),false))n++;c.add(Calendar.DAY_OF_YEAR,1);}
        return n;
    }
    private static int totalCompletedTasks(SharedPreferences p,Calendar start,Calendar now,Calendar target,int totalTasks){
        int n=0; Calendar c=(Calendar)start.clone();
        while(!c.after(now)&&!c.after(target)){n+=countFor(p,key(c),totalTasks);c.add(Calendar.DAY_OF_YEAR,1);}
        return n;
    }
    private static int currentStreak(SharedPreferences p,Calendar start,Calendar now){
        int n=0; Calendar c=(Calendar)now.clone();
        if(!p.getBoolean("done_"+key(c),false))c.add(Calendar.DAY_OF_YEAR,-1);
        while(!c.before(start)&&p.getBoolean("done_"+key(c),false)){n++;c.add(Calendar.DAY_OF_YEAR,-1);}
        return n;
    }
    private static int bestStreak(SharedPreferences p,Calendar start,Calendar now,Calendar target){
        int best=0,run=0; Calendar c=(Calendar)start.clone();
        while(!c.after(now)&&!c.after(target)){
            if(p.getBoolean("done_"+key(c),false))run++;else run=0;
            best=Math.max(best,run);c.add(Calendar.DAY_OF_YEAR,1);
        }
        return Math.max(best,p.getInt("best",0));
    }
}