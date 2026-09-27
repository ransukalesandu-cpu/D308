package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs; private TextToSpeech tts;
    private static final int BG=0xFF0B0E14,SURFACE=0xFF191D27,TEXT=0xFFF7F8FC,MUTED=0xFFAAB2C3,ACCENT=0xFF63E6BE;
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(6),dp(4),dp(6));return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(12),dp(16),dp(12));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(SURFACE);g.setCornerRadius(dp(18));l.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);prefs=getSharedPreferences("settings",MODE_PRIVATE);buildUi();}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackgroundColor(BG);
        TextView t=label("⚙  Settings",27,TEXT);t.setTypeface(null,1);root.addView(t);root.addView(label("Personalize your discipline experience.",13,MUTED));
        LinearLayout voice=card();voice.addView(label("AI VOICE",11,MUTED));
        Switch speak=new Switch(this);speak.setText("Auto speak AI responses");speak.setTextColor(TEXT);speak.setTextSize(15);speak.setChecked(prefs.getBoolean("auto_speak",true));speak.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("auto_speak",c).apply());voice.addView(speak);
        voice.addView(label("Speech speed",14,TEXT));SeekBar speed=new SeekBar(this);speed.setMax(100);speed.setProgress(prefs.getInt("speech_speed",50));voice.addView(speed);TextView speedText=label("Normal",12,MUTED);voice.addView(speedText);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){String s=p<30?"Slow":p>70?"Fast":"Normal";speedText.setText("Speech speed: "+s);if(u)prefs.edit().putInt("speech_speed",p).apply();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        Button test=new Button(this);test.setText("🔊  Test AI voice");test.setAllCaps(false);test.setOnClickListener(v->speak("Your Discipline assistant is ready."));voice.addView(test);root.addView(voice);
        LinearLayout app=card();app.addView(label("APP",11,MUTED));Switch notifications=new Switch(this);notifications.setText("Notifications");notifications.setTextColor(TEXT);notifications.setTextSize(15);notifications.setChecked(prefs.getBoolean("notifications",true));notifications.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("notifications",c).apply());app.addView(notifications);
        Button reset=new Button(this);reset.setText("↻  Reset progress");reset.setAllCaps(false);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Reset progress?").setMessage("This will remove saved discipline progress and alarms.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{getSharedPreferences("discipline",MODE_PRIVATE).edit().clear().apply();Toast.makeText(this,"Progress reset",Toast.LENGTH_SHORT).show();} ).show());app.addView(reset);root.addView(app);
        LinearLayout about=card();about.addView(label("ABOUT 309",11,MUTED));about.addView(label("309 Day Discipline",19,TEXT));about.addView(label("Build discipline. One day at a time.\nVersion 1.1 • Offline-first",13,MUTED));root.addView(about);
        scroll.addView(root);setContentView(scroll);
    }
    private void speak(String s){if(tts==null)tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS)speakNow(s);});else speakNow(s);}
    private void speakNow(String s){float rate=.65f+(prefs.getInt("speech_speed",50)/100f)*.85f;tts.setLanguage(Locale.US);tts.setSpeechRate(rate);tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"discipline_settings");}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
