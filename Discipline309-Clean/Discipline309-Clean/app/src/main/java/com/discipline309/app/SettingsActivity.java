package com.discipline309.app;

import android.os.Build;
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
import java.util.Calendar;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs; private TextToSpeech tts; private ToneGenerator tone;
    private void clickSound(){try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,70);tone.startTone(ToneGenerator.TONE_PROP_ACK,90);}catch(Exception ignored){}}
    private int BG=0xFF061126,SURFACE=0xFF0B1B3A,TEXT=0xFFF5F8FF,MUTED=0xFF9CB2D9,ACCENT=0xFF2F7BFF;
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private Button buttonStyle(Button b){b.setTextColor(TEXT);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(48));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(0xFF102957);g.setCornerRadius(dp(16));g.setStroke(dp(1),0xFF173D78);b.setBackground(g);return b;}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(6),dp(4),dp(6));return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(12),dp(16),dp(12));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(SURFACE);g.setCornerRadius(dp(18));g.setStroke(dp(1),0xFF173D78);l.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);try{if(SupabaseAccountManager.loggedIn(this)&&!SupabaseAccountManager.can(this,"can_access_settings")){new AlertDialog.Builder(this).setTitle("Settings restricted").setMessage("Your Primary account has disabled Settings access for this Sub account.").setPositiveButton("OK",(d,w)->finish()).show();return;}prefs=getSharedPreferences("settings",MODE_PRIVATE);String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");BG=0xFF061126;SURFACE=0xFF0B1B3A;TEXT=0xFFF5F8FF;MUTED=0xFF9CB2D9;ACCENT=0xFF2F7BFF;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUi();}catch(Exception e){Toast.makeText(this,"Settings could not open safely.",Toast.LENGTH_SHORT).show();finish();}}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));
        root.setBackgroundColor(BG);
        TextView t=label("⚙  Settings",27,TEXT);t.setTypeface(null,1);root.addView(t);
        root.addView(label("Choose a category to edit your settings.",13,MUTED));

        addCategory(root,"🤖  AI ASSISTANT","Maya voice, AI brain, web search and memory.",v->showAiSettings());
        addCategory(root,"🎭  MODES","Maya personality and background motivation modes.",v->showModeSettings());
        addCategory(root,"🎨  DISPLAY","Theme and notification preferences.",v->showDisplaySettings());
        addCategory(root,"🔐  SECURITY & PRIVACY","Permissions and Maya privacy controls.",v->showSecuritySettings());
        addCategory(root,"📱  PHONE & BACKGROUND","Background assistant, phone controls and battery settings.",v->showPhoneSettings());
        addCategory(root,"👥  ACCOUNTS & DATA","Primary/Sub accounts and progress controls.",v->showAccountSettings());
        addCategory(root,"ℹ️  ABOUT 309","App information and version.",v->showAboutSettings());

        scroll.addView(root);setContentView(scroll);
    }

    private void addCategory(LinearLayout root,String titleText,String desc,View.OnClickListener action){
        LinearLayout box=card();
        box.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView title=label(titleText,17,TEXT);title.setTypeface(null,1);box.addView(title);
        box.addView(label(desc,12,MUTED));
        Button open=new Button(this);open=buttonStyle(open);open.setText("Open  ›");open.setGravity(Gravity.CENTER);open.setOnClickListener(action);box.addView(open);
        root.addView(box);
    }

    private LinearLayout categoryLayout(String titleText,String desc){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(28));root.setBackgroundColor(BG);
        TextView t=label(titleText,25,TEXT);t.setTypeface(null,1);root.addView(t);root.addView(label(desc,13,MUTED));return root;
    }
    private ScrollView categoryScroll(LinearLayout root){ScrollView s=new ScrollView(this);s.addView(root);return s;}
    private void showCategory(String titleText,String desc,LinearLayout body){
        LinearLayout wrapper=categoryLayout(titleText,desc);
        Button back=buttonStyle(new Button(this));back.setText("←  Back to Settings");back.setOnClickListener(v->buildUi());wrapper.addView(back);
        wrapper.addView(body);
        setContentView(categoryScroll(wrapper));
    }

    private void showAiSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout voice=card();voice.addView(label("🎙️ AI VOICE",11,MUTED));
        Switch speak=new Switch(this);speak.setText("Auto speak AI responses");speak.setTextColor(TEXT);speak.setTextSize(15);speak.setChecked(prefs.getBoolean("auto_speak",true));speak.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("auto_speak",c).apply());voice.addView(speak);
        voice.addView(label("Speech speed",14,TEXT));SeekBar speed=new SeekBar(this);speed.setMax(100);speed.setProgress(prefs.getInt("speech_speed",50));voice.addView(speed);TextView speedText=label("Normal",12,MUTED);voice.addView(speedText);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){String s=p<30?"Slow":p>70?"Fast":"Normal";speedText.setText("Speech speed: "+s);if(u)prefs.edit().putInt("speech_speed",p).apply();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        Button test=buttonStyle(new Button(this));test.setText("🔊  Test AI voice");test.setOnClickListener(v->speak("Your Discipline assistant is ready."));voice.addView(test);body.addView(voice);

        LinearLayout ai=card();ai.addView(label("🧠 MAYA AI BRAIN",11,MUTED));
        ai.addView(label("Maya AI is connected securely through the Supabase backend. No OpenAI API key is stored in this app.",12,MUTED));
        ai.addView(label("Model: gpt-5-mini",13,TEXT));
        body.addView(ai);

        LinearLayout web=card();web.addView(label("🌐 WEB SEARCH",11,MUTED));
        web.addView(label("Current web searches are handled securely by the Supabase backend.",12,MUTED));
        body.addView(web);

        LinearLayout mem=card();mem.addView(label("🧠 MAYA MEMORY",11,MUTED));mem.addView(label("Saved memory stays on this phone.",12,MUTED));
        Button view=buttonStyle(new Button(this));view.setText("👀  View / delete saved memory");view.setOnClickListener(v->showMemoryManager());mem.addView(view);
        Button clear=buttonStyle(new Button(this));clear.setText("🗑  Clear all Maya memory");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear Maya memory?").setMessage("This removes all saved ordinary facts and preferences.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{try{new MayaMemory(this).clear();Toast.makeText(this,"Maya memory cleared.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Could not clear memory.",Toast.LENGTH_SHORT).show();}}).show());mem.addView(clear);body.addView(mem);

                showCategory("🤖  AI ASSISTANT","Maya AI, voice, web search and memory.",body);
    }

    private void showModeSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout modes=card();
        modes.addView(label("🎭 MAYA MODE",11,MUTED));
        modes.addView(label("Maya is focused by default on discipline + fitness training. Select one mode only.",12,MUTED));

        final String[] modeKeys={"romance","caring","angry","motivative","auto"};
        final String[] modeLabels={"💗 Romance","💛 Caring","😤 Angry / Tough","🔥 Motivative","🤖 Auto"};
        RadioGroup group=new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        String current=prefs.getString("maya_mode","motivative");
        int checkedId=-1;
        for(int i=0;i<modeKeys.length;i++){
            RadioButton rb=new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(modeLabels[i]);
            rb.setTextColor(TEXT);rb.setTextSize(15);
            rb.setPadding(0,dp(5),0,dp(5));
            rb.setTag(modeKeys[i]);
            rb.setChecked(modeKeys[i].equals(current));
            if(rb.isChecked()) checkedId=rb.getId();
            group.addView(rb);
        }
        if(checkedId!=-1) group.check(checkedId);
        group.setOnCheckedChangeListener((g,id)->{
            View selected=g.findViewById(id);
            if(selected!=null && selected.getTag()!=null){
                prefs.edit().putString("maya_mode",String.valueOf(selected.getTag())).apply();
            }
        });
        modes.addView(group);
        modes.addView(label("Auto chooses the tone from the situation while keeping discipline and fitness as Maya's main target.",12,MUTED));
        body.addView(modes);

        LinearLayout coach=card();
        coach.addView(label("🔥 BACKGROUND COACH",11,MUTED));
        Switch bg=new Switch(this);
        bg.setText("Background discipline + fitness motivation");
        bg.setTextColor(TEXT);bg.setTextSize(15);
        bg.setChecked(getSharedPreferences("discipline",MODE_PRIVATE).getBoolean("coach_enabled",false));
        bg.setOnCheckedChangeListener((v,on)->{
            getSharedPreferences("discipline",MODE_PRIVATE).edit().putBoolean("coach_enabled",on).apply();
            Intent i=new Intent(this,MotivationService.class);
            if(on){
                try{
                    if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
                    Toast.makeText(this,"Background coach ON 🔥",Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    getSharedPreferences("discipline",MODE_PRIVATE).edit().putBoolean("coach_enabled",false).apply();
                    v.setChecked(false);
                }
            }else{stopService(i);Toast.makeText(this,"Background coach OFF",Toast.LENGTH_SHORT).show();}
        });
        coach.addView(bg);body.addView(coach);
        showCategory("🎭  MODES","Maya personality and discipline/fitness training modes.",body);
    }

    private void showDisplaySettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout theme=card();theme.addView(label("🎨 THEME",11,MUTED));theme.addView(label("Choose the app appearance.",12,MUTED));
        RadioButton rb=new RadioButton(this);rb.setText("🌌 Deep Navy — 309 Day");rb.setTextColor(TEXT);rb.setTextSize(15);rb.setChecked(true);theme.addView(rb);body.addView(theme);
        LinearLayout language=card();
        language.addView(label("🌐 APP LANGUAGE",11,MUTED));
        language.addView(label("English is the default app language. Maya voice/conversation remains Sinhala.",12,MUTED));
        Button chooseLanguage=buttonStyle(new Button(this));
        chooseLanguage.setText("🌐  Change app language");
        chooseLanguage.setOnClickListener(v->showLanguagePicker());
        language.addView(chooseLanguage);
        body.addView(language);

        LinearLayout app=card();app.addView(label("🔔 NOTIFICATIONS",11,MUTED));Switch n=new Switch(this);n.setText("Notifications");n.setTextColor(TEXT);n.setTextSize(15);n.setChecked(prefs.getBoolean("notifications",true));n.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("notifications",c).apply());app.addView(n);body.addView(app);
        showCategory("🎨  DISPLAY","Theme and notification preferences.",body);
    }

    private void showLanguagePicker(){
        final String[] labels={"English","සිංහල"};
        final String[] tags={"en","si"};
        String current="en";
        if(Build.VERSION.SDK_INT>=33){
            try{
                android.app.LocaleManager lm=(android.app.LocaleManager)getSystemService(LocaleManager.class);
                if(lm!=null && !lm.getApplicationLocales().isEmpty()) current=lm.getApplicationLocales().get(0).getLanguage();
            }catch(Exception ignored){}
        }
        int checked=current.equals("si")?1:0;
        new AlertDialog.Builder(this)
            .setTitle("App language")
            .setSingleChoiceItems(labels,checked,(dialog,which)->{
                String lang=tags[which];
                prefs.getSharedPreferences("ui_settings",MODE_PRIVATE).edit().putBoolean("language_selected",true).apply();
                if(Build.VERSION.SDK_INT>=33){
                    try{
                        android.app.LocaleManager lm=(android.app.LocaleManager)getSystemService(LocaleManager.class);
                        if(lm!=null) lm.setApplicationLocales(android.os.LocaleList.forLanguageTags(lang));
                        dialog.dismiss();
                        Toast.makeText(this,lang.equals("si")?"App language changed to Sinhala.":"App language changed to English.",Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        Toast.makeText(this,"Could not change app language.",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    dialog.dismiss();
                    Toast.makeText(this,"App language selection is supported on Android 13+.",Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton("Cancel",null).show();
    }

    private void showSecuritySettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout p=card();p.addView(label("🔐 PERMISSIONS & PRIVACY",11,MUTED));p.addView(label("Open Android controls for permissions used by Maya.",12,MUTED));
        Button notify=buttonStyle(new Button(this));notify.setText("🔔  Notification access");notify.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});p.addView(notify);
        Button dnd=buttonStyle(new Button(this));dnd.setText("🔕  DND control access");dnd.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));}catch(Exception ignored){}});p.addView(dnd);body.addView(p);

        LinearLayout password=card();
        password.addView(label("🔑 ACCOUNT PASSWORD",11,MUTED));
        password.addView(label("Change the password directly while this account is signed in. No reset email is needed.",12,MUTED));
        Button changePassword=buttonStyle(new Button(this));
        changePassword.setText("🔐  Change password");
        changePassword.setOnClickListener(v->startActivity(new Intent(this,ChangePasswordActivity.class)));
        password.addView(changePassword);
        body.addView(password);

        LinearLayout key=card();key.addView(label("🔑 API KEY SAFETY",11,MUTED));key.addView(label("OpenAI and Tavily keys are never stored in the app. They stay in Supabase backend secrets.",12,MUTED));body.addView(key);
        showCategory("🔐  SECURITY & PRIVACY","Permissions, privacy and key safety.",body);
    }

    private void showPhoneSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout control=card();control.addView(label("📱 MAYA PHONE CONTROLS",11,MUTED));
        Button battery=buttonStyle(new Button(this));battery.setText("🔋  Battery / background settings");battery.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});control.addView(battery);
        Button help=buttonStyle(new Button(this));help.setText("🎙️  Maya command guide");help.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Maya commands").setMessage("Say “Maya” first.\n\n• Maya torch on/off\n• Maya volume up\n• Maya play / pause music\n• Maya call [contact]\n• Maya notifications\n• Maya DND on/off\n• Maya motivate me").setPositiveButton("OK",null).show());control.addView(help);body.addView(control);

        LinearLayout maya=card();maya.addView(label("🎙️ BACKGROUND ASSISTANT",11,MUTED));Switch bg=new Switch(this);bg.setText("Keep Maya available in background");bg.setTextColor(TEXT);bg.setTextSize(15);bg.setChecked(getSharedPreferences("maya_settings",MODE_PRIVATE).getBoolean("enabled",false));bg.setOnCheckedChangeListener((v,on)->{getSharedPreferences("maya_settings",MODE_PRIVATE).edit().putBoolean("enabled",on).apply();if(on){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},3101);bg.setChecked(false);return;}Intent i=new Intent(this,MayaAssistantService.class);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Toast.makeText(this,"Maya background assistant ON 🎙️",Toast.LENGTH_SHORT).show();}catch(Exception e){getSharedPreferences("maya_settings",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();bg.setChecked(false);}}else{stopService(new Intent(this,MayaAssistantService.class));Toast.makeText(this,"Maya background assistant OFF",Toast.LENGTH_SHORT).show();}});maya.addView(bg);
        Button def=buttonStyle(new Button(this)); def.setText("🤖  Set Maya as phone assistant"); def.setOnClickListener(v->{ requestMayaAssistantRole(); }); maya.addView(def);body.addView(maya);
        showCategory("📱  PHONE & BACKGROUND","Background assistant, phone controls and battery settings.",body);
    }

    private void requestMayaAssistantRole(){
        try{
            if(Build.VERSION.SDK_INT>=29){
                android.app.role.RoleManager rm=(android.app.role.RoleManager)getSystemService(Context.ROLE_SERVICE);
                if(rm!=null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)){
                    if(rm.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT)){
                        Toast.makeText(this,"Maya is already the phone assistant. 🤖",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT),3098);
                    return;
                }
            }
            Intent i;
            if(Build.VERSION.SDK_INT>=29) i=new Intent("android.settings.VOICE_INPUT_SETTINGS");
            else i=new Intent("android.settings.VOICE_INPUT_SETTINGS");
            startActivity(i);
        }catch(Exception e){
            try{ startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch(Exception ignored){ Toast.makeText(this,"Open Default apps → Digital assistant and choose Maya.",Toast.LENGTH_LONG).show(); }
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==3098 && Build.VERSION.SDK_INT>=29){
            try{
                android.app.role.RoleManager rm=(android.app.role.RoleManager)getSystemService(Context.ROLE_SERVICE);
                boolean held=rm!=null && rm.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT);
                Toast.makeText(this,held?"Maya is now your phone assistant. 🤖":"Maya was not selected as the phone assistant.",Toast.LENGTH_SHORT).show();
            }catch(Exception ignored){}
        }
    }

    private void showAccountSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout accounts=card();accounts.addView(label("👥 PRIMARY + SUB ACCOUNTS",11,MUTED));accounts.addView(label("Sign in, create Primary accounts, invite Sub accounts and sync progress.",12,MUTED));Button open=buttonStyle(new Button(this));open.setText("👥  Open Account Dashboard");open.setOnClickListener(v->startActivity(new Intent(this,AccountsActivity.class)));accounts.addView(open);body.addView(accounts);
        LinearLayout reset=card();reset.addView(label("⚠️ PROGRESS",11,MUTED));reset.addView(label("Reset the local 309-day progress and reminders.",12,MUTED));Button b=buttonStyle(new Button(this));b.setText("↻  Reset progress");if(SupabaseAccountManager.loggedIn(this)&&!SupabaseAccountManager.can(this,"can_reset_progress"))b.setEnabled(false);b.setOnClickListener(v->confirmReset());reset.addView(b);body.addView(reset);
        showCategory("👥  ACCOUNTS & DATA","Accounts, sync and progress controls.",body);
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Reset progress?").setMessage("This removes progress, custom habits and reminders, then starts a fresh 309-day program.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{SharedPreferences p=getSharedPreferences("discipline",MODE_PRIVATE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);for(String k:p.getAll().keySet())if(k.startsWith("alarm")){try{int id=Integer.parseInt(k.substring(5));PendingIntent pi=PendingIntent.getBroadcast(this,id,new Intent(this,AlarmReceiver.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);if(am!=null)am.cancel(pi);}catch(Exception ignored){}}Calendar resetDate=Calendar.getInstance();resetDate.set(Calendar.HOUR_OF_DAY,0);resetDate.set(Calendar.MINUTE,0);resetDate.set(Calendar.SECOND,0);resetDate.set(Calendar.MILLISECOND,0);p.edit().clear().putLong("program_start",resetDate.getTimeInMillis()).apply();Toast.makeText(this,"Progress reset 🔥",Toast.LENGTH_SHORT).show();}).show();
    }

    private void showAboutSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);LinearLayout about=card();about.addView(label("309 DAY DISCIPLINE",20,TEXT));about.addView(label("Build discipline. One day at a time.\nVersion 2.0 • Discipline + Maya",13,MUTED));body.addView(about);showCategory("ℹ️  ABOUT 309","App information and version.",body);
    }

    private void showMemoryManager(){try{MayaMemory m=new MayaMemory(this);String all=m.all();if(all.isEmpty()){new AlertDialog.Builder(this).setTitle("Maya memory").setMessage("No saved memory yet.").setPositiveButton("OK",null).show();return;}String[] items=all.split(String.valueOf((char)10));new AlertDialog.Builder(this).setTitle("Maya memory — tap one to delete").setItems(items,(d,which)->new AlertDialog.Builder(this).setTitle("Delete this memory?").setMessage(items[which]).setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{try{m.remove(which,()->runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())showMemoryManager();}));}catch(Exception e){Toast.makeText(this,"Could not delete memory.",Toast.LENGTH_SHORT).show();}}).show()).setNegativeButton("Close",null).show();}catch(Exception e){Toast.makeText(this,"Maya memory could not be opened.",Toast.LENGTH_SHORT).show();}}

    private void speak(String s){if(tts==null)tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS)speakNow(s);});else speakNow(s);}
    private void speakNow(String s){try{if(tts==null)return;float rate=.65f+(prefs.getInt("speech_speed",50)/100f)*.85f;tts.setLanguage(Locale.US);tts.setSpeechRate(rate);tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"discipline_settings");}catch(Exception ignored){}}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}if(tone!=null)tone.release();super.onDestroy();}
}
