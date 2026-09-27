package com.discipline309.app;

import android.content.Context;
import java.util.Locale;

public final class MayaOfflineNLP {
    private MayaOfflineNLP(){}

    public static String answer(Context context, String text) {
        String q = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return null;

        if (has(q, "hello","hi","hey","ආයුබෝවන්","හලෝ")) return "Hey 😄 Maya මෙතන! කියන්න, මොකක්ද වැඩේ?";
        if (has(q, "who are you","what are you","ඔයා කවුද","oyaa kawda","oya kawda")) return "මම Maya — 309 Day Discipline එකේ personal assistant. 🧠";
        if (has(q, "what can you do","මොනවද කරන්න පුළුවන්","monawada karanna puluwan")) return "මට app progress බලන්න, mission/XP/streak කියන්න, basic phone commands handle කරන්න, memory සහ reminders එක්ක වැඩ කරන්න පුළුවන්.";
        if (has(q, "help","මට උදව්","udaw")) return "හරි 😄 mission, XP, streak, progress, reminder වගේ දෙයක් කියන්න.";
        if (has(q, "motivate me","motivate me","මාව motivate","mata motivate")) return "අද එක පොඩි step එකක් හරි complete කරමු. 🔥 Start කළාම momentum එක එනවා.";
        return null;
    }

    private static boolean has(String q, String... words) {
        for (String w : words) if (q.contains(w)) return true;
        return false;
    }
}
