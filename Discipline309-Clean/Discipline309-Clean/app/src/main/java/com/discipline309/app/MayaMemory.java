package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MayaMemory {
    private static final String PREFS = "maya_memory";
    private static final String KEY = "items";

    private final SharedPreferences prefs;
    private final MayaMemoryDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MayaMemory(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        dao = MayaDatabase.get(app).mayaMemoryDao();
        migrateLegacyIfNeeded();
    }

    public void remember(String fact) {
        if (fact == null) return;
        fact = fact.trim();
        if (fact.isEmpty() || fact.length() > 300) return;
        final String value = fact;
        try{executor.execute(() -> {try{if (dao.search(value).isEmpty()) dao.insert(new MayaMemoryEntity(value, System.currentTimeMillis()));}catch(Exception ignored){}});}catch(Exception ignored){}
    }

    public String relevant(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<MayaMemoryEntity> all; try{all=dao.getAll();}catch(Exception e){return "";}
        if (all.isEmpty()) return "";
        if (q.isEmpty()) return format(all);

        StringBuilder out = new StringBuilder();
        for (MayaMemoryEntity item : all) {
            String fact = item.fact == null ? "" : item.fact;
            boolean hit = false;
            for (String token : q.split("\\s+")) {
                if (token.length() >= 3 && fact.toLowerCase(Locale.ROOT).contains(token)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                if (out.length() > 0) out.append("\n");
                out.append("• ").append(fact);
            }
        }
        return out.toString();
    }

    public int count() { try{return dao.count();}catch(Exception e){return 0;} }

    public String all() { try{return format(dao.getAll());}catch(Exception e){return "";} }

    public void remove(int index) {
        remove(index,null);
    }

    public void remove(int index,Runnable onDone) {
        try{executor.execute(() -> {
            try{
                List<MayaMemoryEntity> all=dao.getAll();
                if(index<0||index>=all.size())return;
                dao.delete(all.get(index));
            }catch(Exception ignored){} 
            finally{if(onDone!=null)try{onDone.run();}catch(Exception ignored){}}
        });}catch(Exception ignored){}
    }

    public void clear() {
        clear(null);
    }

    public void clear(Runnable onDone) {
        try{executor.execute(() -> {
            try{dao.clear();}catch(Exception ignored){}
            finally{if(onDone!=null)try{onDone.run();}catch(Exception ignored){}}
        });}catch(Exception ignored){}
    }

    private String format(List<MayaMemoryEntity> items) {
        StringBuilder out = new StringBuilder();
        for (MayaMemoryEntity item : items) {
            if (out.length() > 0) out.append("\n");
            out.append("• ").append(item.fact == null ? "" : item.fact);
        }
        return out.toString();
    }

    private static final class ScoredMemory {
        final String fact; final int score; final long createdAt;
        ScoredMemory(String fact, int score, long createdAt) { this.fact=fact; this.score=score; this.createdAt=createdAt; }
    }

    private void migrateLegacyIfNeeded() {
        try{if (dao.count() > 0) return;}catch(Exception ignored){return;}
        String raw; try{raw=prefs.getString(KEY, "[]");}catch(Exception e){return;}
        try {
            JSONArray a = new JSONArray(raw);
            List<MayaMemoryEntity> migrated = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                String fact = a.optString(i, "").trim();
                if (!fact.isEmpty()) migrated.add(new MayaMemoryEntity(fact, System.currentTimeMillis()));
            }
            if (!migrated.isEmpty()) {
                try{executor.execute(() -> {try{for (MayaMemoryEntity item : migrated) if (dao.search(item.fact).isEmpty()) dao.insert(item);}catch(Exception ignored){}});}catch(Exception ignored){}
            }
        } catch (JSONException ignored) {
        }
    }
}
