package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SupabaseAccountManager {
    private static final String URL="https://ohyhoixzpenuodeqlnzi.supabase.co";
    private static final String KEY="sb_publishable_2d3twySH_g0xQtsqVniOxA_1McxPbje";
    private static final String PREF="supabase_account";
    private static final ExecutorService IO=Executors.newCachedThreadPool();

    public interface Callback { void done(boolean ok,String message); }

    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public static boolean loggedIn(Context c){return !p(c).getString("access_token","").isEmpty() && !p(c).getString("user_id","").isEmpty();}
    public static String userId(Context c){return p(c).getString("user_id","");}
    public static String displayName(Context c){return p(c).getString("display_name","");}
    public static String role(Context c){return p(c).getString("role","");}

    public static void signUp(Context c,String email,String password,Callback cb){
        IO.execute(()->{
            try{
                JSONObject body=new JSONObject().put("email",email).put("password",password);
                JSONObject r=request("POST","/auth/v1/signup",body,null);
                String id=r.optJSONObject("user")!=null?r.getJSONObject("user").optString("id"):r.optString("id");
                String token=r.optString("access_token","");
                if(token.isEmpty()) cb.done(true,"Account created. Check your email if verification is required, then sign in.");
                else {saveSession(c,r);cb.done(true,"Account created and signed in.");}
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void signIn(Context c,String email,String password,Callback cb){
        IO.execute(()->{
            try{
                JSONObject body=new JSONObject().put("email",email).put("password",password);
                JSONObject r=request("POST","/auth/v1/token?grant_type=password",body,null);
                saveSession(c,r);
                ensureProfile(c, null, "primary", null);
                cb.done(true,"Signed in.");
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void createPrimaryProfile(Context c,String name,Callback cb){
        IO.execute(()->{
            try{
                ensureProfile(c,name,"primary",null);
                cb.done(true,"Primary account ready.");
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void joinInvite(Context c,String code,Callback cb){
        IO.execute(()->{
            try{
                JSONObject body=new JSONObject().put("invite_code",code.trim().toUpperCase(Locale.US));
                JSONObject r=rpc("consume_account_invite",body,c);
                String primaryId=r.optString("","");
                p(c).edit().putString("role","sub").putString("parent_id",primaryId).apply();
                cb.done(true,"Joined the Primary account.");
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void createInvite(Context c,Callback cb){
        IO.execute(()->{
            try{
                String uid=userId(c);
                if(uid.isEmpty())throw new IOException("Sign in first.");
                String code=randomCode();
                JSONObject body=new JSONObject().put("primary_id",uid).put("code",code);
                request("POST","/rest/v1/account_invites",body,c);
                cb.done(true,code);
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void loadLinked(Context c,Callback cb){
        IO.execute(()->{
            try{
                if(!loggedIn(c))throw new IOException("Sign in first.");
                String uid=userId(c);
                JSONArray profiles=requestArray("GET","/rest/v1/profiles?parent_id=eq."+URLEncoder.encode(uid,"UTF-8")+"&select=id,display_name,role,created_at",null,c);
                JSONArray out=new JSONArray();
                for(int i=0;i<profiles.length();i++){
                    JSONObject prof=profiles.getJSONObject(i);
                    String id=prof.optString("id");
                    JSONArray rows=requestArray("GET","/rest/v1/discipline_progress?user_id=eq."+URLEncoder.encode(id,"UTF-8")+"&select=snapshot,updated_at",null,c);
                    JSONObject item=new JSONObject(prof.toString());
                    if(rows.length()>0){
                        JSONObject row=rows.getJSONObject(0);
                        item.put("snapshot",row.optJSONObject("snapshot"));
                        item.put("updated_at",row.optString("updated_at"));
                    }
                    out.put(item);
                }
                cb.done(true,out.toString());
            }catch(Exception e){cb.done(false,errorMessage(e));}
        });
    }

    public static void syncLocalProgress(Context c,Callback cb){
        if(!loggedIn(c)){if(cb!=null)cb.done(false,"Not signed in.");return;}
        IO.execute(()->{
            try{
                JSONObject s=buildSnapshot(c);
                JSONObject body=new JSONObject().put("user_id",userId(c)).put("snapshot",s).put("updated_at",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(new Date()));
                request("POST","/rest/v1/discipline_progress?on_conflict=user_id",body,c,"resolution=merge-duplicates,return=minimal");
                if(cb!=null)cb.done(true,"Progress synced.");
            }catch(Exception e){if(cb!=null)cb.done(false,errorMessage(e));}
        });
    }

    public static void signOut(Context c){
        p(c).edit().clear().apply();
    }

    private static void ensureProfile(Context c,String name,String role,String parentId)throws Exception{
        String uid=userId(c);
        JSONArray existing=requestArray("GET","/rest/v1/profiles?id=eq."+URLEncoder.encode(uid,"UTF-8")+"&select=id,display_name,role,parent_id",null,c);
        if(existing.length()>0){
            JSONObject x=existing.getJSONObject(0);
            p(c).edit().putString("display_name",x.optString("display_name","")).putString("role",x.optString("role","")).putString("parent_id",x.optString("parent_id","")).apply();
            return;
        }
        String display=(name==null||name.trim().isEmpty())?"309 User":name.trim();
        JSONObject body=new JSONObject().put("id",uid).put("display_name",display).put("role",role);
        if(parentId!=null)body.put("parent_id",parentId);
        request("POST","/rest/v1/profiles",body,c);
        p(c).edit().putString("display_name",display).putString("role",role).putString("parent_id",parentId==null?"":parentId).apply();
    }

    private static JSONObject buildSnapshot(Context c)throws Exception{
        SharedPreferences d=c.getSharedPreferences("discipline",Context.MODE_PRIVATE);
        Calendar start=Calendar.getInstance();start.setTimeInMillis(d.getLong("program_start",System.currentTimeMillis()));
        Calendar now=Calendar.getInstance();
        int day=Math.max(0,Math.min(309,(int)((now.getTimeInMillis()-start.getTimeInMillis())/86400000L)+1));
        int completed=0,totalTasks=0,best=0,run=0,totalCompletedTasks=0;
        Calendar cur=(Calendar)start.clone();
        SimpleDateFormat fmt=new SimpleDateFormat("yyyyMMdd",Locale.US);
        while(!cur.after(now)&&!cur.after((Calendar)start.clone())){
            cur.add(Calendar.DAY_OF_YEAR,1);
        }
        cur=(Calendar)start.clone();
        while(!cur.after(now)){
            String k=fmt.format(cur.getTime());
            if(d.getBoolean("done_"+k,false)){completed++;run++;best=Math.max(best,run);}else run=0;
            for(int i=0;i<6+d.getInt("custom_count",0);i++)if(d.getBoolean("task_"+i+"_"+k,false))totalCompletedTasks++;
            cur.add(Calendar.DAY_OF_YEAR,1);
        }
        totalTasks=6+d.getInt("custom_count",0);
        int todayCount=0;String today=fmt.format(new Date());
        for(int i=0;i<totalTasks;i++)if(d.getBoolean("task_"+i+"_"+today,false))todayCount++;
        int xp=completed*100+totalCompletedTasks*20+d.getInt("xp_bonus",0);
        JSONObject s=new JSONObject();
        s.put("day",day).put("completedDays",completed).put("currentStreak",d.getBoolean("done_"+today,false)?run:0)
         .put("bestStreak",best).put("totalCompletedTasks",totalCompletedTasks).put("todayTasks",todayCount)
         .put("totalTasks",totalTasks).put("xp",xp).put("level",xp/500+1)
         .put("todayMission",d.getString("mission_"+today,"")).put("missionCompleted",d.getBoolean("mission_"+today+"_done",false));
        return s;
    }

    private static String randomCode(){
        final String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";SecureRandom r=new SecureRandom();StringBuilder b=new StringBuilder();
        for(int i=0;i<8;i++)b.append(chars.charAt(r.nextInt(chars.length())));return b.toString();
    }

    private static void saveSession(Context c,JSONObject r)throws Exception{
        JSONObject u=r.optJSONObject("user");
        String uid=u!=null?u.optString("id",""):r.optString("user_id","");
        p(c).edit().putString("access_token",r.optString("access_token","")).putString("refresh_token",r.optString("refresh_token",""))
         .putString("user_id",uid).putLong("expires_at",System.currentTimeMillis()+r.optLong("expires_in",3600)*1000L).apply();
    }

    private static JSONObject request(String method,String path,JSONObject body,Context c)throws Exception{return request(method,path,body,c,null);}
    private static JSONObject request(String method,String path,JSONObject body,Context c,String prefer)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(URL+path).openConnection();h.setRequestMethod(method);h.setConnectTimeout(15000);h.setReadTimeout(20000);
        h.setRequestProperty("apikey",KEY);h.setRequestProperty("Accept","application/json");
        if(c!=null&&!p(c).getString("access_token","").isEmpty())h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));
        if(prefer!=null)h.setRequestProperty("Prefer",prefer);
        if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json");try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=h.getResponseCode();String text=read(h,code);
        if(code<200||code>=300)throw new IOException(text.isEmpty()?("HTTP "+code):text);
        return text.isEmpty()?new JSONObject():new JSONObject(text);
    }
    private static JSONArray requestArray(String method,String path,JSONObject body,Context c)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(URL+path).openConnection();h.setRequestMethod(method);h.setConnectTimeout(15000);h.setReadTimeout(20000);
        h.setRequestProperty("apikey",KEY);h.setRequestProperty("Accept","application/json");
        if(c!=null)h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));
        int code=h.getResponseCode();String text=read(h,code);
        if(code<200||code>=300)throw new IOException(text.isEmpty()?("HTTP "+code):text);
        return new JSONArray(text);
    }
    private static JSONObject rpc(String name,JSONObject body,Context c)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(URL+"/rest/v1/rpc/"+name).openConnection();h.setRequestMethod("POST");h.setConnectTimeout(15000);h.setReadTimeout(20000);
        h.setRequestProperty("apikey",KEY);h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));h.setRequestProperty("Content-Type","application/json");
        h.setDoOutput(true);try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=h.getResponseCode();String text=read(h,code);if(code<200||code>=300)throw new IOException(text);return text.isEmpty()?new JSONObject():new JSONObject(text);
    }
    private static String read(HttpURLConnection h,int code)throws Exception{
        InputStream in=code>=200&&code<400?h.getInputStream():h.getErrorStream();if(in==null)return "";
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);return b.toString();}
    }
    private static String errorMessage(Exception e){String s=e.getMessage();if(s==null||s.isEmpty())return "Network error.";try{JSONObject j=new JSONObject(s);if(j.has("msg"))return j.optString("msg");if(j.has("message"))return j.optString("message");if(j.has("error_description"))return j.optString("error_description");}catch(Exception ignored){}return s.length()>180?s.substring(0,180):s;}
}
