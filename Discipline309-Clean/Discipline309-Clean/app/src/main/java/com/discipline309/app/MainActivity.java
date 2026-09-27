package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS = "discipline";
    private static final String[] TASKS = {
            "Wake up on time", "Study / learning", "Workout or active recovery",
            "Eat planned meals", "No-phone block", "Night review + prepare tomorrow"
    };
    private static final int BG=0xFF0B0E14, SURFACE=0xFF191D27, TEXT=0xFFF7F8FC, MUTED=0xFFAAB2C3, ACCENT=0xFF63E6BE;
    private SharedPreferences prefs;
    private LinearLayout content;
    private TextView homeStats, homeStreak;
    private VoiceAssistant voiceAssistant;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(SURFACE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));c.setLayoutParams(p);return c;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(48));return b;}
    private String key(){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());}
    private String key(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE); voiceAssistant=new VoiceAssistant(this); buildShell(); showHome();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
    }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(16),dp(18),dp(10));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(4),dp(5),dp(4),dp(5));nav.setBackgroundColor(0xFF11151E);
        String[] names={"⌂\nHome","▥\nProgress","♧\nReminders","⚙\nSettings"};
        for(int i=0;i<4;i++){final int n=i;Button b=button(names[i]);b.setTextSize(12);b.setPadding(0,0,0,0);b.setOnClickListener(v->{if(n==0)showHome();else if(n==1)showProgress();else if(n==2)showReminders();else openSettings();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));}
        root.addView(nav);setContentView(root);
    }

    private void baseHeader(String title,String sub){
        content.removeAllViews();
        TextView t=label(title,26,TEXT);t.setTypeface(null,1);content.addView(t);
        content.addView(label(sub,13,MUTED));
    }

    private void showHome(){
        baseHeader("309 DAY DISCIPLINE","Build discipline. One day at a time. 🔥");
        LinearLayout hero=card();hero.addView(label("TARGET",11,MUTED));hero.addView(label("01 AUG 2027",23,TEXT));
        homeStats=label("",14,0xFFDCE1EA);hero.addView(homeStats);content.addView(hero);
        LinearLayout st=card();st.addView(label("CURRENT STREAK",11,MUTED));homeStreak=label("",22,TEXT);homeStreak.setTypeface(null,1);st.addView(homeStreak);content.addView(st);
        TextView sec=label("TODAY'S PLAN",19,TEXT);sec.setTypeface(null,1);sec.setPadding(4,dp(16),4,dp(4));content.addView(sec);
        for(int i=0;i<TASKS.length;i++){final int idx=i;LinearLayout row=card();CheckBox cb=new CheckBox(this);cb.setText(TASKS[i]);cb.setTextColor(TEXT);cb.setTextSize(15);cb.setChecked(prefs.getBoolean("t"+i+key(),false));cb.setOnCheckedChangeListener((v,c)->{prefs.edit().putBoolean("t"+idx+key(),c).apply();refreshHome();});row.addView(cb);content.addView(row);}
        Button complete=button("✓  COMPLETE TODAY'S CHALLENGE");complete.setOnClickListener(v->completeDay());content.addView(complete);
        LinearLayout actions=card();actions.addView(label("QUICK ACTIONS",11,MUTED));
        Button r=button("⏰  Manage reminders");r.setOnClickListener(v->showReminders());actions.addView(r);
        Button a=button("🎙️  Talk to Maya");a.setOnClickListener(v->voiceAssistant.start());actions.addView(a);
        Button chat=button("💬  Text Assistant");chat.setOnClickListener(v->chatDialog());actions.addView(chat);content.addView(actions);refreshHome();
    }

    private void refreshHome(){
        if(homeStats==null)return;int done=countFor(key());int pct=Math.round(done*100f/TASKS.length);long days=Math.max(0,(target().getTimeInMillis()-System.currentTimeMillis())/86400000L);
        homeStats.setText("Today  "+pct+"%   •   "+done+"/"+TASKS.length+" tasks   •   "+days+" days until target");
        homeStreak.setText("🔥  "+prefs.getInt("streak",0)+" days   •   🏆 Best "+prefs.getInt("best",0));
    }

    private int countFor(String k){int n=0;for(int i=0;i<TASKS.length;i++)if(prefs.getBoolean("t"+i+k,false))n++;return n;}
    private Calendar target(){Calendar c=Calendar.getInstance();c.set(2027,Calendar.AUGUST,1,0,0,0);c.set(Calendar.MILLISECOND,0);return c;}

    private void showProgress(){
        baseHeader("Progress","See your consistency and recent history.");
        int today=countFor(key()), total=0, perfect=0;
        for(int d=0;d<14;d++){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-d);int n=countFor(key(c));total+=n;if(n==TASKS.length)perfect++;}
        LinearLayout summary=card();summary.addView(label("LAST 14 DAYS",11,MUTED));summary.addView(label(total+" tasks completed",23,TEXT));summary.addView(label(perfect+" complete days",14,MUTED));content.addView(summary);
        TextView h=label("RECENT ACTIVITY",18,TEXT);h.setTypeface(null,1);content.addView(h);
        for(int d=0;d<14;d++){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-d);String k=key(c);int n=countFor(k);int pct=Math.round(n*100f/TASKS.length);LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);TextView date=label(new SimpleDateFormat("EEE, dd MMM",Locale.US).format(c.getTime()),14,TEXT);row.addView(date,new LinearLayout.LayoutParams(0,-2,1));row.addView(label(n==TASKS.length?"✓":n>0?"•":"—",18,n==TASKS.length?ACCENT:MUTED));row.addView(label("  "+pct+"%",13,MUTED));content.addView(row);}
    }

    private void showReminders(){
        baseHeader("Reminders","Daily alarms that keep your plan on track.");
        Button add=button("+  ADD DAILY REMINDER");add.setOnClickListener(v->alarmDialog());content.addView(add);
        TextView h=label("SAVED REMINDERS",18,TEXT);h.setTypeface(null,1);content.addView(h);
        boolean found=false;
        for(String k:prefs.getAll().keySet())if(k.startsWith("alarm")){String v=prefs.getString(k,"");String[] p=v.split("\\|",-1);if(p.length==3){found=true;LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.addView(label("🔔  "+(p[0].trim().isEmpty()?"Discipline reminder":p[0]),15,TEXT),new LinearLayout.LayoutParams(0,-2,1));row.addView(label(String.format(Locale.US,"%02d:%02d",Integer.parseInt(p[1]),Integer.parseInt(p[2])),15,ACCENT));content.addView(row);}}
        if(!found)content.addView(label("No reminders yet. Add your first one above.",14,MUTED));
        content.addView(label("Tip: Android may ask for notification and exact-alarm permission.",12,MUTED));
    }

    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Exception e){settingsDialog();}}
    private void completeDay(){
        String k=key();if(prefs.getBoolean("done"+k,false)){toast("Today is already completed. 🔥");return;}
        if(countFor(k)!=TASKS.length){toast("Finish all 6 tasks first.");return;}
        int streak=prefs.getInt("streak",0)+1;int best=Math.max(prefs.getInt("best",0),streak);
        prefs.edit().putInt("streak",streak).putInt("best",best).putBoolean("done"+k,true).apply();toast("Day completed! 🔥");refreshHome();
    }

    private void alarmDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(8),0,dp(8),0);
        EditText name=new EditText(this);name.setHint("Reminder name");l.addView(name);TimePicker p=new TimePicker(this);p.setIs24HourView(true);l.addView(p);
        new AlertDialog.Builder(this).setTitle("Add daily reminder").setView(l).setPositiveButton("SAVE",(d,w)->{schedule(name.getText().toString(),p.getHour(),p.getMinute());showReminders();toast("Reminder saved");}).setNegativeButton("CANCEL",null).show();
    }
    private void schedule(String name,int hour,int minute){
        int id=(name+hour+minute).hashCode();Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,hour);c.set(Calendar.MINUTE,minute);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
        scheduleStatic(this,name,hour,minute,id,c.getTimeInMillis());prefs.edit().putString("alarm"+id,name+"|"+hour+"|"+minute).apply();
    }
    public static void scheduleAll(Context context){SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);for(String k:p.getAll().keySet())if(k.startsWith("alarm")){String v=p.getString(k,null);if(v==null)continue;String[] a=v.split("\\|",-1);if(a.length==3)try{scheduleStatic(context,a[0],Integer.parseInt(a[1]),Integer.parseInt(a[2]),k.substring(5).hashCode(),-1);}catch(Exception ignored){}}}
    private static void scheduleStatic(Context c,String name,int h,int m,int id,long requested){
        Calendar x=Calendar.getInstance();if(requested>0)x.setTimeInMillis(requested);else{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,m);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())x.add(Calendar.DAY_OF_YEAR,1);}
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return;
        Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("title",name==null||name.trim().isEmpty()?"Discipline reminder":name);in.putExtra("msg","It's time. Start your next task. 🔥");
        PendingIntent pi=PendingIntent.getBroadcast(c,id,in,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);
    }
    private void chatDialog(){EditText input=new EditText(this);input.setHint("Ask in Sinhala or English...");new AlertDialog.Builder(this).setTitle("💬 Discipline Assistant").setMessage("I'm here to help you start your next step.").setView(input).setPositiveButton("SEND",(d,w)->toast(localReply(input.getText().toString()))).setNegativeButton("CLOSE",null).show();}
    private String localReply(String q){q=q.toLowerCase(Locale.ROOT);if(q.contains("sleep")||q.contains("නින්ද"))return"Put the phone away and get ready for sleep. 🌙";if(q.contains("can't")||q.contains("බැහැ"))return"Start with one small task. 🔥";return"Start with one small step now. You've got this. 🔥";}
    private void settingsDialog(){new AlertDialog.Builder(this).setTitle("Settings").setMessage("Open Settings from the bottom navigation to manage voice, notifications and progress.").setPositiveButton("OK",null).show();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){if(voiceAssistant!=null)voiceAssistant.destroy();super.onDestroy();}
}
