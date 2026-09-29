package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyStore;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.File;

public final class MayaSecureStorage {
    private static final String MAYA="maya_ai";
    private static final String WEB="maya_web";

    private static SharedPreferences secure(Context c,String name){
        try{
            return createSecure(c,name);
        }catch(Exception first){
            resetSecure(c,name);
            try{
                return createSecure(c,name);
            }catch(Exception second){
                throw new IllegalStateException("Secure Maya storage unavailable",second);
            }
        }
    }

    private static SharedPreferences createSecure(Context c,String name)throws Exception{
        MasterKey key=new MasterKey.Builder(c)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(c,name,key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    private static void resetSecure(Context c,String name){
        try{
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            String alias=MasterKey.DEFAULT_MASTER_KEY_ALIAS;
            if(ks.containsAlias(alias)) ks.deleteEntry(alias);
        }catch(Exception ignored){}

        try{
            File prefs=new File(c.getApplicationInfo().dataDir,"shared_prefs/"+name+".xml");
            if(prefs.exists()) prefs.delete();
            File backup=new File(c.getApplicationInfo().dataDir,"shared_prefs/"+name+".xml.bak");
            if(backup.exists()) backup.delete();
        }catch(Exception ignored){}
    }

    private static void migrate(Context c,String name){
        SharedPreferences s=secure(c,name);
        if(s.getBoolean("_migration_done",false)) return;
        SharedPreferences legacy=c.getSharedPreferences(name,Context.MODE_PRIVATE);
        String secret=legacy.getString("api_key","");
        if(secret!=null&&!secret.isEmpty()){
            s.edit().putString("api_key",secret).putBoolean("_migration_done",true).apply();
        }else{
            s.edit().putBoolean("_migration_done",true).apply();
        }
        legacy.edit().remove("api_key").apply();
    }

    public static String getApiKey(Context c,String name){
        try{
            migrate(c,name);
            return secure(c,name).getString("api_key","").trim();
        }catch(Exception e){
            return "";
        }
    }

    public static SharedPreferences maya(Context c){return openMigrated(c,MAYA);}
    public static SharedPreferences web(Context c){return openMigrated(c,WEB);}

    public static void setApiKey(Context c,String name,String key){
        try{
            secure(c,name).edit().putString("api_key",key==null?"":key.trim()).apply();
        }catch(Exception ignored){}
    }

    private static SharedPreferences openMigrated(Context c,String name){
        try{
            migrate(c,name);
            return secure(c,name);
        }catch(Exception e){
            resetSecure(c,name);
            try{
                return createSecure(c,name);
            }catch(Exception second){
                throw new IllegalStateException("Secure Maya storage unavailable",second);
            }
        }
    }

    private MayaSecureStorage(){}
}
