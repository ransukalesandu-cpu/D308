package com.discipline309.app;

import java.util.Locale;

/**
 * Intent and action router for Maya's Android assistant.
 *
 * Keep this class deterministic: it only identifies an intended capability.
 * A matching action name is not proof that the action has been executed.
 */
public final class MayaToolRouter {
    public enum Tool { WEB_SEARCH, APP_STATE, NOTE, PHONE_CONTROL, CHAT }

    private MayaToolRouter() {}

    public static Tool route(String text) {
        String q = normalize(text);
        if (MayaAI.shouldWebSearchForTool(q)) return Tool.WEB_SEARCH;

        if (containsAny(q,
                "note", "notes", "note එක", "නෝට්", "මතක සටහන", "journal", "take a note", "save this", "remember this", "note ekak", "note ekak danna", "mathaka thiyaganna",
                "mission", "xp", "streak", "progress", "level", "day", "task",
                "achievement", "309", "මගේ xp", "මගේ streak",
                "plan", "short plan", "focus goal", "focus", "daily plan", "today plan", "what should i do next", "next task", "mage wada monawada", "ada plan eka",
                "මගේ plan", "ප්ලෑන්", "කාර්ය", "වැඩ ලිස්ට්", "අද වැඩ", "දවසේ වැඩ")) {
            return containsAny(q, "note", "notes", "note එක", "නෝට්", "මතක සටහන", "journal")
                    ? Tool.NOTE : Tool.APP_STATE;
        }

        if (containsAny(q,
                "torch", "flashlight", "flash", "ටෝච්",
                "volume", "sound", "ශබ්ද", "music", "play music", "pause music", "සින්දු",
                "call ", "call my", "කෝල්", "dnd", "do not disturb", "notification",
                "whatsapp", "wifi", "wi-fi", "වයිෆයි", "bluetooth", "බ්ලූටූත්",
                "brightness", "screen light", "alarm", "එලාම්", "ඇලම්",
                "timer", "ටයිමර්", "settings", "open settings", "සෙටින්ග්ස්",
                "open app", "launch app", "battery", "බැටරි", "battery status", "battery level", "battery eka kiyada", "battery eka balanna", "phone battery",
                "screenshot", "screen shot", "lock screen", "device info", "phone info", "mage phone eke details", "phone eke details", "open camera", "camera", "camera eka arinna", "open browser", "browser", "internet", "browser eka arinna", "open youtube", "youtube", "youtube eka arinna", "open maps", "maps", "maps eka arinna", "location", "app eka arinna", "app eka open karanna", "whatsapp eka arinna", "settings arinna")) {
            return Tool.PHONE_CONTROL;
        }
        return Tool.CHAT;
    }

    /** Stable action identifiers consumed by Maya's action handling layer. */
    public static String action(String text) {
        String q = normalize(text);

        if (containsAny(q, "alarm", "set alarm", "add alarm", "alarm එක",
                "alarm ekak", "alarm eka", "set an alarm", "remind me at",
                "reminder", "එලාම්", "ඇලම්", "alarm ekak danna", "alarm ekak hadanna", "alarm ekak set karanna", "hawasata alarm", "udeta alarm", "alarm eka danna"))
            return "ADD_ALARM";

        if (containsAny(q, "timer", "set timer", "start timer", "timer එක",
                "timer ekak", "ටයිමර්", "timer ekak danna", "timer ekak hadanna", "timer eka danna", "minutes walin timer", "thathpara timer"))
            return "SET_TIMER";

        if (containsAny(q, "start workout", "workout start", "ව්‍යායාම පටන්",
                "workout එක පටන්", "workout eka patan", "workout eka start karanna", "exercise patan ganna"))
            return "START_WORKOUT";

        if (containsAny(q, "complete task", "mark task done", "task done",
                "වැඩේ ඉවරයි", "task එක complete", "task eka iwara", "task eka complete"))
            return "COMPLETE_TASK";

        if (containsAny(q, "show progress", "my progress", "progress එක",
                "මගේ progress", "mage progress", "mage progress eka", "mage streak eka", "xp kiyada", "ada progress"))
            return "SHOW_PROGRESS";

        if (containsAny(q, "show notes", "list notes", "මගේ notes",
                "notes ටික", "mage notes", "notes pennanna", "mage note tika", "saved notes", "notes balanna"))
            return "LIST_NOTES";

        if (containsAny(q, "open settings", "settings open", "සෙටින්ග්ස් අරින්න",
                "settings arinna", "settings open karanna", "settings eka arinna", "phone settings open karanna"))
            return "OPEN_SETTINGS";

        return "NONE";
    }

    public static boolean requiresConfirmation(String action) {
        return "ADD_ALARM".equals(action)
                || "SET_TIMER".equals(action)
                || "COMPLETE_TASK".equals(action)
                || "START_WORKOUT".equals(action);
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

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean containsAny(String q, String... values) {
        for (String value : values) {
            if (q.contains(value)) return true;
        }
        return false;
    }
}
