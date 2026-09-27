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

    @Override public void onCreate(){
        super.onCreate();
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

        if(q.contains("call")||q.contains("කෝල්")){
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
        }else if(q.contains("notification")||q.contains("whatsapp")||q.contains("නොටිෆිකේෂන්")||q.contains("whatsapp")){
            readLatestNotification();
        }else if(q.contains("hello")||q.contains("hi")||q.contains("හෙලෝ")){
            speak(modeReply("හෙලෝ! මං Maya. කියන්න. 😄","හෙලෝ 😄 Maya online! කියන්නකෝ ✨","හෙලෝ! මං මෙතන. හෙමින් කියන්න. 💛"));
        }else if(q.contains("motivat")||q.contains("වැඩ")||q.contains("බැහැ")){
            speak(modeReply("හරි, පොඩි task එකක් දැන්ම පටන් ගමු. 🔥","හරි hero 😂 excuses වලට අද නිවාඩු! පටන් ගමු. 🔥","හරි, අමාරු නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛"));
        }else{
            speak("හරි 😄 මට call screen, DND, notifications, torch, volume, music වගේ phone actions කරන්න කියන්න.");
        }
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