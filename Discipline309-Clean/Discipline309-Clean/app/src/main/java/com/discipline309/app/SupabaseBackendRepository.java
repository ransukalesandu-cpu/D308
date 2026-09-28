package com.discipline309.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Single data-access layer for the 309 Day backend.
 * UI screens should call this class instead of talking to Supabase directly.
 */
public final class SupabaseBackendRepository {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    public interface Callback { void done(boolean ok,String message,JSONObject data); }

    public SupabaseBackendRepository(Context c){context=c.getApplicationContext();}

    private boolean ready(){return SupabaseAccountManager.loggedIn(context);}

    public void getSettings(Callback cb){requestArray("GET","/rest/v1/user_settings?user_id=eq."+uid()+"&select=*",cb);}
    public void saveSettings(JSONObject settings,Callback cb){
        try{
            JSONObject body=new JSONObject().put("user_id",uid())
                .put("display",settings.optJSONObject("display")!=null?settings.optJSONObject("display"):new JSONObject())
                .put("modes",settings.optJSONObject("modes")!=null?settings.optJSONObject("modes"):new JSONObject())
                .put("ai",settings.optJSONObject("ai")!=null?settings.optJSONObject("ai"):new JSONObject())
                .put("privacy",settings.optJSONObject("privacy")!=null?settings.optJSONObject("privacy"):new JSONObject())
                .put("notifications",settings.optJSONObject("notifications")!=null?settings.optJSONObject("notifications"):new JSONObject())
                .put("updated_at",now());
            request("POST","/rest/v1/user_settings?on_conflict=user_id",body,cb,"resolution=merge-duplicates,return=minimal");
        }catch(Exception e){fail(cb,e);}
    }

    public void getHabits(Callback cb){requestArray("GET","/rest/v1/habits?owner_id=eq."+uid()+"&active=eq.true&order=position.asc",cb);}
    public void createHabit(String name,int position,Callback cb){
        try{request("POST","/rest/v1/habits",new JSONObject().put("owner_id",uid()).put("name",name).put("position",position),cb,"return=representation");}catch(Exception e){fail(cb,e);}
    }
    public void updateHabit(String id,String name,boolean active,int position,Callback cb){
        try{request("PATCH","/rest/v1/habits?id=eq."+enc(id),new JSONObject().put("name",name).put("active",active).put("position",position).put("updated_at",now()),cb,"return=minimal");}catch(Exception e){fail(cb,e);}
    }
    public void deleteHabit(String id,Callback cb){request("DELETE","/rest/v1/habits?id=eq."+enc(id),null,cb,null);}

    public void setHabitLog(String habitId,String day,boolean completed,Callback cb){
        try{request("POST","/rest/v1/habit_logs?on_conflict=habit_id,day",new JSONObject().put("habit_id",habitId).put("owner_id",uid()).put("day",day).put("completed",completed).put("updated_at",now()),cb,"resolution=merge-duplicates,return=minimal");}catch(Exception e){fail(cb,e);}
    }
    public void getHabitLogs(String from,String to,Callback cb){
        requestArray("GET","/rest/v1/habit_logs?owner_id=eq."+uid()+"&day=gte."+enc(from)+"&day=lte."+enc(to)+"&order=day.desc",cb);
    }

    public void getDailyPlan(String day,Callback cb){
        requestArray("GET","/rest/v1/daily_plans?owner_id=eq."+uid()+"&day=eq."+enc(day)+"&select=*,daily_plan_tasks(*)",cb);
    }
    public void saveDailyPlan(String day,String focus,Callback cb){
        try{request("POST","/rest/v1/daily_plans?on_conflict=owner_id,day",new JSONObject().put("owner_id",uid()).put("day",day).put("focus",focus==null?"":focus).put("updated_at",now()),cb,"resolution=merge-duplicates,return=representation");}catch(Exception e){fail(cb,e);}
    }
    public void createPlanTask(String planId,String title,String time,String priority,int position,Callback cb){
        try{request("POST","/rest/v1/daily_plan_tasks",new JSONObject().put("plan_id",planId).put("owner_id",uid()).put("title",title).put("task_time",time).put("priority",priority).put("position",position),cb,"return=representation");}catch(Exception e){fail(cb,e);}
    }
    public void setPlanTaskCompleted(String taskId,boolean completed,Callback cb){
        try{request("PATCH","/rest/v1/daily_plan_tasks?id=eq."+enc(taskId),new JSONObject().put("completed",completed).put("updated_at",now()),cb,"return=minimal");}catch(Exception e){fail(cb,e);}
    }

