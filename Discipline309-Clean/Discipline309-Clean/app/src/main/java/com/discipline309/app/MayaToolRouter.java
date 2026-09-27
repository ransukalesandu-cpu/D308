package com.discipline309.app;

import android.content.Context;
import java.util.Locale;

/**
 * Small intent router for Maya. It decides which app capability should handle
 * a request before the real AI is called.
 */
public final class MayaToolRouter {
    public enum Tool { WEB_SEARCH, APP_STATE, PHONE_CONTROL, CHAT }

    private MayaToolRouter() {}

    public static Tool route(String text) {
        String q = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (MayaAI.shouldWebSearchForTool(q)) return Tool.WEB_SEARCH;

        if (containsAny(q, "mission", "xp", "streak", "progress", "level", "day", "task",
                "achievement", "journal", "309", "mission එක", "මගේ xp", "මගේ streak",
                "plan", "short plan", "focus goal", "focus", "daily plan", "today plan",
                "මගේ plan", "ප්ලෑන්", "කාර්ය", "වැඩ ලිස්ට්", "අද වැඩ", "දවසේ වැඩ")) {
            return Tool.APP_STATE;
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

    public static String describe(Tool tool) {
        switch (tool) {
            case WEB_SEARCH: return "Use current web-search results.";
            case APP_STATE: return "Use live 309 Day Discipline app state.";
            case PHONE_CONTROL: return "Use an available phone-control command; never claim success unless executed.";
            default: return "Answer normally with the AI brain.";
        }
    }

    private static boolean containsAny(String q, String... values) {
        for (String value : values) if (q.contains(value)) return true;
        return false;
    }
}
