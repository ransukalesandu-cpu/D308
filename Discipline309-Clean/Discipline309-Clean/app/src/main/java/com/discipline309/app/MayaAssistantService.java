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
import android.view.KeyCharacterMap;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.CalendarContract;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MayaAssistantService extends Service {
    // Wake-word architecture:
    // This service currently uses Android SpeechRecognizer for command capture.
    // A provider-independent WakeWordEngine hook lets us add Porcupine/openWakeWord
    // later without changing the command-routing code.
    private boolean wakeWordEnabled = true;
    private boolean wakeWordDetected = false;
    private boolean realWakeWordActive = false;
    private OpenWakeWordAdapter wakeWordAdapter;

    private static final int ID=3099;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ready=false, stopping=false;
    private Handler handler;
    private MayaMemory memory;
    private int speechErrorCount=0;
    private boolean listening=false;
    private boolean ttsSpeaking=false;\n    // Lightweight in-session context for short follow-up replies.\n    private String lastUserQuery="";\n    private String lastMayaReply="";

    @Override public void onCreate(){
        super.onCreate();
        if(!mayaAllowed()){ stopSelf(); return; }
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
        handler.postDelayed(this::startWakeWord,700);
        handler.postDelayed(this::scheduleProactiveCheckIn,2500);
    }

    private boolean mayaAllowed(){
        return !SupabaseAccountManager.loggedIn(this) || SupabaseAccountManager.can(this,"can_use_maya");
    }

    private static final long PROACTIVE_INTERVAL_MS=2L*60L*60L*1000L;
    private final Object PROACTIVE_TOKEN=new Object();

    private void scheduleProactiveCheckIn(){
        if(stopping || handler==null) return;
        handler.removeCallbacksAndMessages(PROACTIVE_TOKEN);
        handler.postDelayed(() -> {
            if(!stopping && mayaAllowed() && ready && getSharedPreferences("settings",MODE_PRIVATE).getBoolean("auto_speak",true)){
                if(!listening && !ttsSpeaking){
                    String suggestion=MayaPredictiveActions.nextSuggestion(this);
                    SharedPreferences p=getSharedPreferences("maya_proactive",MODE_PRIVATE);
                    long now=System.currentTimeMillis();
                    long last=p.getLong("last_spoken_at",0L);
                    String lastText=p.getString("last_text","");
                    if(!suggestion.equals(lastText) || now-last>=6L*60L*60L*1000L){
                        speak(suggestion);
                        p.edit().putLong("last_spoken_at",now).putString("last_text",suggestion).apply();
                    }
                }
            }
            scheduleProactiveCheckIn();
        }, PROACTIVE_INTERVAL_MS);
    }

    private void startWakeWord(){
        if(stopping || !wakeWordEnabled || !mayaAllowed()){ if(!mayaAllowed()) stopSelf(); return; }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            speak("Microphone permission එක allow කරන්න.");
            return;
        }
        try{
            wakeWordAdapter=new OpenWakeWordAdapter(
                this,
                () -> { handler.post(this::onMayaWakeWord); return kotlin.Unit.INSTANCE; },
                error -> { handler.post(() -> {
                    realWakeWordActive=false;
                    // Keep Maya usable if the bundled ONNX engine cannot initialize.
                    listen();
                }); return kotlin.Unit.INSTANCE; }
            );
            wakeWordAdapter.start();
        }catch(Exception e){
            realWakeWordActive=false;
            listen();
        }
    }

    private void onMayaWakeWord(){
        if(stopping || !mayaAllowed()){ stopSelf(); return; }
        wakeWordDetected=true;
        realWakeWordActive=true;
        if(wakeWordAdapter!=null) wakeWordAdapter.stop();
        speak(modeReply("ඔව්, කියන්න.","Yoo 😄 කියන්න, Maya online!","ඔව්, කියන්න. 💛"));
        handler.postDelayed(this::listen,350);
    }

    private void listen(){
        if(stopping || !ready || !mayaAllowed() || !SpeechRecognizer.isRecognitionAvailable(this)){ if(!mayaAllowed()) stopSelf(); return; }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){speak("Microphone permission එක allow කරන්න.");return;}
        if(listening) return;
        try{
        if(recognizer!=null) recognizer.destroy();
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        }catch(Exception e){
            listening=false;
            restart(1500);
            return;
        }
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){listening=true; speechErrorCount=0;}
            public void onBeginningOfSpeech(){}
            public void onRmsChanged(float r){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){listening=false;}
            public void onPartialResults(Bundle b){}
            public void onEvent(int t,Bundle b){}
            public void onError(int e){
                listening=false;
                speechErrorCount++;
                long delay=Math.min(5000,700L*(1L<<Math.min(3,speechErrorCount-1)));
                if(speechErrorCount>=4){
                    speechErrorCount=0;
                    speak("Voice listening එකට පොඩි issue එකක්. Maya ආයෙත් try කරනවා. 🎙️");
                    restart(2500);
                }else restart(delay);
            }
            public void onResults(Bundle b){
                listening=false; speechErrorCount=0;
                if(!mayaAllowed()){ stopSelf(); return; }
                ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String s=m==null||m.isEmpty()?"":m.get(0);
                handle(s);
                if(realWakeWordActive){
                    realWakeWordActive=false;
                    wakeWordDetected=false;
                    restart(900);
                }else{
                    restart(1300);
                }
            }
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"si-LK");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"si-LK");
        try{
            recognizer.startListening(i);
        }catch(Exception e){
            listening=false;
            restart(1500);
        }
    }
    private void restart(long d){
        if(handler!=null && mayaAllowed() && !stopping){
            handler.removeCallbacks(this::listen);
            handler.postDelayed(this::listen,d);
        }
    }

    private void handle(String raw){
        if(!mayaAllowed()){ stopSelf(); return; }
        String s=raw==null?"":raw.trim();
        String l=s.toLowerCase(Locale.ROOT);
        if(!realWakeWordActive && !(l.contains("maya")||l.contains("මායා"))) return;
        String q=l.replace("maya","").replace("මායා","").replace("මයා","").trim();\n        q=normalizeMixedCommand(q);\n        q=resolveSmartIntent(q);\n        if(q.isEmpty()) return;\n        String followUp=followUpContext(q);\n        if(followUp!=null){\n            askAI(followUp);\n            return;\n        }\n        lastUserQuery=q;

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
        }else if(q.contains("battery")||q.contains("බැටරි")||q.contains("charge")){
            batteryStatus();
        }else if(q.contains("wifi")||q.contains("wi-fi")||q.contains("වයිෆයි")){
            openSystemSettings(Settings.ACTION_WIFI_SETTINGS,"Wi-Fi settings");
        }else if(q.contains("bluetooth")||q.contains("බ්ලූටූත්")){
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS,"Bluetooth settings");
        }else if(q.contains("airplane")||q.contains("flight mode")||q.contains("airplane mode")){
            openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS,"Airplane mode settings");
        }else if(q.contains("location")||q.contains("gps")||q.contains("ලොකේෂන්")){
            openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS,"Location settings");
        }else if(q.contains("brightness")||q.contains("screen light")||q.contains("දීප්තිය")){
            setBrightness(q);
        }else if(q.contains("screen timeout")||q.contains("screen sleep")||q.contains("display timeout")){
            setScreenTimeout(q);
        }else if(q.contains("alarm")||q.contains("ඇලර්ම්")){
            openAlarm();
        }else if(q.contains("timer")||q.contains("ටයිමර්")){
            openTimer(q);
        }else if(q.contains("nfc")){
            openSystemSettings(Settings.ACTION_NFC_SETTINGS,"NFC settings");
        }else if(q.contains("data usage")||q.contains("mobile data")||q.contains("internet settings")){
            openSystemSettings(Settings.ACTION_DATA_USAGE_SETTINGS,"Data usage settings");
        }else if(q.contains("display settings")||q.contains("screen settings")){
            openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }else if(q.contains("app settings")||q.contains("application settings")){
            openSystemSettings(Settings.ACTION_APPLICATION_SETTINGS,"App settings");
        }else if(q.contains("notification settings")){
            openSystemSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS,"Notification settings");
        }else if(q.contains("calendar")||q.contains("schedule")||q.contains("කැලැන්ඩර්")||q.contains("event")){
            calendarEvent(q);
        }else if(q.contains("remind")||q.contains("reminder")||q.contains("මතක් කරන්න")){
            reminder(q);
        }else if(q.contains("device info")||q.contains("phone info")||q.contains("about phone")){
            deviceInfo();
        }else if(q.contains("plan")||q.contains("short plan")||q.contains("daily plan")||q.contains("මගේ plan")||q.contains("අද plan")||q.contains("අද වැඩ")){
            speak("📝 "+MayaOfflineNLP.answer(this,"plan"));
        }else if(q.contains("focus")||q.contains("focus goal")||q.contains("මගේ focus")){
            String live=MayaContextProvider.build(this);
            speak("🎯 "+extractContext(live,"focusGoal="));
        }else if(q.contains("mission")||q.contains("මගේ mission")||q.contains("mission එක")||q.contains("mission eka")||q.contains("ada mission")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"mission"));
        }else if(q.contains("xp")||q.contains("level")||q.contains("මගේ ලෙවල්")||q.contains("mage xp")||q.contains("xp kiyada")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"xp"));
        }else if(q.contains("streak")||q.contains("මගේ streak")||q.contains("streak eka")||q.contains("mage streak")){
            speak("ඔන්න 🔥 "+MayaContextProvider.quickStatus(this,"streak"));
        }else if(q.contains("day")||q.contains("දවස කීයද")||q.contains("කීවෙනි දවස")||q.contains("kaweni dawaseda")||q.contains("mage day")){
            speak("හරි 📅 "+MayaContextProvider.quickStatus(this,"day"));
        }else if(q.contains("progress")||q.contains("කොහොමද යන්නේ")||q.contains("mage progress")||q.contains("progress eka kohomada")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"progress"));
        }else if(q.contains("hello")||q.contains("hi")||q.contains("හෙලෝ")){
            speak(modeReply("හෙලෝ! මං Maya. කියන්න. 😄","හෙලෝ 😄 Maya online! කියන්නකෝ ✨","හෙලෝ! මං මෙතන. හෙමින් කියන්න. 💛"));
        }else if(q.contains("motivat")||q.contains("වැඩ")||q.contains("බැහැ")){
            speak(modeReply("හරි, පොඩි task එකක් දැන්ම පටන් ගමු. 🔥","හරි hero 😂 excuses වලට අද නිවාඩු! පටන් ගමු. 🔥","හරි, අමාරු නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛"));
        }else{
            askAI(s);
        }
    }

    private String extractContext(String s,String key){
        int i=s.indexOf(key); if(i<0)return "අද focus goal එකක් set කරලා නැහැ.";
        int j=s.indexOf(';',i); if(j<0)j=s.length();
        String v=s.substring(i+key.length(),j).trim();
        if(v.startsWith("\"")) v=v.substring(1);
        if(v.endsWith("\"")) v=v.substring(0,v.length()-1);
        return v.isEmpty()?"අද focus goal එකක් set කරලා නැහැ.":v;
    }

    private void askAI(String userText){
        String predictive = MayaPredictiveActions.nextSuggestion(this);
        if (userText == null || userText.trim().isEmpty()) { speak(predictive); return; }
        String memoryText=memory==null?"":memory.all();
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        String personality;
        if(p.getBoolean("mode_auto",false)){
            int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            personality=hour<12?"calm morning coach":hour>=21?"gentle night coach":"energetic discipline coach";
        }else if(p.getBoolean("mode_sweet",false)) personality="sweet/caring";
        else if(p.getBoolean("mode_cute",false)) personality="cute";
        else if(p.getBoolean("mode_funny",true)) personality="funny";
        else personality="normal";

        String live=MayaContextProvider.build(this);
        String context="Public creator profile: Maya was created by Lesandu Ransuka, born September 19, 2008. He studies A/L Science with Mathematics. His sister is Sethuli Senanga; his mother is Gayani Fernando; his father is Hemal Asiri. This is public profile information and may be shared when asked. Do not reveal private memory or private conversations. | Saved memory: "+memoryText+" | "+live+
                " Current app state is authoritative for discipline data. Use it naturally and don't invent values.";
        MayaAI.ask(this,userText,context,personality,reply->handler.post(()->{\n            lastMayaReply=reply==null?"":reply;\n            speak(reply);\n        })));
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
            KeyEvent down=new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,0);
            KeyEvent up=new KeyEvent(now,now,KeyEvent.ACTION_UP,key,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,0);
            am.dispatchMediaKeyEvent(down);
            am.dispatchMediaKeyEvent(up);
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

    private void openSystemSettings(String action,String label){
        try{
            Intent i=new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(label+" open කළා. ⚙️");
        }catch(Exception e){speak(label+" open කරන්න බැරි වුණා.");}
    }

    private void batteryStatus(){
        try{
            BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);
            int level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            IntentFilter f=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent b=registerReceiver(null,f);
            boolean charging=false;
            if(b!=null){int status=b.getIntExtra(BatteryManager.EXTRA_STATUS,-1); charging=status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL;}
            speak("Battery එක "+level+"%. "+(charging?"දැනට charge වෙනවා. 🔋":"දැනට charge වෙන්නේ නැහැ. 🔋"));
        }catch(Exception e){speak("Battery status එක ගන්න බැරි වුණා.");}
    }

    private void setBrightness(String q){
        if(!Settings.System.canWrite(this)){
            speak("Screen brightness control කරන්න WRITE SETTINGS permission එක allow කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\\\d{1,3})").matcher(q);
            if(m.find()){
                int pct=Math.max(1,Math.min(100,Integer.parseInt(m.group(1))));
                int value=Math.round(255f*pct/100f);
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
                speak("Brightness "+pct+"% කළා. ☀️");
            }else openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }catch(Exception e){speak("Brightness change කරන්න බැරි වුණා.");}
    }

    private void setScreenTimeout(String q){
        if(!Settings.System.canWrite(this)){
            speak("Screen timeout change කරන්න WRITE SETTINGS permission එක allow කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\\\d+)").matcher(q);
            if(m.find()){
                int minutes=Math.max(1,Math.min(60,Integer.parseInt(m.group(1))));
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,minutes*60*1000);
                speak("Screen timeout "+minutes+" minutes කළා. 💤");
            }else openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }catch(Exception e){speak("Screen timeout change කරන්න බැරි වුණා.");}
    }

    private void openAlarm(){
        try{
            Intent i=new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Alarm screen එක open කළා. ⏰");
        }catch(Exception e){speak("Alarm app එක open කරන්න බැරි වුණා.");}
    }

    private void openTimer(String q){
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\\\d+)").matcher(q);
            Intent i=new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
            if(m.find()) i.putExtra(android.provider.AlarmClock.EXTRA_LENGTH,Math.max(1,Math.min(86400,Integer.parseInt(m.group(1))*60)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(m.find()?"Timer screen එක open කළා. ⏱️":"Timer screen එක open කළා. ⏱️");
        }catch(Exception e){speak("Timer app එක open කරන්න බැරි වුණා.");}
    }

    private void calendarEvent(String q){
        try{
            Intent i=new Intent(Intent.ACTION_INSERT);
            i.setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE, extractEventTitle(q));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Calendar event එක add කරන්න screen එක open කළා. 📅");
        }catch(Exception e){speak("Calendar එක open කරන්න බැරි වුණා.");}
    }

    private String extractEventTitle(String q){
        String s=q.replace("add calendar event","").replace("calendar event","")
            .replace("add event","").replace("schedule","").replace("event","").trim();
        return s.isEmpty()?"Maya event":s;
    }

    private void reminder(String q){
        try{
            String text=q.replace("remind me","").replace("set reminder","")
                .replace("reminder","").replace("මතක් කරන්න","").trim();
            Intent i=new Intent(Intent.ACTION_INSERT);
            i.setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE,text.isEmpty()?"Maya reminder":text);
            i.putExtra(CalendarContract.Events.DESCRIPTION,"Created by Maya");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Reminder එක save කරන්න calendar screen එක open කළා. 🔔");
        }catch(Exception e){speak("Reminder එක create කරන්න බැරි වුණා.");}
    }

    private void deviceInfo(){
        String model=Build.MANUFACTURER+" "+Build.MODEL;
        speak("Phone එක "+model+". Android "+Build.VERSION.RELEASE+". API "+Build.VERSION.SDK_INT+".");
    }

    private void speak(String s){
        if(s==null||s.trim().isEmpty()||tts==null||!ready)return;
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        if(!p.getBoolean("auto_speak",true))return;
        // Prevent Maya from hearing her own response through SpeechRecognizer.
        if(recognizer!=null&&listening){
            try{recognizer.cancel();}catch(Exception ignored){}
            listening=false;
        }
        float rate=.65f+(p.getInt("speech_speed",50)/100f)*.85f;
        tts.setSpeechRate(rate);
        ttsSpeaking=true;
        String id="maya_"+System.currentTimeMillis();
        if(Build.VERSION.SDK_INT>=15){
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
                @Override public void onStart(String utteranceId){ttsSpeaking=true;}
                @Override public void onDone(String utteranceId){ttsSpeaking=false;}
                @Override public void onError(String utteranceId){ttsSpeaking=false;}
            });
        }
        tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,id);
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel("maya_assistant","Maya Assistant",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Visible notification for Maya background microphone service");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!mayaAllowed()){stopSelf();return START_NOT_STICKY;}
        return START_STICKY;
    }
    @Override public void onTaskRemoved(Intent rootIntent){
        // Keep Maya alive when the app task is dismissed. Android may still stop the
        // process for battery/memory reasons, so START_STICKY remains the main recovery path.
        if(mayaAllowed() && !stopping){
            try{
                Intent restart=new Intent(this,MayaAssistantService.class);
                if(Build.VERSION.SDK_INT>=26) startForegroundService(restart);
                else startService(restart);
            }catch(Exception ignored){}
        }
        super.onTaskRemoved(rootIntent);
    }
    @Override public void onDestroy(){
        stopping=true;
        if(handler!=null)handler.removeCallbacksAndMessages(null);
        if(recognizer!=null)recognizer.destroy();
        if(wakeWordAdapter!=null)wakeWordAdapter.stop();
        if(tts!=null){tts.stop();tts.shutdown();}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}