package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
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
    private static final String LAST_SNAPSHOT="progress_last_sync_snapshot";
    private static final String LAST_SYNC_AT="progress_last_sync_at";
    private static final String CONFLICT_SNAPSHOT="progress_last_conflict_snapshot";
    private static final ExecutorService IO=Executors.newCachedThreadPool();
    public interface Callback { void done(boolean ok,String message); }

    private static SharedPreferences p(Context c){
        try{
            MasterKey masterKey=new MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            SharedPreferences secure=EncryptedSharedPreferences.create(c,PREF,masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            migrateLegacySession(c,secure);
            return secure;
        }catch(Exception e){throw new IllegalStateException("Secure account storage unavailable",e);}
    }

    private static void migrateLegacySession(Context c,SharedPreferences secure){
        if(secure.getBoolean("_migration_done",false)) return;
        SharedPreferences legacy=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        SharedPreferences.Editor out=secure.edit();
        Map<String,?> all=legacy.getAll();
        for(Map.Entry<String,?> entry:all.entrySet()){
            String k=entry.getKey(); Object v=entry.getValue();
            if(v instanceof String) out.putString(k,(String)v);
            else if(v instanceof Boolean) out.putBoolean(k,(Boolean)v);
            else if(v instanceof Long) out.putLong(k,(Long)v);
            else if(v instanceof Integer) out.putInt(k,(Integer)v);
            else if(v instanceof Float) out.putFloat(k,(Float)v);
        }
        out.putBoolean("_migration_done",true).apply();
        legacy.edit().clear().apply();
    }
    public static boolean loggedIn(Context c){return !p(c).getString("access_token","").isEmpty()&&!p(c).getString("user_id","").isEmpty();}
    public static String userId(Context c){return p(c).getString("user_id","");}
    public static String displayName(Context c){return p(c).getString("display_name","");}
    public static String role(Context c){return p(c).getString("role","");}
    public static boolean can(Context c,String permission){
        if(!"sub".equals(role(c))) return true;
        return p(c).getBoolean(permission,true);
    }

    public static void signUp(Context c,String email,String password,Callback cb){
        IO.execute(()->{try{
            JSONObject body=new JSONObject().put("email",email).put("password",password);
            JSONObject r=request("POST","/auth/v1/signup",body,null);
            if(r.optString("access_token","").isEmpty()) cb.done(true,"Account created. Verify your email if required, then sign in.");
            else {saveSession(c,r);cb.done(true,"Account created and signed in.");}
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void signIn(Context c,String email,String password,Callback cb){
        IO.execute(()->{try{
            JSONObject body=new JSONObject().put("email",email).put("password",password);
            JSONObject r=request("POST","/auth/v1/token?grant_type=password",body,null);
            saveSession(c,r);loadExistingProfile(c);loadPermissions(c);cb.done(true,"Signed in.");
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void createPrimaryProfile(Context c,String name,Callback cb){
        IO.execute(()->{try{ensureProfile(c,name,"primary",null);cb.done(true,"Primary account ready.");}catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void joinInvite(Context c,String code,Callback cb){
        IO.execute(()->{try{
            String primaryId=rpcText("consume_account_invite",new JSONObject().put("invite_code",code.trim().toUpperCase(Locale.US)),c);
            p(c).edit().putString("role","sub").putString("parent_id",primaryId).apply();
            loadPermissions(c);cb.done(true,"Joined the Primary account.");
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void createInvite(Context c,Callback cb){
        IO.execute(()->{try{
            String uid=userId(c); if(uid.isEmpty()||!"primary".equals(role(c)))throw new IOException("Primary account required.");
            String code=randomCode();
            request("POST","/rest/v1/account_invites",new JSONObject().put("primary_id",uid).put("code",code),c);
            cb.done(true,code);
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void loadLinked(Context c,Callback cb){
        IO.execute(()->{try{
            if(!loggedIn(c)||!"primary".equals(role(c)))throw new IOException("Primary account required.");
            String uid=userId(c);
            JSONArray profiles=requestArray("GET","/rest/v1/profiles?parent_id=eq."+URLEncoder.encode(uid,"UTF-8")+"&role=eq.sub&select=id,display_name,role,created_at",c);
            JSONArray out=new JSONArray();
            for(int i=0;i<profiles.length();i++){
                JSONObject prof=profiles.getJSONObject(i),item=new JSONObject(prof.toString());
                String id=prof.optString("id");
                JSONArray rows=requestArray("GET","/rest/v1/discipline_progress?user_id=eq."+URLEncoder.encode(id,"UTF-8")+"&select=snapshot,updated_at",c);
                JSONArray perms=requestArray("GET","/rest/v1/sub_permissions?sub_user_id=eq."+URLEncoder.encode(id,"UTF-8")+"&select=can_view_progress,can_edit_habits,can_edit_mission,can_reset_progress,can_use_maya,can_access_settings,can_sync_progress,can_manage_account,updated_at",c);
                if(rows.length()>0){JSONObject row=rows.getJSONObject(0);item.put("snapshot",row.optJSONObject("snapshot"));item.put("updated_at",row.optString("updated_at"));}
                if(perms.length()>0)item.put("permissions",perms.getJSONObject(0));
                out.put(item);
            }
            cb.done(true,out.toString());
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void setPermission(Context c,String subId,String column,boolean value,Callback cb){
        final Set<String> allowed=new HashSet<>(Arrays.asList("can_view_progress","can_edit_habits","can_edit_mission","can_reset_progress","can_use_maya","can_access_settings","can_sync_progress","can_manage_account"));
        if(!allowed.contains(column)){cb.done(false,"Invalid permission.");return;}
        IO.execute(()->{try{
            JSONObject body=new JSONObject().put("sub_user_id",subId).put(column,value);
            request("POST","/rest/v1/sub_permissions?on_conflict=sub_user_id",body,c,"resolution=merge-duplicates,return=minimal");
            cb.done(true,"Permission updated.");
        }catch(Exception e){cb.done(false,errorMessage(e));}});
    }

    public static void loadPermissions(Context c){
        if(!loggedIn(c)||!"sub".equals(role(c)))return;
        IO.execute(()->{try{
            JSONArray a=requestArray("GET","/rest/v1/sub_permissions?sub_user_id=eq."+URLEncoder.encode(userId(c),"UTF-8")+"&select=can_view_progress,can_edit_habits,can_edit_mission,can_reset_progress,can_use_maya,can_access_settings,can_sync_progress,can_manage_account",c);
            if(a.length()>0){
                JSONObject x=a.getJSONObject(0);SharedPreferences.Editor e=p(c).edit();
                Iterator<String> it=x.keys();while(it.hasNext()){String k=it.next();if(x.opt(k) instanceof Boolean)e.putBoolean(k,x.optBoolean(k));}e.apply();
            }
        }catch(Exception ignored){}}); 
    }

    public static void syncLocalProgress(Context c,Callback cb){
        if(!loggedIn(c)){if(cb!=null)cb.done(false,"Not signed in.");return;}
        if(!can(c,"can_sync_progress")){if(cb!=null)cb.done(false,"Primary disabled progress sync.");return;}
        IO.execute(()->{try{
            JSONObject local=buildSnapshot(c);
            String localText=local.toString();
            SharedPreferences prefs=p(c);
            String baseline=prefs.getString(LAST_SNAPSHOT,"");
            long lastSync=prefs.getLong(LAST_SYNC_AT,0L);

            JSONArray rows=requestArray("GET","/rest/v1/discipline_progress?user_id=eq."+URLEncoder.encode(userId(c),"UTF-8")+"&select=snapshot,updated_at",c);
            JSONObject remote=rows.length()>0?rows.getJSONObject(0):null;
            String remoteText=remote==null||remote.optJSONObject("snapshot")==null?"":remote.optJSONObject("snapshot").toString();
            long remoteAt=parseIsoMillis(remote==null?"":remote.optString("updated_at"));

            boolean localChanged=!baseline.isEmpty()&&!localText.equals(baseline);
            boolean remoteChanged=!baseline.isEmpty()&&!remoteText.equals(baseline);
            boolean conflict=localChanged&&remoteChanged&&remoteAt>lastSync;

            if(conflict){
                prefs.edit().putString(CONFLICT_SNAPSHOT,remoteText).apply();
            }

            String now=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(new Date());
            JSONObject body=new JSONObject().put("user_id",userId(c)).put("snapshot",local).put("updated_at",now);
            request("POST","/rest/v1/discipline_progress?on_conflict=user_id",body,c,"resolution=merge-duplicates,return=minimal");

            prefs.edit().putString(LAST_SNAPSHOT,localText).putLong(LAST_SYNC_AT,System.currentTimeMillis()).apply();

            if(cb!=null)cb.done(true,conflict
                    ?"Progress synced. A newer remote version was detected and backed up locally before using this device's changes."
                    :"Progress synced.");
        }catch(Exception e){if(cb!=null)cb.done(false,errorMessage(e));}});
    }

    public static String lastConflictSnapshot(Context c){return p(c).getString(CONFLICT_SNAPSHOT,"");}
    public static void clearLastConflict(Context c){p(c).edit().remove(CONFLICT_SNAPSHOT).apply();}

    public static void signOut(Context c){p(c).edit().clear().apply();}

    private static long parseIsoMillis(String value){
        if(value==null||value.isEmpty())return 0L;
        try{return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",Locale.US).parse(value).getTime();}
        catch(Exception ignored){}
        try{return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).parse(value).getTime();}
        catch(Exception ignored){}
        return 0L;
    }

    private static void loadExistingProfile(Context c)throws Exception{
        JSONArray a=requestArray("GET","/rest/v1/profiles?id=eq."+URLEncoder.encode(userId(c),"UTF-8")+"&select=id,display_name,role,parent_id",c);
        if(a.length()>0){JSONObject x=a.getJSONObject(0);p(c).edit().putString("display_name",x.optString("display_name","")).putString("role",x.optString("role","")).putString("parent_id",x.optString("parent_id","")).apply();}
    }

    private static void ensureProfile(Context c,String name,String role,String parentId)throws Exception{
        String uid=userId(c);
        JSONArray a=requestArray("GET","/rest/v1/profiles?id=eq."+URLEncoder.encode(uid,"UTF-8")+"&select=id,display_name,role,parent_id",c);
        if(a.length()>0){JSONObject x=a.getJSONObject(0);p(c).edit().putString("display_name",x.optString("display_name","")).putString("role",x.optString("role","")).putString("parent_id",x.optString("parent_id","")).apply();return;}
        String display=(name==null||name.trim().isEmpty())?"309 User":name.trim();
        JSONObject body=new JSONObject().put("id",uid).put("display_name",display).put("role",role);
        if(parentId!=null)body.put("parent_id",parentId);
        request("POST","/rest/v1/profiles",body,c);
        p(c).edit().putString("display_name",display).putString("role",role).putString("parent_id",parentId==null?"":parentId).apply();
    }

    private static JSONObject buildSnapshot(Context c)throws Exception{
        SharedPreferences d=c.getSharedPreferences("discipline",Context.MODE_PRIVATE);
        Calendar start=Calendar.getInstance();start.setTimeInMillis(d.getLong("program_start",System.currentTimeMillis()));
        Calendar now=Calendar.getInstance();int day=Math.max(0,Math.min(309,(int)((now.getTimeInMillis()-start.getTimeInMillis())/86400000L)+1));
        int completed=0,best=0,run=0,totalCompletedTasks=0;SimpleDateFormat fmt=new SimpleDateFormat("yyyyMMdd",Locale.US);Calendar cur=(Calendar)start.clone();
        while(!cur.after(now)){String k=fmt.format(cur.getTime());if(d.getBoolean("done_"+k,false)){completed++;run++;best=Math.max(best,run);}else run=0;for(int i=0;i<6+d.getInt("custom_count",0);i++)if(d.getBoolean("task_"+i+"_"+k,false))totalCompletedTasks++;cur.add(Calendar.DAY_OF_YEAR,1);}
        int totalTasks=6+d.getInt("custom_count",0),todayCount=0;String today=fmt.format(new Date());for(int i=0;i<totalTasks;i++)if(d.getBoolean("task_"+i+"_"+today,false))todayCount++;
        int xp=completed*100+totalCompletedTasks*20+d.getInt("xp_bonus",0);
        return new JSONObject().put("day",day).put("completedDays",completed).put("currentStreak",d.getBoolean("done_"+today,false)?run:0).put("bestStreak",best).put("totalCompletedTasks",totalCompletedTasks).put("todayTasks",todayCount).put("totalTasks",totalTasks).put("xp",xp).put("level",xp/500+1).put("todayMission",d.getString("mission_"+today,"")).put("missionCompleted",d.getBoolean("mission_"+today+"_done",false));
    }

    private static String randomCode(){String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";SecureRandom r=new SecureRandom();StringBuilder b=new StringBuilder();for(int i=0;i<8;i++)b.append(chars.charAt(r.nextInt(chars.length())));return b.toString();}

    private static void saveSession(Context c,JSONObject r)throws Exception{
        JSONObject u=r.optJSONObject("user");String uid=u!=null?u.optString("id",""):r.optString("user_id","");
        p(c).edit().putString("access_token",r.optString("access_token","")).putString("refresh_token",r.optString("refresh_token","")).putString("user_id",uid).putLong("expires_at",System.currentTimeMillis()+r.optLong("expires_in",3600)*1000L).apply();
    }

    private static void refresh(Context c)throws Exception{
        String rt=p(c).getString("refresh_token","");if(rt.isEmpty())return;
        JSONObject r=requestRaw("POST","/auth/v1/token?grant_type=refresh_token",new JSONObject().put("refresh_token",rt),null,false);
        saveSession(c,r);
    }

    private static JSONObject request(String method,String path,JSONObject body,Context c)throws Exception{return request(method,path,body,c,null);}
    private static JSONObject request(String method,String path,JSONObject body,Context c,String prefer)throws Exception{
        if(c!=null&&!p(c).getString("access_token","").isEmpty()&&p(c).getLong("expires_at",0)<System.currentTimeMillis()+60000)refresh(c);
        return requestRaw(method,path,body,c,true,prefer);
    }
    private static JSONObject requestRaw(String method,String path,JSONObject body,Context c,boolean auth)throws Exception{return requestRaw(method,path,body,c,auth,null);}
    private static JSONObject requestRaw(String method,String path,JSONObject body,Context c,boolean auth,String prefer)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(URL+path).openConnection();h.setRequestMethod(method);h.setConnectTimeout(15000);h.setReadTimeout(20000);h.setRequestProperty("apikey",KEY);h.setRequestProperty("Accept","application/json");
        if(auth&&c!=null&&!p(c).getString("access_token","").isEmpty())h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));
        if(prefer!=null)h.setRequestProperty("Prefer",prefer);
        if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json");try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=h.getResponseCode();String txt=read(h,code);if(code<200||code>=300)throw new IOException(txt.isEmpty()?"HTTP "+code:txt);return txt.isEmpty()?new JSONObject():new JSONObject(txt);
    }
    private static JSONArray requestArray(String method,String path,Context c)throws Exception{
        if(c!=null&&!p(c).getString("access_token","").isEmpty()&&p(c).getLong("expires_at",0)<System.currentTimeMillis()+60000)refresh(c);
        HttpURLConnection h=(HttpURLConnection)new URL(URL+path).openConnection();h.setRequestMethod(method);h.setConnectTimeout(15000);h.setReadTimeout(20000);h.setRequestProperty("apikey",KEY);h.setRequestProperty("Accept","application/json");if(c!=null)h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));
        int code=h.getResponseCode();String txt=read(h,code);if(code<200||code>=300)throw new IOException(txt.isEmpty()?"HTTP "+code:txt);return new JSONArray(txt);
    }
    private static String rpcText(String name,JSONObject body,Context c)throws Exception{
        if(c!=null&&!p(c).getString("access_token","").isEmpty()&&p(c).getLong("expires_at",0)<System.currentTimeMillis()+60000)refresh(c);
        HttpURLConnection h=(HttpURLConnection)new URL(URL+"/rest/v1/rpc/"+name).openConnection();h.setRequestMethod("POST");h.setConnectTimeout(15000);h.setReadTimeout(20000);h.setRequestProperty("apikey",KEY);h.setRequestProperty("Authorization","Bearer "+p(c).getString("access_token",""));h.setRequestProperty("Content-Type","application/json");h.setDoOutput(true);try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=h.getResponseCode();String txt=read(h,code);if(code<200||code>=300)throw new IOException(txt.isEmpty()?"HTTP "+code:txt);if(txt.startsWith("\"")&&txt.endsWith("\""))return new JSONArray("["+txt+"]").getString(0);return txt;
    }
    private static String read(HttpURLConnection h,int code)throws Exception{InputStream in=code>=200&&code<400?h.getInputStream():h.getErrorStream();if(in==null)return "";try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);return b.toString();}}
    private static String errorMessage(Exception e){String s=e.getMessage();if(s==null||s.isEmpty())return "Network error.";try{JSONObject j=new JSONObject(s);if(j.has("msg"))return j.optString("msg");if(j.has("message"))return j.optString("message");if(j.has("error_description"))return j.optString("error_description");}catch(Exception ignored){}return s.length()>180?s.substring(0,180):s;}
}