    public void getJournal(String day,Callback cb){requestArray("GET","/rest/v1/journals?owner_id=eq."+uid()+"&day=eq."+enc(day)+"&select=*",cb);}
    public void saveJournal(String day,String body,Callback cb){
        try{request("POST","/rest/v1/journals?on_conflict=owner_id,day",new JSONObject().put("owner_id",uid()).put("day",day).put("body",body==null?"":body).put("updated_at",now()),cb,"resolution=merge-duplicates,return=representation");}catch(Exception e){fail(cb,e);}
    }

    public void getMood(String day,Callback cb){requestArray("GET","/rest/v1/moods?owner_id=eq."+uid()+"&day=eq."+enc(day)+"&select=*&limit=1",cb);}
    public void getMoodHistory(String from,String to,Callback cb){requestArray("GET","/rest/v1/moods?owner_id=eq."+uid()+"&day=gte."+enc(from)+"&day=lte."+enc(to)+"&select=*&order=day.desc",cb);}
    public void saveMood(String day,int score,String mood,String note,Callback cb){try{JSONObject b=new JSONObject().put("owner_id",uid()).put("day",day).put("mood_score",score).put("mood",mood).put("note",note==null?"":note).put("updated_at",now());request("POST","/rest/v1/moods?on_conflict=owner_id,day",b,cb,"resolution=merge-duplicates,return=representation");}catch(Exception e){fail(cb,e);}}

    public void getReminders(Callback cb){requestArray("GET","/rest/v1/reminders?owner_id=eq."+uid()+"&order=hour.asc,minute.asc",cb);}
    public void saveReminder(String id,String title,int hour,int minute,boolean enabled,Callback cb){
        try{
            JSONObject b=new JSONObject().put("owner_id",uid()).put("title",title).put("hour",hour).put("minute",minute).put("enabled",enabled).put("updated_at",now());
            String path=id==null||id.isEmpty()?"/rest/v1/reminders":"/rest/v1/reminders?id=eq."+enc(id);
            request(id==null||id.isEmpty()?"POST":"PATCH",path,b,cb,"return=representation");
        }catch(Exception e){fail(cb,e);}
    }
    public void deleteReminder(String id,Callback cb){request("DELETE","/rest/v1/reminders?id=eq."+enc(id),null,cb,null);}

    public void getNotes(Callback cb){requestArray("GET","/rest/v1/notes?owner_id=eq."+uid()+"&select=*&order=updated_at.desc",cb);}
    public void saveNote(String id,String title,String body,String category,Callback cb){
        try{
            JSONObject b=new JSONObject().put("owner_id",uid()).put("title",title).put("body",body).put("category",category).put("updated_at",now());
            if(id==null||id.isEmpty()){b.put("id",UUID.randomUUID().toString());request("POST","/rest/v1/notes",b,cb,"return=representation");}
            else request("PATCH","/rest/v1/notes?id=eq."+enc(id),b,cb,"return=representation");
        }catch(Exception e){fail(cb,e);}
    }
    public void deleteNote(String id,Callback cb){request("DELETE","/rest/v1/notes?id=eq."+enc(id),null,cb,null);}

    private void request(String method,String path,JSONObject body,Callback cb){request(method,path,body,cb,null);}
    private void request(String method,String path,JSONObject body,Callback cb,String prefer){
        if(!ready()){fail(cb,new IllegalStateException("Not signed in."));return;}
        new Thread(()->{try{
            JSONObject data=SupabaseAccountManager.backendRequestObject(context,method,path,body,prefer);
            if(cb!=null)main.post(()->cb.done(true,"OK",data));
        }catch(Exception e){fail(cb,e);}}).start();
    }
    private void requestArray(String method,String path,Callback cb){
        if(!ready()){fail(cb,new IllegalStateException("Not signed in."));return;}
        new Thread(()->{try{
            JSONArray rows=SupabaseAccountManager.backendRequestArray(context,method,path);
            JSONObject wrapper=new JSONObject().put("rows",rows);
            if(cb!=null)main.post(()->cb.done(true,"OK",wrapper));
        }catch(Exception e){fail(cb,e);}}).start();
    }
    private void fail(Callback cb,Exception e){if(cb!=null)main.post(()->cb.done(false,e.getMessage()==null?"Backend error.":e.getMessage(),null));}
    private String uid(){return SupabaseAccountManager.userId(context);}
    private String enc(String s){try{return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8.toString());}catch(Exception e){return "";}}
    private String now(){return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",java.util.Locale.US).format(new java.util.Date());}
}