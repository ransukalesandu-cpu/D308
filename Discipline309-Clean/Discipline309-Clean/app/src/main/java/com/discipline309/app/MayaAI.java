package com.discipline309.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MayaAI {
    public interface Callback { void onReply(String reply); }

    public static void ask(Context context, String userText, String memoryText, String personality, Callback callback){
        new Thread(() -> {
            try{
                SharedPreferences p=context.getSharedPreferences("maya_ai",Context.MODE_PRIVATE);
                String key=p.getString("api_key","").trim();
                if(key.isEmpty()){callback.onReply("Maya AI brain එක activate කරන්න Settings වල AI API key එක add කරන්න. 🧠");return;}
                String endpoint=p.getString("endpoint","https://api.openai.com/v1/chat/completions").trim();
                String model=p.getString("model","gpt-5-mini").trim();

                JSONObject body=new JSONObject();
                body.put("model",model);
                body.put("temperature",0.8);
                JSONArray messages=new JSONArray();

                JSONObject system=new JSONObject();
                system.put("role","system");
                system.put("content",
                    "You are Maya, a friendly personal AI assistant inside an Android app called 309 Day Discipline. " +
                    "Reply naturally and briefly. Understand Sinhala, Singlish (Sinhala typed in English letters), and English. " +
                    "Be funny when appropriate, supportive, respectful, and never pretend you performed a phone action unless the app actually did it. " +
                    "Do not ask for or store passwords, OTPs, PINs, card numbers, or other secrets. " +
                    "The user may be a teenager, so keep advice age-appropriate and safe. " +
                    "Personality mode: "+personality+". " +
                    "The LIVE APP STATE below is authoritative for the user's current discipline progress. " +
                    "Use it when answering questions about day number, remaining days, tasks, mission, mission completion, streaks, XP, level, milestones, achievements, or journal. " +
                    "Never invent a number when live state provides it. If the user asks for something the state does not contain, say so briefly. " +
                    "If asked for today's mission, give the exact mission from live state. " +
                    "If asked how they are doing, summarize the actual state and give one practical next step. " +
                    "LIVE APP STATE: "+memoryText);
                messages.put(system);

                JSONObject user=new JSONObject();
                user.put("role","user");
                user.put("content",userText);
                messages.put(user);
                body.put("messages",messages);

                HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setDoOutput(true);
                c.setRequestProperty("Authorization","Bearer "+key);
                c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                byte[] out=body.toString().getBytes(StandardCharsets.UTF_8);
                c.getOutputStream().write(out);

                int code=c.getResponseCode();
                InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                String response=read(stream);
                if(code<200||code>=300){
                    callback.onReply("AI response එක ගන්න බැරි වුණා. API key/model එක Settings වල check කරන්න.");
                    return;
                }
                JSONObject json=new JSONObject(response);
                JSONArray choices=json.optJSONArray("choices");
                String reply="";
                if(choices!=null&&choices.length()>0){
                    JSONObject message=choices.getJSONObject(0).optJSONObject("message");
                    if(message!=null) reply=message.optString("content","").trim();
                }
                callback.onReply(reply.isEmpty()?"Mayaට ඒකට reply එකක් හදාගන්න බැරි වුණා 😅":reply);
                c.disconnect();
            }catch(Exception e){
                callback.onReply("AI connection එකට connect වෙන්න බැරි වුණා. Internet එක සහ API settings check කරන්න. 🌐");
            }
        }).start();
    }

    private static String read(InputStream in)throws Exception{
        if(in==null)return "";
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();String line;
        while((line=r.readLine())!=null)b.append(line);
        r.close();return b.toString();
    }
}
