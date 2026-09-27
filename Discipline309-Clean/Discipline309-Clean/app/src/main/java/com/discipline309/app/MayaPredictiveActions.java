package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public final class MayaPredictiveActions {
    private MayaPredictiveActions(){}

    public static String nextSuggestion(Context context) {
        try {
            SharedPreferences p=context.getSharedPreferences("discipline",Context.MODE_PRIVATE);
            Calendar c=Calendar.getInstance();
            int hour=c.get(Calendar.HOUR_OF_DAY);
            int day=Math.max(1,p.getInt("program_day",1));
            boolean missionDone=p.getBoolean("mission_"+new java.text.SimpleDateFormat("yyyyMMdd",java.util.Locale.ROOT).format(c.getTime())+"_done",false);
            int xp=p.getInt("xp_bonus",0)+Math.max(0,p.getInt("completed_days",0))*100;
            String key=new java.text.SimpleDateFormat("yyyyMMdd",java.util.Locale.ROOT).format(c.getTime());
            int planCount=p.getInt("plan_count_"+key,0);
            int planDone=0;
            String nextTask="";
            for(int i=0;i<planCount;i++){
                if(p.getBoolean("plan_"+key+"_"+i+"_done",false)) planDone++;
                else if(nextTask.isEmpty()) nextTask=p.getString("plan_"+key+"_"+i+"_name","next task");
            }
            String focus=p.getString("plan_"+key+"_goal","").trim();
            // Time-aware coaching with light personality variation, while keeping the advice task-focused.
            int vibe=Math.abs((day*31+hour)%4);
            if(planCount>0 && planDone<planCount && !nextTask.isEmpty()){
                if(hour>=18) return vibe==0 ? "දවස ඉවර වෙන්න කලින් Short Plan එකේ "+nextTask+" එක knock කරමුද? 😄📋" : vibe==1 ? "අදට තව එකක් තියෙනවා 😅 "+nextTask+" කරලා close කරමු. 📋" : "Short Plan එකේ next move: "+nextTask+". දැන් කරමු. 🔥";
                if(hour<12) return vibe==0 ? "Good morning 😄 අද පළවෙනි target එක "+nextTask+" කරමු. 🎯" : vibe==1 ? "Morning! ☀️ අද boss move එක "+nextTask+" 😄🎯" : "අද උදේ first win එක "+nextTask+". පටන් ගමු. ⚡";
                if(!focus.isEmpty()) return vibe==0 ? "අද focus goal එක: "+focus+". ඊළඟට "+nextTask+" කරමු. 🔥" : "Focus එක "+focus+" — next move "+nextTask+". One step at a time. 🎯";
                return vibe==0 ? "දැන් next task එක "+nextTask+". පටන් ගමු. 💪" : "Next up 😄 "+nextTask+". ඒක finish කරමු. ⚡";
            }
            if(!missionDone && hour>=17) return vibe==0 ? "අද mission එක තාම complete නෑ. දැන් පොඩි step එකක් කරමුද? 🎯" : "Mission එක තාම waiting 😄 පොඩි step එකක් දාලා ඉවර කරමු. 🎯";
            if(!missionDone && hour<12) return "Good morning ☀️ Day "+day+" — අද first win එක ගමු. 🎯";
            if(xp>0 && xp%500>=400) return vibe==0 ? "Level up එක ළඟයි! තව ටිකක් push කරමු. 🔥" : "XP bar එක level-up එකට ළඟයි 😄 තව එක solid step එකක්. 🔥";
            if(hour>=21) return "දවස close කරන්න කලින් අද progress එක check කරමුද? 🌙";
            if(hour>=12 && hour<17) return vibe==0 ? "දවල් focus time එක. එක වැඩක් තෝරගෙන finish කරමු. ⚡" : "Focus mode 😄 එක වැඩක් තෝරගෙන finish කරමු. 🔥";
            return "Day "+day+" එකේ next step එකට ready. 💪";
        } catch(Exception e) {
            return "අද next step එකක් පටන් ගමු. 💪";
        }
    }

    public static void speakIfUseful(Context context, java.util.function.Consumer<String> callback) {
        if (callback != null) callback.accept(nextSuggestion(context));
    }
}
