package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;

public class MayaMemory {
    private static final String PREFS = "maya_memory";
    private static final String KEY = "items";

    private final SharedPreferences prefs;

    public MayaMemory(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void remember(String fact) {
        if (fact == null) return;
        fact = fact.trim();
        if (fact.isEmpty() || fact.length() > 300) return;

        JSONArray old = read();
        for (int i = 0; i < old.length(); i++) {
            if (fact.equalsIgnoreCase(old.optString(i))) return;
        }
        old.put(fact);
        prefs.edit().putString(KEY, old.toString()).apply();
    }

    public String all() {
        JSONArray a = read();
        if (a.length() == 0) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < a.length(); i++) {
            if (i > 0) out.append("\n");
            out.append("• ").append(a.optString(i));
        }
        return out.toString();
    }

    public void remove(int index) {
        JSONArray a=read();
        if(index<0 || index>=a.length()) return;
        JSONArray out=new JSONArray();
        for(int i=0;i<a.length();i++) if(i!=index) out.put(a.optString(i));
        prefs.edit().putString(KEY,out.toString()).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY).apply();
    }

    private JSONArray read() {
        try {
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}
