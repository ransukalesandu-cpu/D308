package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class WebSearch {
    public interface Callback { void onResult(String text); }

    public static void search(Context context, String query, Callback callback){
        new Thread(() -> {
            try{
                SharedPreferences p=context.getSharedPreferences("maya_web",Context.MODE_PRIVATE);
                String key=p.getString("api_key","").trim();
                if(key.isEmpty()){ callback.onResult(""); return; }

                JSONObject body=new JSONObject();
                body.put("api_key",key);
                body.put("query",query);
                body.put("search_depth","basic");
                body.put("include_answer",true);
                body.put("max_results",5);

                HttpURLConnection c=(HttpURLConnection)new URL("https://api.tavily.com/search").openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(12000);
                c.setReadTimeout(20000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                c.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));

                int code=c.getResponseCode();
                InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                String response=read(stream);
                if(code<200||code>=300){ callback.onResult(""); return; }

                JSONObject json=new JSONObject(response);
                StringBuilder out=new StringBuilder();
                String answer=json.optString("answer","").trim();
                if(!answer.isEmpty()) out.append("Tavily answer: ").append(answer).append("\n");

                JSONArray results=json.optJSONArray("results");
                if(results!=null){
                    for(int i=0;i<results.length()&&i<5;i++){
                        JSONObject r=results.optJSONObject(i);
                        if(r==null) continue;
                        String title=r.optString("title","").trim();
                        String url=r.optString("url","").trim();
                        String snippet=r.optString("content","").trim();
                        if(snippet.length()>700) snippet=snippet.substring(0,700);
                        out.append("SOURCE ").append(i+1).append(": ").append(title)
                           .append(" | ").append(url).append(" | ").append(snippet).append("\n");
                    }
                }
                callback.onResult(out.toString().trim());
                c.disconnect();
            }catch(Exception e){ callback.onResult(""); }
        }).start();
    }

    public static boolean enabled(Context context){
        return !context.getSharedPreferences("maya_web",Context.MODE_PRIVATE)
            .getString("api_key","").trim().isEmpty();
    }

    private static String read(InputStream in)throws Exception{
        if(in==null)return "";
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder(); String line;
        while((line=r.readLine())!=null)b.append(line);
        r.close(); return b.toString();
    }
}
