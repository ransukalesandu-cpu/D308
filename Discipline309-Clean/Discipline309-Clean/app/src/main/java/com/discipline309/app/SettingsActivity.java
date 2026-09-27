package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.text.InputType;
import android.widget.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs; private TextToSpeech tts; private ToneGenerator tone;
    private void clickSound(){try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,70);tone.startTone(ToneGenerator.TONE_PROP_ACK,90);}catch(Exception ignored){}}
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
        Button test=new Button(this);test.setText("🔊  Test AI voice");test.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP)clickSound();return false;});test.setAllCaps(false);test.setOnClickListener(v->speak("Your Discipline assistant is ready."));voice.addView(test);root.addView(voice);
        LinearLayout ai=card();ai.addView(label("🧠 MAYA REAL AI BRAIN",11,MUTED));
        ai.addView(label("Connect Maya to an AI model for natural conversations. The key is stored only on this phone.",12,MUTED));
        EditText key=new EditText(this);key.setHint("AI API key");key.setText(getSharedPreferences("maya_ai",MODE_PRIVATE).getString("api_key",""));key.setSingleLine(true);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);ai.addView(key);
        EditText model=new EditText(this);model.setHint("Model (default: gpt-5-mini)");model.setText(getSharedPreferences("maya_ai",MODE_PRIVATE).getString("model","gpt-5-mini"));model.setSingleLine(true);ai.addView(model);
        Button saveAi=new Button(this);saveAi.setText("🧠  Save AI brain settings");saveAi.setAllCaps(false);saveAi.setOnClickListener(v->{getSharedPreferences("maya_ai",MODE_PRIVATE).edit().putString("api_key",key.getText().toString().trim()).putString("model",model.getText().toString().trim().isEmpty()?"gpt-5-mini":model.getText().toString().trim()).apply();Toast.makeText(this,"Maya AI brain settings saved 🧠",Toast.LENGTH_SHORT).show();});ai.addView(saveAi);
        ai.addView(label("Default endpoint: OpenAI-compatible /v1/chat/completions. Never paste your API key into GitHub or share it with anyone.",11,MUTED));
        root.addView(ai);

        LinearLayout customize=card();customize.addView(label("🎨 CUSTOMIZE EXPERIENCE",11,MUTED));
        customize.addView(label("Choose one of 3 complete UI styles. The app restarts its screen when you return.",12,MUTED));
        RadioGroup themes=new RadioGroup(this); themes.setOrientation(RadioGroup.VERTICAL);
        String[] themeNames={"🌙 Midnight — clean dark","⚡ Neon — cyber energy","☁ Soft — calm pastel"};
        String[] themeKeys={"midnight","neon","soft"}; String current=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");
        for(int i=0;i<3;i++){RadioButton rb=new RadioButton(this);rb.setText(themeNames[i]);rb.setTextColor(TEXT);rb.setTextSize(15);rb.setTag(themeKeys[i]);rb.setChecked(current.equals(themeKeys[i]));themes.addView(rb);}
        themes.setOnCheckedChangeListener((g,id)->{RadioButton rb=g.findViewById(id);if(rb!=null){getSharedPreferences("ui_settings",MODE_PRIVATE).edit().putString("theme",String.valueOf(rb.getTag())).apply();Toast.makeText(this,"UI saved — reopen the app to apply ✨",Toast.LENGTH_SHORT).show();}});
        customize.addView(themes);
        root.addView(customize);

        LinearLayout modes=card();modes.addView(label("🤖 MAYA PERSONALITY MODES",11,MUTED));
        modes.addView(label("Turn styles on/off independently. Maya stays respectful and supportive.",12,MUTED));
        Switch funny=new Switch(this);funny.setText("😂 Funny mode");funny.setTextColor(TEXT);funny.setTextSize(15);funny.setChecked(prefs.getBoolean("mode_funny",true));funny.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("mode_funny",on).apply());modes.addView(funny);
        Switch cute=new Switch(this);cute.setText("🌸 Cute mode");cute.setTextColor(TEXT);cute.setTextSize(15);cute.setChecked(prefs.getBoolean("mode_cute",false));cute.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("mode_cute",on).apply());modes.addView(cute);
        Switch sweet=new Switch(this);sweet.setText("💛 Sweet / caring mode");sweet.setTextColor(TEXT);sweet.setTextSize(15);sweet.setChecked(prefs.getBoolean("mode_sweet",false));sweet.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("mode_sweet",on).apply());modes.addView(sweet);
        Switch auto=new Switch(this);auto.setText("🧠 Auto mood");auto.setTextColor(TEXT);auto.setTextSize(15);auto.setChecked(prefs.getBoolean("mode_auto",true));auto.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("mode_auto",on).apply());modes.addView(auto);
        root.addView(modes);

        LinearLayout coach=card();coach.addView(label("BACKGROUND COACH",11,MUTED));Switch bgCoach=new Switch(this);bgCoach.setText("Funny Sinhala motivation in background");bgCoach.setTextColor(TEXT);bgCoach.setTextSize(15);bgCoach.setChecked(getSharedPreferences("discipline",MODE_PRIVATE).getBoolean("coach_enabled",false));bgCoach.setOnCheckedChangeListener((v,on)->{getSharedPreferences("discipline",MODE_PRIVATE).edit().putBoolean("coach_enabled",on).apply();Intent i=new Intent(this,MotivationService.class);if(on){if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Toast.makeText(this,"Background coach ON 🔥",Toast.LENGTH_SHORT).show();}else{stopService(i);Toast.makeText(this,"Background coach OFF",Toast.LENGTH_SHORT).show();}});coach.addView(bgCoach);coach.addView(label("Uses your phone's installed Sinhala TTS voice. Android shows a persistent notification while active.",12,MUTED));root.addView(coach);

        LinearLayout control=card();control.addView(label("📱 MAYA PHONE CONTROLS",11,MUTED));
        control.addView(label("Voice commands for simple phone actions.",12,MUTED));
        Button battery=new Button(this);battery.setText("🔋  Battery / background settings");battery.setAllCaps(false);battery.setOnClickListener(v->{try{Intent i=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});control.addView(battery);
        Button voiceHelp=new Button(this);voiceHelp.setText("🎙️  Maya command guide");voiceHelp.setAllCaps(false);voiceHelp.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Maya commands").setMessage("Say “Maya” first.\n\n• Maya torch on/off\n• Maya volume up\n• Maya play / pause music\n• Maya call [contact]\n• Maya notifications\n• Maya DND on/off\n• Maya motivate me").setPositiveButton("OK",null).show());control.addView(voiceHelp);
        root.addView(control);

        LinearLayout maya=card();maya.addView(label("MAYA BACKGROUND ASSISTANT",11,MUTED));
        Switch bgMaya=new Switch(this);bgMaya.setText("Keep Maya available in background");bgMaya.setTextColor(TEXT);bgMaya.setTextSize(15);
        bgMaya.setChecked(getSharedPreferences("maya_settings",MODE_PRIVATE).getBoolean("enabled",false));
        bgMaya.setOnCheckedChangeListener((v,on)->{
            getSharedPreferences("maya_settings",MODE_PRIVATE).edit().putBoolean("enabled",on).apply();
            if(on){
                if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},3101);
                    bgMaya.setChecked(false); return;
                }
                if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},3102);
                Intent i=new Intent(this,MayaAssistantService.class);
                if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
                Toast.makeText(this,"Maya background assistant ON 🎙️",Toast.LENGTH_SHORT).show();
            }else{
                stopService(new Intent(this,MayaAssistantService.class));
                Toast.makeText(this,"Maya background assistant OFF",Toast.LENGTH_SHORT).show();
            }
        });
        maya.addView(bgMaya);
        maya.addView(label("Maya uses a visible Android foreground notification while listening. Voice recognition may use mobile data depending on the phone's speech engine.",12,MUTED));
        Button notifyAccess=new Button(this);notifyAccess.setText("🔔  Allow WhatsApp notification access");notifyAccess.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP)clickSound();return false;});notifyAccess.setAllCaps(false);
        notifyAccess.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});
        maya.addView(notifyAccess);
        Button dndAccess=new Button(this);dndAccess.setText("🔕  Allow DND control");dndAccess.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP)clickSound();return false;});dndAccess.setAllCaps(false);
        dndAccess.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));}catch(Exception ignored){}});
        maya.addView(dndAccess);
        root.addView(maya);
        LinearLayout mem=card();mem.addView(label("🧠 MAYA MEMORY",11,MUTED));
        mem.addView(label("Saved memory stays on this phone and can be cleared anytime.",12,MUTED));
        Button viewMem=new Button(this);viewMem.setText("👀  View / delete saved memory");viewMem.setAllCaps(false);viewMem.setOnClickListener(v->showMemoryManager());mem.addView(viewMem);
        Button clearMem=new Button(this);clearMem.setText("🗑  Clear all Maya memory");clearMem.setAllCaps(false);clearMem.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear Maya memory?").setMessage("This removes all saved ordinary facts and preferences.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{new MayaMemory(this).clear();Toast.makeText(this,"Maya memory cleared.",Toast.LENGTH_SHORT).show();} ).show());mem.addView(clearMem);
        root.addView(mem);

        LinearLayout app=card();app.addView(label("APP",11,MUTED));Switch notifications=new Switch(this);notifications.setText("Notifications");notifications.setTextColor(TEXT);notifications.setTextSize(15);notifications.setChecked(prefs.getBoolean("notifications",true));notifications.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("notifications",c).apply());app.addView(notifications);
        Button reset=new Button(this);reset.setText("↻  Reset progress");reset.setAllCaps(false);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Reset progress?").setMessage("This will remove saved discipline progress and alarms.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{getSharedPreferences("discipline",MODE_PRIVATE).edit().clear().apply();Toast.makeText(this,"Progress reset",Toast.LENGTH_SHORT).show();} ).show());app.addView(reset);root.addView(app);
        LinearLayout about=card();about.addView(label("ABOUT 309",11,MUTED));about.addView(label("309 Day Discipline",19,TEXT));about.addView(label("Build discipline. One day at a time.\nVersion 2.0 • Discipline + Maya",13,MUTED));root.addView(about);
        scroll.addView(root);setContentView(scroll);
    }
    private void showMemoryManager(){MayaMemory m=new MayaMemory(this);String all=m.all();if(all.isEmpty()){new AlertDialog.Builder(this).setTitle("Maya memory").setMessage("No saved memory yet.").setPositiveButton("OK",null).show();return;}String[] items=all.split(String.valueOf((char)10));new AlertDialog.Builder(this).setTitle("Maya memory — tap one to delete").setItems(items,(d,which)->new AlertDialog.Builder(this).setTitle("Delete this memory?").setMessage(items[which]).setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{m.remove(which);showMemoryManager();}).show()).setNegativeButton("Close",null).show();}

    private void speak(String s){if(tts==null)tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS)speakNow(s);});else speakNow(s);}
    private void speakNow(String s){float rate=.65f+(prefs.getInt("speech_speed",50)/100f)*.85f;tts.setLanguage(Locale.US);tts.setSpeechRate(rate);tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"discipline_settings");}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}if(tone!=null)tone.release();super.onDestroy();}
}
