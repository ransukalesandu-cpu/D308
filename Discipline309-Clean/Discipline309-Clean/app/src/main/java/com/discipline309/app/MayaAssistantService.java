package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MayaAssistantService extends Service {
    private static final int ID=3099;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ready=false, stopping=false;
    private Handler handler;
    private MayaMemory memory;

    @Override public void onCreate(){
        super.onCreate();
        memory=new MayaMemory(this);
        createChannel();
        Intent open=new Intent(this,SettingsActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"maya_assistant")
            .setContentTitle("Maya is active")
            .setContentText("Voice assistant is listening • tap to manage")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).setContentIntent(pi).build();
        startForeground(ID,n);
        handler=new Handler(Looper.getMainLooper());
        tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("si","LK"));tts.setSpeechRate(.92f);ready=true;}});
        handler.postDelayed(this::listen,700);
    }

    private void listen(){
        if(stopping || !ready || !SpeechRecognizer.isRecognitionAvailable(this)) return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){speak("Microphone permission එක allow කරන්න.");return;}
        if(recognizer!=null) recognizer.destroy();
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){}
            public void onBeginningOfSpeech(){}
            public void onRmsChanged(float r){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){}
            public void onPartialResults(Bundle b){}
            public void onEvent(int t,Bundle b){}
            public void onError(int e){restart(900);}
            public void onResults(Bundle b){
                ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String s=m==null||m.isEmpty()?"":m.get(0);
                handle(s); restart(1300);
            }
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"si-LK");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"si-LK");
        recognizer.startListening(i);
    }
    private void restart(long d){if(handler!=null)handler.postDelayed(this::listen,d);}

    private void handle(String raw){
        String s=raw==null?"":raw.trim();
        String l=s.toLowerCase(Locale.ROOT);
        if(!(l.contains("maya")||l.contains("මායා"))) return;
        String q=l.replace("maya","").replace("මායා","").trim();

        if(isMemoryCommand(q)){
            handleMemory(q);
        }else if(q.contains("open ")||q.startsWith("open")||q.contains("launch ")||q.contains("start ")||q.contains("open app")||q.contains("ඇප් එක open")||q.contains("ඇප් එක අරින්න")){
            openApp(q);
        }else if(q.contains("call")||q.contains("කෝල්")){
            String target=q.replace("call","").replace("කෝල්","").trim();
            callContact(target);
        }else if((q.contains("do not disturb")||q.contains("dnd")||q.contains("disturb")) && (q.contains("on")||q.contains("දාන්න")||q.contains("enable"))){
            setDnd(true);
        }else if((q.contains("do not disturb")||q.contains("dnd")||q.contains("disturb")) && (q.contains("off")||q.contains("අයින්")||q.contains("disable"))){
            setDnd(false);
        }else if(q.contains("flash")||q.contains("torch")||q.contains("ටෝච්")){
            toggleFlash();
        }else if(q.contains("volume")||q.contains("ශබ්ද")||q.contains("sound")){
            volumeUp();
        }else if(q.contains("music")||q.contains("pause")||q.contains("play")||q.contains("සින්දු")){
            mediaKey(q.contains("pause")||q.contains("නවත්ත"));
        }else if(q.contains("notification")||q.contains("whatsapp")||q.contains("නොටිෆිකේෂන්")){
            readLatestNotification();
        }else if(q.contains("hello")||q.contains("hi")||q.contains("හෙලෝ")){
            speak(modeReply("හෙලෝ! මං Maya. කියන්න. 😄","හෙලෝ 😄 Maya online! කියන්නකෝ ✨","හෙලෝ! මං මෙතන. හෙමින් කියන්න. 💛"));
        }else if(q.contains("motivat")||q.contains("වැඩ")||q.contains("බැහැ")){
            speak(modeReply("හරි, පොඩි task එකක් දැන්ම පටන් ගමු. 🔥","හරි hero 😂 excuses වලට අද නිවාඩු! පටන් ගමු. 🔥","හරි, අමාරු නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛"));
        }else{
            askAI(s);
        }
    }

    private void askAI(String userText){
        String memoryText=memory==null?"":memory.all();
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        SharedPreferences d=getSharedPreferences("discipline",MODE_PRIVATE);
        String personality;
        if(p.getBoolean("mode_auto",false)){
            int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            personality=hour<12?"calm morning coach":hour>=21?"gentle night coach":"energetic discipline coach";
        }else if(p.getBoolean("mode_sweet",false)) personality="sweet/caring";
        else if(p.getBoolean("mode_cute",false)) personality="cute";
        else if(p.getBoolean("mode_funny",true)) personality="funny";
        else personality="normal";
        int completed=0;
        for(String k:d.getAll().keySet()) if(k.startsWith("done_") && d.getBoolean(k,false)) completed++;
        int today=0;
        String todayKey=new java.text.SimpleDateFormat("yyyyMMdd",java.util.Locale.US).format(new java.util.Date());
        for(int i=0;i<20;i++) if(d.getBoolean("task_"+i+"_"+todayKey,false)) today++;
        String context="Saved memory: "+memoryText+" | Discipline context: today has "+today+" completed tasks; "+completed+" completed days are recorded. Use this only when relevant.";
        MayaAI.ask(this,userText,context,personality,reply->handler.post(()->speak(reply)));
    }

    private boolean isMemoryCommand(String q){
        return q.contains("remember") || q.contains("mathaka") || q.contains("මතක") ||
               q.contains("memory") || q.contains("save this") || q.contains("save me") ||
               q.contains("forget") || q.contains("delete memory") || q.contains("clear memory");
    }

    private void handleMemory(String q){
        if(q.contains("what do you remember") || q.contains("what you remember") ||
           q.contains("මොනවා මතක") || q.contains("මතක තියෙන්නේ මොනවාද") || q.contains("memory list")){
            String all=memory.all();
            speak(all.isEmpty() ? "දැනට මගේ memory එක හිස්. 😄" : "මට මතක තියෙන්නේ මෙන්න:\n"+all);
            return;
        }
        if(q.contains("forget") || q.contains("delete memory") || q.contains("clear memory") ||
           q.contains("මතක අයින්") || q.contains("මතක මකන්න")){
            memory.clear();
            speak("හරි, මගේ saved memory එක clear කළා. 🧹");
            return;
        }

        String fact=q;
        String[] prefixes={
            "please remember","remember that","remember","save this","save me",
            "mathaka thiyaganna","mathaka thiyaganna meka","මතක තියාගන්න","මතක තියාගන්න මේක",
            "මතක තියාගන්න"
        };
        for(String prefix:prefixes){
            if(fact.startsWith(prefix)){
                fact=fact.substring(prefix.length()).trim();
                break;
            }
        }
        fact=fact.replaceFirst("^[,:;- ]+","");
        if(fact.isEmpty()){
            speak("මොකක්ද මතක තියාගන්න ඕනේ? 😄");
            return;
        }

        // Automatic memory only accepts clear, ordinary facts/preferences.
        // Avoid storing passwords, codes, payment details, or other sensitive secrets.
        String lower=fact.toLowerCase(Locale.ROOT);
        if(lower.contains("password")||lower.contains("passcode")||lower.contains("otp")||
           lower.contains("pin")||lower.contains("cvv")||lower.contains("credit card")||
           lower.contains("debit card")){
            speak("Passwords, PINs, OTPs වගේ sensitive details මං memory එකට save කරන්නේ නැහැ. 🔒");
            return;
        }

        memory.remember(fact);
        speak("හරි, ඒක මතක තියාගත්තා. 🧠✨");
    }

    private void openApp(String command){
        String q=command.toLowerCase(Locale.ROOT)
            .replace("open app","").replace("open","").replace("launch","").replace("start","")
            .replace("ඇප් එක open","").replace("ඇප් එක අරින්න","").trim();
        String pkg=null, name=q;
        if(q.contains("youtube")||q.contains("යූටියුබ්")){pkg="com.google.android.youtube";name="YouTube";}
        else if(q.contains("whatsapp")||q.contains("වට්ස්ඇප්")){pkg="com.whatsapp";name="WhatsApp";}
        else if(q.contains("chrome")||q.contains("ක්‍රෝම්")){pkg="com.android.chrome";name="Chrome";}
        else if(q.contains("instagram")||q.contains("ඉන්ස්ටග්‍රෑම්")){pkg="com.instagram.android";name="Instagram";}
        else if(q.contains("facebook")||q.contains("ෆේස්බුක්")){pkg="com.facebook.katana";name="Facebook";}
        else if(q.contains("tiktok")||q.contains("ටික්ටොක්")){pkg="com.zhiliaoapp.musically";name="TikTok";}
        else if(q.contains("spotify")||q.contains("ස්පොටිෆයි")){pkg="com.spotify.music";name="Spotify";}
        else if(q.contains("maps")||q.contains("map")||q.contains("මැප්")){pkg="com.google.android.apps.maps";name="Google Maps";}
        else if(q.contains("gmail")||q.contains("ජීමේල්")){pkg="com.google.android.gm";name="Gmail";}
        else if(q.contains("camera")||q.contains("කැමරා")){pkg="com.android.camera";name="Camera";}
        else if(q.contains("settings")||q.contains("සෙටින්")){pkg="com.android.settings";name="Settings";}
        if(pkg==null){
            speak("ඒ app එකේ නම මට හඳුනාගන්න බැරි වුණා. YouTube, WhatsApp, Chrome, Instagram, TikTok, Spotify, Maps වගේ app එකක් කියන්න.");
            return;
        }
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null){speak(name+" phone එකේ install කරලා නැහැ.");return;}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            speak(name+" open කළා. 📱");
        }catch(Exception e){speak(name+" open කරන්න බැරි වුණා.");}
    }

    private String modeReply(String normal,String funny,String sweet){
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        if(p.getBoolean("mode_sweet",false)) return sweet;
        if(p.getBoolean("mode_cute",false)) return funny;
        if(p.getBoolean("mode_funny",true)) return funny;
        return normal;
    }

    private void toggleFlash(){
        if(Build.VERSION.SDK_INT<23){speak("මේ phone එකේ flashlight control support නැහැ.");return;}
        try{
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);
            String id=cm.getCameraIdList()[0];
            SharedPreferences p=getSharedPreferences("maya_runtime",MODE_PRIVATE);
            boolean on=!p.getBoolean("flash",false);
            cm.setTorchMode(id,on);p.edit().putBoolean("flash",on).apply();
            speak(on?"Torch ON 🔦":"Torch OFF");
        }catch(Exception e){speak("Torch control කරන්න බැරි වුණා.");}
    }

    private void volumeUp(){
        try{
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
            speak("Volume ටිකක් වැඩි කළා. 🔊");
        }catch(Exception e){speak("Volume control කරන්න බැරි වුණා.");}
    }

    private void mediaKey(boolean pause){
        try{
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            long now=SystemClock.uptimeMillis();
            int key=pause?KeyEvent.KEYCODE_MEDIA_PAUSE:KeyEvent.KEYCODE_MEDIA_PLAY;
            am.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key));
            am.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,key));
            speak(pause?"Music pause කළා. ⏸️":"Music play කළා. ▶️");
        }catch(Exception e){speak("Music control කරන්න බැරි වුණා.");}
    }

    private void callContact(String name){
        if(name==null||name.trim().isEmpty()){speak("කාට call කරන්නද කියන්න.");return;}
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
            speak("Contacts permission එක app එකේ Settings වලින් allow කරන්න.");
            return;
        }
        Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+name.trim()+"%"},
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
        String number=null,display=null;
        if(c!=null){if(c.moveToFirst()){number=c.getString(0);display=c.getString(1);}c.close();}
        if(number==null){speak(name+" කියන contact එක හම්බවුනේ නැහැ.");return;}
        try{
            Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(display+" ගේ call screen එක open කළා.");
        }catch(Exception e){speak("Call screen එක open කරන්න බැරි වුණා.");}
    }

    private void setDnd(boolean on){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=23 && !nm.isNotificationPolicyAccessGranted()){
            speak("DND permission එක දෙන්න. Settings වල Maya DND access enable කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        if(Build.VERSION.SDK_INT>=23){
            nm.setInterruptionFilter(on?NotificationManager.INTERRUPTION_FILTER_NONE:NotificationManager.INTERRUPTION_FILTER_ALL);
            speak(on?"Do Not Disturb ON කළා. 🔕":"Do Not Disturb OFF කළා. 🔔");
        }
    }

    private void readLatestNotification(){
        String text=getSharedPreferences("maya_notifications",MODE_PRIVATE).getString("latest","");
        if(text.isEmpty()) speak("අලුත් notification එකක් මට read කරන්න ලැබිලා නැහැ.");
        else speak(text);
    }

    private void speak(String s){
        if(tts!=null&&ready) tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"maya_"+System.currentTimeMillis());
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel("maya_assistant","Maya Assistant",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Visible notification for Maya background microphone service");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
    @Override public void onDestroy(){stopping=true;if(handler!=null)handler.removeCallbacksAndMessages(null);if(recognizer!=null)recognizer.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}