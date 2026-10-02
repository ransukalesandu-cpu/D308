package com.discipline309.app;

import android.content.Context;
import java.util.Locale;

/**
 * Small intent router for Maya. It decides which app capability should handle
 * a request before the real AI is called.
 */
public final class MayaToolRouter {
    public enum Tool { WEB_SEARCH, APP_STATE, NOTE, PHONE_CONTROL, CHAT }

    private MayaToolRouter() {}

    public static Tool route(String text) {
        String q = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (MayaAI.shouldWebSearchForTool(q)) return Tool.WEB_SEARCH;

        if (containsAny(q, "note", "notes", "note එක", "නෝට්", "මතක සටහන", "mission", "xp", "streak", "progress", "level", "day", "task",
                "achievement", "journal", "309", "mission එක", "මගේ xp", "මගේ streak",
                "plan", "short plan", "focus goal", "focus", "daily plan", "today plan",
                "මගේ plan", "ප්ලෑන්", "කාර්ය", "වැඩ ලිස්ට්", "අද වැඩ", "දවසේ වැඩ")) {
            return containsAny(q, "note", "notes", "note එක", "නෝට්", "මතක සටහන") ? Tool.NOTE : Tool.APP_STATE;
        }

        if (containsAny(q, "torch", "flashlight", "flash", "ටෝච්", "volume", "sound", "ශබ්ද",
                "music", "play music", "pause music", "සින්දු", "call ", "call my", "කෝල්",
                "dnd", "do not disturb", "notification", "whatsapp", "wifi", "wi-fi", "වයිෆයි",
                "bluetooth", "බ්ලූටූත්", "brightness", "screen light", "alarm", "timer", "ටൈമർ",
                "settings", "open settings", "සെറ്റിംഗ്സ്")) {
            return Tool.PHONE_CONTROL;
        }
        return Tool.CHAT;
    }


    /** Returns a stable action name for safe, app-exposed commands. */
    public static String action(String text) {
        String q = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (containsAny(q, "alarm", "alarm එක", "alarm ekak", "alarm eka", "reminder", "ඇලම්", "එලාම්")) return "ADD_ALARM";
        if (containsAny(q, "timer", "timer එක", "timer ekak", "ටයිමර්", "ටයිමර් එක")) return "SET_TIMER";
        if (containsAny(q, "start workout", "workout start", "ව්‍යායාම පටන්", "workout එක පටන්")) return "START_WORKOUT";
        if (containsAny(q, "complete task", "mark task done", "task done", "වැඩේ ඉවරයි", "task එක complete")) return "COMPLETE_TASK";
        if (containsAny(q, "show progress", "my progress", "progress එක", "මගේ progress")) return "SHOW_PROGRESS";
        if (containsAny(q, "show notes", "list notes", "මගේ notes", "notes ටික")) return "LIST_NOTES";
        if (containsAny(q, "open settings", "settings open", "සෙටින්ග්ස් අරින්න")) return "OPEN_SETTINGS";
        return "NONE";
    }

    public static boolean requiresConfirmation(String action) {
        return "ADD_ALARM".equals(action) || "SET_TIMER".equals(action) || "COMPLETE_TASK".equals(action) || "START_WORKOUT".equals(action);
    }

    public static String describe(Tool tool) {
        switch (tool) {
            case WEB_SEARCH: return "Use current web-search results.";
            case APP_STATE: return "Use live 309 Day Discipline app state.";
            case NOTE: return "Use the local Notes feature to create, list, read, or delete notes.";
            case PHONE_CONTROL: return "Use an available phone-control command; never claim success unless executed.";
            default: return "Answer normally with the AI brain.";
        }
    }

    private static boolean containsAny(String q, String... values) {
        for (String value : values) if (q.contains(value)) return true;
        return false;
    }
}
