package com.discipline309.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MayaVision {
    public interface Callback { void onReply(String reply); }
    private MayaVision(){}

    public static void analyze(Context context, Uri imageUri, String question, Callback callback) {
        new Thread(() -> {
            try {
                if(callback==null) return;
                android.content.SharedPreferences p=MayaSecureStorage.maya(context);
                String key=p.getString("api_key","").trim();
                if(key.isEmpty()){ callback.onReply("Image understand කරන්න AI API key එක Settings වල add කරන්න. 🖼️"); return; }

                Bitmap bitmap=BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri));
                if(bitmap==null){ callback.onReply("Image එක read කරන්න බැරි වුණා."); return; }

                int max=1280;
                float scale=Math.min(1f, max/(float)Math.max(bitmap.getWidth(),bitmap.getHeight()));
                if(scale<1f) bitmap=Bitmap.createScaledBitmap(bitmap,Math.max(1,(int)(bitmap.getWidth()*scale)),Math.max(1,(int)(bitmap.getHeight()*scale)),true);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,82,baos);
                String data=Base64.encodeToString(baos.toByteArray(),Base64.NO_WRAP);

                String endpoint=p.getString("endpoint","https://api.openai.com/v1/chat/completions").trim();
                String model=p.getString("model","gpt-5-mini").trim();
                JSONObject body=new JSONObject();
                body.put("model",model);
                body.put("temperature",0.5);
                JSONArray messages=new JSONArray();
                JSONObject system=new JSONObject();
                system.put("role","system");
                system.put("content","You are Maya, a helpful multimodal assistant. Understand Sinhala, Singlish and English. Describe and analyze the provided image accurately. Do not invent details you cannot see. Keep voice-friendly answers concise.");
                messages.put(system);
                JSONObject user=new JSONObject(); user.put("role","user");
                JSONArray content=new JSONArray();
                JSONObject text=new JSONObject(); text.put("type","text"); text.put("text",question==null||question.trim().isEmpty()?"Describe this image and tell me what is important about it.":question); content.put(text);
                JSONObject img=new JSONObject(); img.put("type","image_url");
                JSONObject imageUrl=new JSONObject(); imageUrl.put("url","data:image/jpeg;base64,"+data); img.put("image_url",imageUrl);
                content.put(img);
                user.put("content",content); messages.put(user);
                body.put("messages",messages);

                HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
                c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(45000); c.setDoOutput(true);
                c.setRequestProperty("Authorization","Bearer "+key);
                c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                c.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
                int code=c.getResponseCode();
                InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                String response=read(stream);
                if(code<200||code>=300){ callback.onReply("Image AI response එක ගන්න බැරි වුණා. Vision-support කරන model එකක් Settings වල select කරලා බලන්න."); return; }
                JSONObject json=new JSONObject(response);
                JSONArray choices=json.optJSONArray("choices");
                String reply="";
                if(choices!=null&&choices.length()>0){
                    JSONObject msg=choices.getJSONObject(0).optJSONObject("message");
                    if(msg!=null) reply=msg.optString("content","").trim();
                }
                callback.onReply(reply.isEmpty()?"Image එක ගැන reply එකක් හදාගන්න බැරි වුණා 😅":reply);
                c.disconnect();
            }catch(Exception e){ if(callback!=null) callback.onReply("Image analyze කරන්න බැරි වුණා. Image එක සහ AI settings check කරන්න. 🌐"); }
        }).start();
    }

    public static String documentText(Context context, Uri uri) {
        try {
            String type=context.getContentResolver().getType(uri);
            if(type!=null && (type.startsWith("text/") || type.equals("application/json"))) {
                InputStream in=context.getContentResolver().openInputStream(uri);
                byte[] b=readBytes(in,400000);
                return new String(b,StandardCharsets.UTF_8);
            }
        }catch(Exception ignored){}
        return "";
    }

    private static byte[] readBytes(InputStream in,int max)throws Exception{
        if(in==null)return new byte[0];
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        byte[] buf=new byte[8192];int n,total=0;
        while((n=in.read(buf))!=-1 && total<max){int take=Math.min(n,max-total);out.write(buf,0,take);total+=take;if(take<n)break;}
        in.close();return out.toByteArray();
    }
    private static String read(InputStream in)throws Exception{
        if(in==null)return "";
        java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);r.close();return b.toString();
    }
}
