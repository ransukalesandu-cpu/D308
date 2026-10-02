package com.discipline309.app;

import android.os.Build;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.role.RoleManager;
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
    private int BG=0xFF0A0E1A,SURFACE=0xB8151A2C,TEXT=0xFFF5F8FF,MUTED=0xFFA9A8C5,ACCENT=0xFF8A2BE2,GOLD=0xFFFFD700;
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private Button buttonStyle(Button b){b.setTextColor(TEXT);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(48));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(0xFF211038);g.setCornerRadius(dp(16));g.setStroke(dp(1),0x668A2BE2);b.setBackground(g);return b;}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(6),dp(4),dp(6));return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(12),dp(16),dp(12));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(SURFACE);g.setCornerRadius(dp(20));g.setStroke(dp(1),0x668A2BE2);l.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);try{if(SupabaseAccountManager.loggedIn(this)&&!SupabaseAccountManager.can(this,"can_access_settings")){new AlertDialog.Builder(this).setTitle("Settings restricted").setMessage("Your Primary account has disabled Settings access for this Sub account.").setPositiveButton("OK",(d,w)->finish()).show();return;}prefs=getSharedPreferences("settings",MODE_PRIVATE);String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");BG=0xFF0A0E1A;SURFACE=0xB8151A2C;TEXT=0xFFF5F8FF;MUTED=0xFFA9A8C5;ACCENT=0xFF8A2BE2;GOLD=0xFFFFD700;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildUi();}catch(Exception e){Toast.makeText(this,"Settings could not open safely.",Toast.LENGTH_SHORT).show();finish();}}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF0A0E1A,0xFF000000});root.setBackground(bg);
        TextView t=label("⚙  Settings",27,TEXT);t.setTypeface(null,1);root.addView(t);
        root.addView(label("MAYA • 309 DAY DISCIPLINE",12,ACCENT));root.addView(label("Choose a category to edit your settings.",13,MUTED));

        addCategory(root,"🤖  AI ASSISTANT","Maya voice, AI brain, web search and memory.",v->showAiSettings());
        addCategory(root,"📱  PHONE ASSISTANT","Set Maya as your Android phone assistant.",v->showPhoneAssistantSettings());
        addCategory(root,"🎭  MODES","Maya personality and background motivation modes.",v->showModeSettings());
        addCategory(root,"🎨  DISPLAY","Theme and notification preferences.",v->showDisplaySettings());
        addCategory(root,"🔒  ADVANCED / STRICT MODE","No-skip discipline, distracting-app limits and temporary DND control.",v->showStrictSettings());
        addCategory(root,"👥  ACCOUNTS & DATA","Primary/Sub accounts and progress controls.",v->showAccountSettings());
        addCategory(root,"ℹ️  ABOUT 309","App information and version.",v->showAboutSettings());

        scroll.addView(root);setContentView(scroll);
    }

    private void addCategory(LinearLayout root,String titleText,String desc,View.OnClickListener action){
        LinearLayout box=card();
        box.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView title=label(titleText,17,TEXT);title.setTypeface(null,1);box.addView(title);
        box.addView(label(desc,12,MUTED));
        box.setElevation(dp(2));
        Button open=new Button(this);open=buttonStyle(open);open.setText("Open  ›");open.setGravity(Gravity.CENTER);open.setOnClickListener(action);box.addView(open);
        root.addView(box);
    }

    private LinearLayout categoryLayout(String titleText,String desc){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(28));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF0A0E1A,0xFF000000});root.setBackground(bg);
        TextView t=label(titleText,25,TEXT);t.setTypeface(null,1);root.addView(t);root.addView(label(desc,13,MUTED));return root;
    }
    private ScrollView categoryScroll(LinearLayout root){ScrollView s=new ScrollView(this);s.addView(root);return s;}
    private void showCategory(String titleText,String desc,LinearLayout body){
        LinearLayout wrapper=categoryLayout(titleText,desc);
        Button back=buttonStyle(new Button(this));back.setText("←  Back to Settings");back.setOnClickListener(v->buildUi());wrapper.addView(back);
        wrapper.addView(body);
        setContentView(categoryScroll(wrapper));
    }


    private void showStrictSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout strict=card();strict.addView(label("🔒 STRICT MODE",11,GOLD));
        strict.addView(label("When ON: today cannot be completed until every required task is finished. Selected distracting apps are limited by a daily time cap.",12,MUTED));
        Switch on=new Switch(this);on.setText("Enable Advanced / Strict Mode");on.setTextColor(TEXT);on.setTextSize(16);on.setChecked(StrictModeManager.isEnabled(this));
        on.setOnCheckedChangeListener((v,c)->{StrictModeManager.setEnabled(this,c);if(c)Toast.makeText(this,"Strict Mode ON 🔒",Toast.LENGTH_SHORT).show();else Toast.makeText(this,"Strict Mode OFF",Toast.LENGTH_SHORT).show();});strict.addView(on);body.addView(strict);

        LinearLayout alarms=card();alarms.addView(label("⏰ STRICT ALARMS",11,GOLD));
        alarms.addView(label("These alarms are separate from your normal Discipline alarms. They ring only while Strict Mode is ON.",12,MUTED));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<StrictAlarmManager.count(this);i++){final int idx=i;TextView a=label("⏰ "+StrictAlarmManager.time(this,i)+"  •  "+StrictAlarmManager.title(this,i),14,TEXT);a.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Remove Strict Alarm?").setMessage(StrictAlarmManager.title(this,idx)+"\n"+StrictAlarmManager.time(this,idx)).setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{StrictAlarmManager.remove(this,idx);showStrictSettings();}).show());list.addView(a);}
        alarms.addView(list);
        Button addAlarm=buttonStyle(new Button(this));addAlarm.setText("➕  Add Strict Alarm");addAlarm.setOnClickListener(v->showStrictAlarmDialog());alarms.addView(addAlarm);
        body.addView(alarms);

        LinearLayout apps=card();apps.addView(label("📵 DISTRACTING APPS",11,MUTED));
        apps.addView(label("Choose apps Maya should monitor. You must enable Android Accessibility access for the blocker to work.",12,MUTED));
        Button choose=buttonStyle(new Button(this));choose.setText("📱  Choose apps + daily limit");choose.setOnClickListener(v->showStrictAppPicker());apps.addView(choose);
        Button access=buttonStyle(new Button(this));access.setText("♿  Enable Strict Mode app control");access.setOnClickListener(v->{try{startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));}catch(Exception ignored){}});apps.addView(access);body.addView(apps);

        LinearLayout dnd=card();dnd.addView(label("🌙 DO NOT DISTURB",11,MUTED));
        Switch auto=new Switch(this);auto.setText("Auto DND while Strict Mode is ON");auto.setTextColor(TEXT);auto.setTextSize(15);auto.setChecked(StrictModeManager.p(this).getBoolean("auto_dnd",false));
        auto.setOnCheckedChangeListener((v,c)->{StrictModeManager.p(this).edit().putBoolean("auto_dnd",c).apply();if(c&&StrictModeManager.isEnabled(this))StrictModeManager.setDnd(this,true);});dnd.addView(auto);
        Button dndAccess=buttonStyle(new Button(this));dndAccess.setText("🌙  Allow Maya to control DND");dndAccess.setOnClickListener(v->StrictModeManager.openDndAccess(this));dnd.addView(dndAccess);
        dnd.addView(label("After you tell Maya to turn DND off, she can restore it automatically after the number of minutes you choose.",12,MUTED));body.addView(dnd);

        LinearLayout rules=card();rules.addView(label("🧱 STRICT RULES",11,MUTED));
        Switch noSkip=new Switch(this);noSkip.setText("No-skip / no-undo tasks");noSkip.setTextColor(TEXT);noSkip.setChecked(StrictModeManager.p(this).getBoolean("no_skip",true));
        noSkip.setOnCheckedChangeListener((v,c)->StrictModeManager.p(this).edit().putBoolean("no_skip",c).apply());rules.addView(noSkip);
        Button focus=buttonStyle(new Button(this));focus.setText("🎯  Start Focus Session");focus.setOnClickListener(v->showFocusDialog());rules.addView(focus);
        Button schedule=buttonStyle(new Button(this));schedule.setText("⏰  Schedule Strict Mode");schedule.setOnClickListener(v->showStrictScheduleDialog());rules.addView(schedule);
        Button clearSchedule=buttonStyle(new Button(this));clearSchedule.setText("✕  Remove Strict schedule");clearSchedule.setOnClickListener(v->{StrictModeManager.clearSchedule(this);Toast.makeText(this,"Strict schedule removed.",Toast.LENGTH_SHORT).show();});rules.addView(clearSchedule);
        body.addView(rules);

        LinearLayout allow=card();allow.addView(label("🛡️ ESSENTIAL APP WHITELIST",11,MUTED));
        allow.addView(label("Whitelisted apps stay usable while Strict Mode is active. Use this only for genuinely essential apps.",12,MUTED));
        Button wl=buttonStyle(new Button(this));wl.setText("🛡️  Choose essential apps");wl.setOnClickListener(v->showWhitelistPicker());allow.addView(wl);body.addView(allow);

        LinearLayout stats=card();stats.addView(label("📊 STRICT PROGRESS",11,MUTED));
        stats.addView(label("Selected apps: "+StrictModeManager.blocked(this).size()+"    •    Daily limit: "+StrictModeManager.limitMinutes(this)+" min",13,TEXT));
        stats.addView(label("Focus sessions use DND while active. App usage counters reset by date.",12,MUTED));body.addView(stats);

        LinearLayout info=card();info.addView(label("⚠️ ANDROID LIMIT",11,GOLD));info.addView(label("App blocking uses Android Accessibility and only works after you explicitly enable Maya's service. Android/system apps are never targeted by this feature.",12,MUTED));body.addView(info);
        showCategory("🔒  ADVANCED / STRICT MODE","Harder discipline rules, app limits and DND control.",body);
    }

    private void showStrictAlarmDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(8),dp(18),dp(8));
        EditText name=new EditText(this);name.setHint("Alarm name e.g. Wake up / Study");name.setTextColor(TEXT);name.setHintTextColor(MUTED);l.addView(name);
        TimePicker tp=new TimePicker(this);tp.setIs24HourView(true);l.addView(tp);
        new AlertDialog.Builder(this).setTitle("⏰ Add Strict Alarm").setMessage("This alarm is active only while Strict Mode is ON. Existing normal alarms are unchanged.")
          .setView(l).setPositiveButton("SAVE",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty())n="Strict Alarm";StrictAlarmManager.add(this,n,tp.getHour(),tp.getMinute());Toast.makeText(this,"Strict alarm saved.",Toast.LENGTH_SHORT).show();})
          .setNegativeButton("CANCEL",null).show();
    }

    private void showStrictAppPicker(){
        final java.util.ArrayList<android.content.pm.ResolveInfo> apps=new java.util.ArrayList<>();
        Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);
        try{apps.addAll(getPackageManager().queryIntentActivities(q,0));}catch(Exception ignored){}
        java.util.Collections.sort(apps,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        final String[] labels=new String[apps.size()];final boolean[] checked=new boolean[apps.size()];java.util.Set<String> selected=StrictModeManager.blocked(this);
        int n=0;for(int i=0;i<apps.size();i++){String pkg=apps.get(i).activityInfo.packageName;if(pkg.equals(getPackageName())){labels[i]="Maya (protected)";checked[i]=false;}else{labels[i]=apps.get(i).loadLabel(getPackageManager()).toString();checked[i]=selected.contains(pkg);n++;}}
        new AlertDialog.Builder(this).setTitle("Select distracting apps").setMultiChoiceItems(labels,checked,(d,w,c)->checked[w]=c)
            .setPositiveButton("SAVE",(d,w)->{java.util.Set<String> out=new java.util.HashSet<>();for(int i=0;i<apps.size();i++)if(checked[i]&&!apps.get(i).activityInfo.packageName.equals(getPackageName()))out.add(apps.get(i).activityInfo.packageName);StrictModeManager.setBlocked(this,out);showStrictLimitDialog();})
            .setNegativeButton("CANCEL",null).show();
    }

    private void showStrictLimitDialog(){
        final String[] choices={"15 minutes/day","30 minutes/day","45 minutes/day","60 minutes/day","90 minutes/day","120 minutes/day"};
        int cur=StrictModeManager.limitMinutes(this),idx=1;int[] vals={15,30,45,60,90,120};for(int i=0;i<vals.length;i++)if(vals[i]==cur)idx=i;
        final int[] pick={idx};new AlertDialog.Builder(this).setTitle("Daily app limit").setSingleChoiceItems(choices,idx,(d,w)->pick[0]=w)
            .setPositiveButton("SAVE",(d,w)->{StrictModeManager.p(this).edit().putInt("limit_minutes",vals[pick[0]]).apply();Toast.makeText(this,choices[pick[0]]+" saved.",Toast.LENGTH_SHORT).show();}).setNegativeButton("CANCEL",null).show();
    }

    private void showFocusDialog(){
        final String[] c={"15 minutes","30 minutes","45 minutes","60 minutes","90 minutes","120 minutes"};final int[] v={15,30,45,60,90,120};final int[] pick={1};
        new AlertDialog.Builder(this).setTitle("🎯 Focus Session").setSingleChoiceItems(c,1,(d,w)->pick[0]=w)
            .setPositiveButton("START",(d,w)->{StrictModeManager.startFocus(this,v[pick[0]]);Toast.makeText(this,"Focus started 🔒",Toast.LENGTH_SHORT).show();})
            .setNegativeButton("CANCEL",null).show();
    }
    private void showStrictScheduleDialog(){
        final TimePicker tp1=new TimePicker(this);tp1.setIs24HourView(true);tp1.setHour(6);tp1.setMinute(0);
        final TimePicker tp2=new TimePicker(this);tp2.setIs24HourView(true);tp2.setHour(22);tp2.setMinute(0);
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(8),dp(18),dp(8));
        l.addView(label("START TIME",12,MUTED));l.addView(tp1);l.addView(label("END TIME",12,MUTED));l.addView(tp2);
        new AlertDialog.Builder(this).setTitle("⏰ Scheduled Strict Mode").setMessage("Strict Mode will automatically turn ON at start and OFF at end. It is rescheduled after reboot/time changes.")
            .setView(l).setPositiveButton("SAVE",(d,w)->{StrictModeManager.scheduleStrict(this,tp1.getHour(),tp1.getMinute(),tp2.getHour(),tp2.getMinute());Toast.makeText(this,"Strict schedule saved.",Toast.LENGTH_SHORT).show();}).setNegativeButton("CANCEL",null).show();
    }
    private void showWhitelistPicker(){
        final java.util.ArrayList<android.content.pm.ResolveInfo> apps=new java.util.ArrayList<>();
        Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);try{apps.addAll(getPackageManager().queryIntentActivities(q,0));}catch(Exception ignored){}
        java.util.Collections.sort(apps,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        final String[] labels=new String[apps.size()];final boolean[] checked=new boolean[apps.size()];java.util.Set<String> selected=StrictModeManager.whitelisted(this);
        for(int i=0;i<apps.size();i++){labels[i]=apps.get(i).loadLabel(getPackageManager()).toString();checked[i]=selected.contains(apps.get(i).activityInfo.packageName);}
        new AlertDialog.Builder(this).setTitle("Essential apps").setMultiChoiceItems(labels,checked,(d,w,c)->checked[w]=c)
            .setPositiveButton("SAVE",(d,w)->{java.util.Set<String> out=new java.util.HashSet<>();for(int i=0;i<apps.size();i++)if(checked[i])out.add(apps.get(i).activityInfo.packageName);StrictModeManager.setWhitelist(this,out);Toast.makeText(this,"Whitelist saved.",Toast.LENGTH_SHORT).show();})
            .setNegativeButton("CANCEL",null).show();
    }

    private void showAiSettings(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout voice=card();voice.addView(label("🎙️ AI VOICE",11,MUTED));
        Switch speak=new Switch(this);speak.setText("Auto speak AI responses");speak.setTextColor(TEXT);speak.setTextSize(15);speak.setChecked(prefs.getBoolean("auto_speak",true));speak.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("auto_speak",c).apply());voice.addView(speak);
        Switch backgroundVoice=new Switch(this);backgroundVoice.setText("Maya background auto-talk");backgroundVoice.setTextColor(TEXT);backgroundVoice.setTextSize(15);backgroundVoice.setChecked(prefs.getBoolean("maya_background_voice",false));backgroundVoice.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("maya_background_voice",on).apply();Intent i=new Intent(this,MayaAssistantService.class);if(on){try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Toast.makeText(this,"Maya background auto-talk ON 🎙️",Toast.LENGTH_SHORT).show();}catch(Exception e){prefs.edit().putBoolean("maya_background_voice",false).apply();v.setChecked(false);Toast.makeText(this,"Android could not start Maya background voice.",Toast.LENGTH_SHORT).show();}}else{stopService(i);Toast.makeText(this,"Maya background auto-talk OFF",Toast.LENGTH_SHORT).show();}});voice.addView(backgroundVoice);voice.addView(label("Maya gives occasional Sinhala/funny discipline check-ins while this is ON. Android may limit background microphone access; this mode uses spoken check-ins, not hidden recording.",12,MUTED));
        voice.addView(label("Speech speed",14,TEXT));SeekBar speed=new SeekBar(this);speed.setMax(100);speed.setProgress(prefs.getInt("speech_speed",50));voice.addView(speed);TextView speedText=label("Normal",12,MUTED);voice.addView(speedText);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){String s=p<30?"Slow":p>70?"Fast":"Normal";speedText.setText("Speech speed: "+s);if(u)prefs.edit().putInt("speech_speed",p).apply();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        Button test=buttonStyle(new Button(this));test.setText("🔊  Test AI voice");test.setOnClickListener(v->speak("Your Discipline assistant is ready."));voice.addView(test);

        LinearLayout mayaVoice=card();
        mayaVoice.addView(label("👩  MAYA VOICE",11,MUTED));
        mayaVoice.addView(label("Choose the voice style Maya uses for spoken replies. Available voices depend on the TTS voices installed on your phone.",12,MUTED));
        Button chooseVoice=buttonStyle(new Button(this));
        chooseVoice.setText("👩  Change Maya voice");
        chooseVoice.setOnClickListener(x->showMayaVoicePicker());
        mayaVoice.addView(chooseVoice);
        body.addView(mayaVoice);

        LinearLayout language=card();
        language.addView(label("🌐 MAYA LANGUAGE",11,MUTED));
        language.addView(label("Auto uses your phone region: Sri Lanka → Sinhala; other regions → English. You can override this anytime.",12,MUTED));
        Button chooseMayaLanguage=buttonStyle(new Button(this));
        chooseMayaLanguage.setText("🌐  Change Maya language");
        chooseMayaLanguage.setOnClickListener(v->showMayaLanguagePicker());
        language.addView(chooseMayaLanguage);
        body.addView(language);

        LinearLayout ai=card();ai.addView(label("🧠 MAYA AI BRAIN",11,MUTED));
        ai.addView(label("Maya AI is connected securely through the Supabase backend. No OpenAI API key is stored in this app.",12,MUTED));
        ai.addView(label("Model: Gemini • Fast + Deep AI",13,TEXT));
        body.addView(ai);

        LinearLayout web=card();web.addView(label("🌐 WEB SEARCH",11,MUTED));
        web.addView(label("Current web searches are handled securely by the Supabase backend.",12,MUTED));
        body.addView(web);

        LinearLayout mem=card();mem.addView(label("🧠 MAYA MEMORY",11,MUTED));mem.addView(label("Saved memory stays on this phone.",12,MUTED));
        Button view=buttonStyle(new Button(this));view.setText("👀  View / delete saved memory");view.setOnClickListener(v->showMemoryManager());mem.addView(view);
        Button clear=buttonStyle(new Button(this));clear.setText("🗑  Clear all Maya memory");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear Maya memory?").setMessage("This removes all saved ordinary facts and preferences.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{try{new MayaMemory(this).clear();Toast.makeText(this,"Maya memory cleared.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Could not clear memory.",Toast.LENGTH_SHORT).show();}}).show());mem.addView(clear);body.addView(mem);

                showCategory("🤖  AI ASSISTANT","Maya AI, voice, web search and memory.",body);
    }

    private void setMayaAsPhoneAssistant(){
        try{
            if(Build.VERSION.SDK_INT>=29){
                RoleManager rm=(RoleManager)getSystemService(RoleManager.class);
                if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){
                    if(rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)){
                        Toast.makeText(this,"Maya is already your phone assistant ✨",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),3091);
                    return;
                }
            }
            startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS"));
        }catch(Exception e){
            try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}
            catch(Exception ignored){Toast.makeText(this,"Android assistant settings could not be opened.",Toast.LENGTH_SHORT).show();}
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==3091 && Build.VERSION.SDK_INT>=29){
            try{
                RoleManager rm=(RoleManager)getSystemService(RoleManager.class);
                if(rm!=null && rm.isRoleHeld(RoleManager.ROLE_ASSISTANT))
                    Toast.makeText(this,"Maya is now your phone assistant ✨",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this,"Maya was not selected as the phone assistant.",Toast.LENGTH_SHORT).show();
            }catch(Exception ignored){}
        }
    }

    private void showPhoneAssistantSettings(){
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout assistant=card();
        assistant.addView(label("✨ MAYA AS PHONE ASSISTANT",11,MUTED));
        assistant.addView(label("Open Android's assistant selection and choose Maya as your default phone assistant when your device allows it.",12,MUTED));
        Button set=buttonStyle(new Button(this));
        set.setText("✨  Set Maya as phone assistant");
        set.setOnClickListener(v->setMayaAsPhoneAssistant());
        assistant.addView(set);
        body.addView(assistant);

        LinearLayout info=card();
        info.addView(label("ℹ️ ANDROID NOTE",11,MUTED));
        info.addView(label("Android decides which apps are eligible for the Assistant role. If Maya is not shown, this app version/device may not expose the required assistant service.",12,MUTED));
        body.addView(info);

        showCategory("📱  PHONE ASSISTANT","Choose Maya as your Android assistant.",body);
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
        LinearLayout app=card();app.addView(label("🔔 NOTIFICATIONS",11,MUTED));Switch n=new Switch(this);n.setText("Notifications");n.setTextColor(TEXT);n.setTextSize(15);n.setChecked(prefs.getBoolean("notifications",true));n.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("notifications",c).apply());app.addView(n);body.addView(app);
        showCategory("🎨  DISPLAY","Theme and notification preferences.",body);
    }

    private void showMayaVoicePicker(){
        final String[] labels={"🌸 Aria","💗 Luna","✨ Ava"};
        int current=Math.max(0,Math.min(2,prefs.getInt("maya_voice",0)));
        new AlertDialog.Builder(this).setTitle("Maya voice")
            .setSingleChoiceItems(labels,current,(dialog,which)->{
                prefs.edit().putInt("maya_voice",which).apply();
                dialog.dismiss();
                String msg=which==0?"Aria selected.":which==1?"Luna selected.":"Ava selected.";
                Toast.makeText(this,msg+" Test AI voice to hear the style.",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel",null).show();
    }

    private void showMayaLanguagePicker(){
        final String[] labels={"Auto (region)","සිංහල","English"};
        final String[] values={"auto","si","en"};
        String current=prefs.getString("maya_language","auto");
        int checked=0;
        for(int i=0;i<values.length;i++) if(values[i].equals(current)) checked=i;
        new AlertDialog.Builder(this)
            .setTitle("Maya language")
            .setSingleChoiceItems(labels,checked,(dialog,which)->{
                prefs.edit().putString("maya_language",values[which]).apply();
                dialog.dismiss();
                String msg=which==0
                    ? "Maya will choose the default language from your region."
                    : (which==1 ? "Maya language set to Sinhala." : "Maya language set to English.");
                Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel",null).show();
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
