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
            if(!missionDone && hour>=17) return "අද mission එක තාම complete නෑ. දැන් පොඩි step එකක් කරමුද? 🎯";
            if(!missionDone && hour<12) return "Good morning 😄 අද Day "+day+" එක start කරමු. 🎯";
            if(xp>0 && xp%500>=400) return "Level up එක ළඟයි! තව ටිකක් push කරමු. 🔥";
            if(hour>=21) return "දවස close කරන්න කලින් අද progress එක check කරමුද? 🌙";
            return "Day "+day+" එකේ next step එකට ready. 💪";
        } catch(Exception e) {
            return "අද next step එකක් පටන් ගමු. 💪";
        }
    }

    public static void speakIfUseful(Context context, java.util.function.Consumer<String> callback) {
        if (callback != null) callback.accept(nextSuggestion(context));
    }
}
