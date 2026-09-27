package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MotivationService extends Service {
    private static final int ID=3091;
    private TextToSpeech tts;
    private Handler handler;
    private final Random random=new Random();
    private boolean ready=false;
    private final String[] lines={
        "හරි හරි, දැන්ම එක task එකක් පටන් ගමු. ඔයාට මේක පුළුවන්. 🔥",
        "අනේ මගේ hero, phone එක දිහා බලාගෙන ඉන්න එක නවත්තලා වැඩේ පටන් ගමුකෝ. 😂",
        "ඔයාට motivation නැද්ද? හරි, මං motivation එක අරගෙන ආවා. දැන් වැඩේ කරමු. 😂🔥",
        "අද දවසත් waste කරන්නද හදන්නේ? එහෙම බැහැ හොඳේ. යමු යමු! 😭😂",
        "එක task එකක් විතරයි. ඒක complete කරලා පස්සේ මට thanks කියන්න. 😂",
        "Oiii! තාමත් start කරලා නැද්ද? මං බලන් ඉන්නවා හොඳේ. 😂🔥",
        "Good job! පොඩි progress එකක් වුණත් progress තමයි. දැන් ඊළඟ එක කරමු. ❤️",
        "අද streak එක කඩන්න එපා. ඔයා මෙච්චර දුර ආවේ නිකම් නෙමෙයි. 🔥",
        "හරි champion, excuses ටික පස්සේ. දැන් විනාඩි පහක් focus කරමු. 💪",
        "ඔයාට බැහැ කියලා හිතෙනවද? එහෙනම් පොඩි step එකක් කරලා මට prove කරන්න. 😂🔥"
    };

    @Override public void onCreate(){
        super.onCreate();
        createChannel();
        Notification n=new Notification.Builder(this,"motivation")
            .setContentTitle("309 Day Discipline")
            .setContentText("Your funny motivation coach is active 🔥")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT))
            .build();
        startForeground(ID,n);
        tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("si","LK"));ready=true;scheduleNext(5000);}});
    }

    private void scheduleNext(long delay){
        if(handler==null)handler=new Handler(Looper.getMainLooper());
        handler.postDelayed(()->{if(ready){speak(lines[random.nextInt(lines.length)]);}scheduleNext(30*60*1000L+random.nextInt(15*60*1000));},delay);
    }
    private void speak(String s){
        if(tts==null)return;
        tts.setSpeechRate(.95f);
        tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"motivation_"+System.currentTimeMillis());
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel("motivation","Motivation Coach",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Background voice motivation");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
    @Override public void onDestroy(){
        if(handler!=null)handler.removeCallbacksAndMessages(null);
        if(tts!=null){tts.stop();tts.shutdown();}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}