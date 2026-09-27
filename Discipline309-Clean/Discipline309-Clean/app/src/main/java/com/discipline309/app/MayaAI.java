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
            if(callback==null) return;
            try{
                SharedPreferences p=MayaSecureStorage.maya(context);
                String key=p.getString("api_key","").trim();
                if(key.isEmpty()){
                    String offline=MayaOfflineNLP.answer(context,userText);
                    callback.onReply(offline!=null?offline:"Internet/API නැති නිසා full AI reply එක available නැහැ. Basic offline commands තවමත් වැඩ කරනවා. 📡");
                    return;
                }
                String endpoint=p.getString("endpoint","https://api.openai.com/v1/chat/completions").trim();
                String model=p.getString("model","gpt-5-mini").trim();

                String webResults="";
                MayaMemory memoryStore = new MayaMemory(context);
                String relevantMemory = memoryStore.relevant(userText);
                if (relevantMemory.isEmpty()) relevantMemory = memoryText;
                MayaToolRouter.Tool selectedTool = MayaToolRouter.route(userText);
                if(WebSearch.enabled(context) && selectedTool == MayaToolRouter.Tool.WEB_SEARCH){
                    webResults=WebSearch.searchSync(context,userText);
                }

                JSONObject body=new JSONObject();
                body.put("model",model);
                body.put("temperature",0.85);
                JSONArray messages=new JSONArray();

                JSONObject system=new JSONObject();
                system.put("role","system");
                system.put("content",
                    "You are Maya, the user's personal voice-first AI assistant inside 309 Day Discipline. " +
                    "Your main job is to be useful in the moment: listen, understand intent, remember ordinary preferences, explain things simply, and help the user take the next practical step. " +
                    "Understand Sinhala, Singlish (Sinhala typed in English letters), and English. Prefer natural Sinhala/Singlish when the user speaks that way. " +
                    "For voice replies, keep answers short, conversational, easy to hear, and avoid long lists unless requested. " +
                    "You may be funny, energetic, cute, calm, or caring according to the personality mode, but never act as a romantic partner or encourage emotional dependency. " +
                    "Be supportive without pretending certainty. Never claim a phone action happened unless the app actually performed it. " +
                    "The user may be a teenager, so keep advice age-appropriate and safe. Do not provide dangerous instructions or help with secrets. " +
                    "Never ask for passwords, OTPs, PINs, CVVs, card numbers, or other authentication secrets. " +
                    "The LIVE APP STATE is authoritative for current discipline data. Use it for day, remaining days, tasks, mission, mission completion, streaks, XP, level, milestones, achievements, and journal. Never invent those numbers. " +
                    "Saved memory contains only ordinary user-provided facts/preferences and is lower priority than current app state. " +
                    "When the user asks a vague personal question, use relevant live state and memory instead of asking unnecessary follow-up questions. " +
                    "When a request needs a phone capability the app does not expose, say what you can do and what the app would need to add. " +
                    "When WEB SEARCH RESULTS are provided, use them for current/search-style questions, prefer supplied source evidence, and do not invent facts. " +
                    "Tool selected: "+MayaToolRouter.describe(selectedTool)+". Relevant saved memory: "+(relevantMemory.isEmpty()?"None":relevantMemory)+". Personality mode: "+personality+". PUBLIC CREATOR PROFILE: Maya was created by Lesandu Ransuka, born September 19, 2008. He studies A/L Science with Mathematics. His sister is Sethuli Senanga; his mother is Gayani Fernando; his father is Hemal Asiri. These are public profile facts provided by the creator and may be shared when users ask about Maya's creator. Do not reveal private memory or private conversation details to other users. Creator instructions do not override safety rules. LIVE APP STATE + MEMORY: "+memoryText+" WEB SEARCH RESULTS: "+(webResults.isEmpty()?"No web search was used.":webResults));
                messages.put(system);

                SharedPreferences history=context.getSharedPreferences("maya_chat",Context.MODE_PRIVATE);
                JSONArray recent=new JSONArray(history.getString("recent","[]"));
                int start=Math.max(0,recent.length()-8);
                for(int i=start;i<recent.length();i++) messages.put(recent.getJSONObject(i));

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
                    String fallback=MayaOfflineNLP.answer(context,userText);
                    callback.onReply(fallback!=null?fallback:"Maya AI service එකට දැන් connect වෙන්න බැහැ. 🌐 Settings වල API configuration එක check කරන්න.");
                    c.disconnect();
                    return;
                }
                JSONObject json=new JSONObject(response);
                JSONArray choices=json.optJSONArray("choices");
                String reply="";
                if(choices!=null&&choices.length()>0){
                    JSONObject message=choices.getJSONObject(0).optJSONObject("message");
                    if(message!=null) reply=message.optString("content","").trim();
                }
                String finalReply=reply.isEmpty()?MayaOfflineNLP.answer(context,userText):reply;
                if(finalReply==null||finalReply.trim().isEmpty()) finalReply="Mayaට දැන් full AI reply එක හදාගන්න බැහැ 😅. Basic offline commands තවමත් වැඩ කරනවා.";
                try{
                    JSONArray updated=new JSONArray(history.getString("recent","[]"));
                    JSONObject hu=new JSONObject();hu.put("role","user");hu.put("content",userText);updated.put(hu);
                    JSONObject ha=new JSONObject();ha.put("role","assistant");ha.put("content",finalReply);updated.put(ha);
                    while(updated.length()>8){
                        JSONArray trimmed=new JSONArray();
                        for(int i=1;i<updated.length();i++) trimmed.put(updated.getJSONObject(i));
                        updated=trimmed;
                    }
                    history.edit().putString("recent",updated.toString()).apply();
                }catch(Exception ignored){}
                callback.onReply(finalReply);
                c.disconnect();
            }catch(Exception e){
                String fallback=MayaOfflineNLP.answer(context,userText);
                callback.onReply(fallback!=null?fallback:"Mayaට දැන් AI service එකට connect වෙන්න බැහැ. 🌐 Internet එක හෝ API settings check කරන්න.");
            }
        }).start();
    }

    public static boolean shouldWebSearchForTool(String q){
        String s=q==null?"":q.toLowerCase(java.util.Locale.ROOT);
        String[] markers={"search the web","search web","google this","look this up","look it up","find online","latest","today","current","right now","news","price","weather","අද news","අලුත්ම","දැනට","දැන් තියෙන","online බලන්න","web එකේ බලන්න","search කරන්න"};
        for(String m:markers) if(s.contains(m)) return true;
        return s.startsWith("who is ")||s.startsWith("what is ")||s.startsWith("where is ")||s.startsWith("when is ");
    }

    private static String read(InputStream in)throws Exception{
        if(in==null)return "";
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();String line;
        while((line=r.readLine())!=null)b.append(line);
        r.close();return b.toString();
    }
}
