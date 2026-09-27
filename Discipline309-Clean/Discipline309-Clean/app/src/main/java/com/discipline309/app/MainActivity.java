package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS="discipline";
    private static final String[] DEFAULT_TASKS={
        "Wake up on time","Study / learning","Workout or active recovery",
        "Eat planned meals","No-phone block","Night review + prepare tomorrow"
    };
    private int BG=0xFF0B0E14,SURFACE=0xFF191D27,TEXT=0xFFF7F8FC,MUTED=0xFFAAB2C3,ACCENT=0xFF63E6BE;
    private SharedPreferences prefs;
    private LinearLayout content;
    private VoiceAssistant voiceAssistant;
    private ToneGenerator tone;

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void sound(int t){try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,65);tone.startTone(t,80);}catch(Exception ignored){}}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}
    private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(shape(SURFACE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP)sound(ToneGenerator.TONE_PROP_ACK);return false;});return b;}
    private String key(){return key(Calendar.getInstance());}
    private String key(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());}
    private Calendar startDate(){Calendar c=target();c.add(Calendar.DAY_OF_YEAR,-308);return c;}
    private Calendar target(){Calendar c=Calendar.getInstance();c.set(2027,Calendar.AUGUST,1,0,0,0);c.set(Calendar.MILLISECOND,0);return c;}
    private int daysFromStart(){long diff=System.currentTimeMillis()-startDate().getTimeInMillis();return(int)(diff/86400000L)+1;}
    private int dayNumber(){return Math.max(0,Math.min(309,daysFromStart()));}
    private int taskCount(){return DEFAULT_TASKS.length;}
    private String taskName(int i){return i<DEFAULT_TASKS.length?DEFAULT_TASKS[i]:prefs.getString("habit_"+i,"Habit");}
    private int customCount(){return prefs.getInt("custom_count",0);}
    private int totalTasks(){return DEFAULT_TASKS.length+customCount();}
    private boolean checked(int i,String d){return prefs.getBoolean("task_"+i+"_"+d,false);}
    private void setChecked(int i,String d,boolean v){prefs.edit().putBoolean("task_"+i+"_"+d,v).apply();}
    private int countFor(String d){int n=0;for(int i=0;i<totalTasks();i++)if(checked(i,d))n++;return n;}
    private int completedDays(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){if(prefs.getBoolean("done_"+key(c),false))n++;c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int xp(){return completedDays()*100+totalCompletedTasks()*20;}
    private int totalCompletedTasks(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){n+=countFor(key(c));c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int level(){return xp()/500+1;}
    private int currentStreak(){int n=0;Calendar c=Calendar.getInstance();if(!prefs.getBoolean("done_"+key(c),false))c.add(Calendar.DAY_OF_YEAR,-1);while(!c.before(startDate())&&prefs.getBoolean("done_"+key(c),false)){n++;c.add(Calendar.DAY_OF_YEAR,-1);}return n;}
    private int bestStreak(){int best=0,run=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){if(prefs.getBoolean("done_"+key(c),false))run++;else run=0;best=Math.max(best,run);c.add(Calendar.DAY_OF_YEAR,1);}return Math.max(best,prefs.getInt("best",0));}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences(PREFS,MODE_PRIVATE);applyTheme();
        voiceAssistant=new VoiceAssistant(this);buildShell();showHome();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
    }
    private void applyTheme(){
        String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");
        if("neon".equals(t)){BG=0xFF05050A;SURFACE=0xFF101525;TEXT=0xFFFFFFFF;MUTED=0xFF9CA8C7;ACCENT=0xFF00E5FF;}
        else if("soft".equals(t)){BG=0xFFF6F3F8;SURFACE=0xFFFFFFFF;TEXT=0xFF25222B;MUTED=0xFF77727F;ACCENT=0xFFB56CFF;}
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
    }
    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(14),dp(18),dp(12));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(2),dp(4),dp(2),dp(4));nav.setBackgroundColor(0xFF11151E);
        String[] names={"⌂\nHome","▣\nJourney","✓\nHabits","◫\nStats","⚙\nSettings"};
        for(int i=0;i<5;i++){final int n=i;Button b=button(names[i]);b.setTextSize(11);b.setPadding(0,0,0,0);b.setOnClickListener(v->{if(n==0)showHome();else if(n==1)showJourney();else if(n==2)showHabits();else if(n==3)showStats();else openSettings();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));}
        root.addView(nav);setContentView(root);
    }
    private void header(String title,String sub){
        content.removeAllViews();TextView t=label(title,26,TEXT);t.setTypeface(null,1);content.addView(t);content.addView(label(sub,13,MUTED));
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_discipline);icon.setContentDescription("Discipline");icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(64),dp(64));ip.gravity=Gravity.CENTER_HORIZONTAL;ip.topMargin=dp(5);content.addView(icon,ip);
    }
    private TextView title(String s){TextView t=label(s,19,TEXT);t.setTypeface(null,1);t.setPadding(dp(4),dp(14),dp(4),dp(5));return t;}
    private void addBar(LinearLayout box,int value,int max){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(Math.max(0,Math.min(max,value)));p.setProgressDrawable(getDrawable(android.R.drawable.progress_horizontal));box.addView(p,new LinearLayout.LayoutParams(-1,dp(10)));}
    private void showHome(){
        header("309 DAY DISCIPLINE","Build discipline. One day at a time. 🔥");
        int day=dayNumber(),done=countFor(key()),total=totalTasks(),pct=total==0?0:Math.round(done*100f/total);
        LinearLayout hero=card();hero.addView(label(day==0?"PROGRAM STARTS SOON":"DAY "+day+" / 309",12,MUTED));hero.addView(label(day==0?formatDate(startDate()):"Keep moving. "+Math.max(0,309-day)+" days remaining.",24,TEXT));addBar(hero,Math.max(0,day),309);hero.addView(label("Today  "+pct+"%   •   "+done+"/"+total+" tasks   •   🔥 "+currentStreak()+" day streak",13,MUTED));content.addView(hero);
        LinearLayout xpBox=card();xpBox.addView(label("LEVEL "+level(),11,MUTED));xpBox.addView(label(xp()+" XP",23,TEXT));addBar(xpBox,xp()%500,500);xpBox.addView(label((500-(xp()%500))+" XP to next level",12,MUTED));content.addView(xpBox);
        content.addView(title("TODAY'S MISSION"));
        for(int i=0;i<total;i++)addTaskRow(i,key());
        Button complete=button("✓  COMPLETE TODAY'S CHALLENGE");complete.setOnClickListener(v->completeDay());content.addView(complete);
        LinearLayout quick=card();quick.addView(label("MAYA + QUICK ACTIONS",11,MUTED));
        Button maya=button("🎙️  Talk to Maya");maya.setOnClickListener(v->voiceAssistant.start());quick.addView(maya);
        Button journal=button("📝  Write today's journal");journal.setOnClickListener(v->journalDialog());quick.addView(journal);
        Button remind=button("⏰  Manage reminders");remind.setOnClickListener(v->showReminders());quick.addView(remind);
        content.addView(quick);
        showMilestoneCard();
    }
    private void addTaskRow(int i,String d){
        LinearLayout row=card();row.setPadding(dp(10),dp(8),dp(10),dp(8));CheckBox cb=new CheckBox(this);cb.setText(taskName(i));cb.setTextColor(TEXT);cb.setTextSize(15);cb.setChecked(checked(i,d));cb.setOnCheckedChangeListener((v,c)->setChecked(i,d,c));row.addView(cb);content.addView(row);
    }
    private void showMilestoneCard(){
        int days=completedDays();String next=days<7?"7 days":days<30?"30 days":days<50?"50 days":days<100?"100 days":days<150?"150 days":days<200?"200 days":days<309?"309 days":"ALL 309 DAYS";
        LinearLayout m=card();m.addView(label("NEXT MILESTONE",11,MUTED));m.addView(label("🏆 "+next,20,TEXT));content.addView(m);
    }
    private void showJourney(){
        header("309-DAY JOURNEY","Your complete discipline timeline.");
        int d=dayNumber();LinearLayout top=card();top.addView(label("CURRENT",11,MUTED));top.addView(label(d==0?"Not started": "Day "+d+" of 309",25,TEXT));top.addView(label(completedDays()+" completed days  •  "+xp()+" XP",13,MUTED));content.addView(top);
        Calendar c=startDate();Calendar now=Calendar.getInstance();int index=0;
        while(index<309){
            LinearLayout week=card();week.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=0;j<7&&index<309;j++,index++){String k=key(c);TextView cell=label((index+1)+"",11,TEXT);cell.setGravity(Gravity.CENTER);cell.setBackground(shape(prefs.getBoolean("done_"+k,false)?ACCENT:(c.before(now)?0xFF343B4A:0xFF202633),10));week.addView(cell,new LinearLayout.LayoutParams(0,dp(34),1));c.add(Calendar.DAY_OF_YEAR,1);}
            content.addView(week);
        }
        content.addView(label("Green = completed • Grey = upcoming/missed. Tap Progress for detailed history.",12,MUTED));
    }
    private void showHabits(){
        header("HABITS & MISSIONS","Build your own daily system.");
        Button add=button("+  ADD CUSTOM HABIT");add.setOnClickListener(v->addHabitDialog());content.addView(add);
        content.addView(title("TODAY"));
        for(int i=0;i<totalTasks();i++)addTaskRow(i,key());
        content.addView(label("Long-press a custom habit below to rename or delete it.",12,MUTED));
        for(int i=DEFAULT_TASKS.length;i<totalTasks();i++){
            final int idx=i;Button manage=button("⚙  "+taskName(i)+"  •  Edit / Delete");manage.setOnLongClickListener(v->{editHabitDialog(idx);return true;});content.addView(manage);
        }
    }
    private void addHabitDialog(){
        EditText e=new EditText(this);e.setHint("e.g. Read 20 minutes");new AlertDialog.Builder(this).setTitle("Add custom habit").setView(e).setPositiveButton("ADD",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){int n=customCount();prefs.edit().putInt("custom_count",n+1).putString("habit_"+(DEFAULT_TASKS.length+n),s).apply();showHabits();}}).setNegativeButton("CANCEL",null).show();
    }
    private void editHabitDialog(int idx){
        EditText e=new EditText(this);e.setText(taskName(idx));new AlertDialog.Builder(this).setTitle("Edit habit").setView(e).setPositiveButton("SAVE",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())prefs.edit().putString("habit_"+idx,s).apply();showHabits();}).setNeutralButton("DELETE",(d,w)->deleteHabit(idx)).setNegativeButton("CANCEL",null).show();
    }
    private void deleteHabit(int idx){
        int last=DEFAULT_TASKS.length+customCount()-1;if(idx!=last){String name=prefs.getString("habit_"+last,"Habit");prefs.edit().putString("habit_"+idx,name).remove("habit_"+last).putInt("custom_count",customCount()-1).apply();}else prefs.edit().remove("habit_"+idx).putInt("custom_count",Math.max(0,customCount()-1)).apply();showHabits();
    }
    private void showStats(){
        header("PROGRESS & STATS","See the full picture, not just today's streak.");
        int completed=completedDays(),best=bestStreak(),today=countFor(key()),total=totalTasks();
        LinearLayout summary=card();summary.addView(label("LEVEL "+level(),11,MUTED));summary.addView(label(completed+" completed days",23,TEXT));summary.addView(label("🔥 "+currentStreak()+" current  •  🏆 "+best+" best streak",14,MUTED));summary.addView(label("⚡ "+xp()+" total XP  •  "+totalCompletedTasks()+" completed tasks",14,MUTED));content.addView(summary);
        LinearLayout week=card();week.addView(label("LAST 7 DAYS",11,MUTED));for(int i=6;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);int n=countFor(key(c));int pct=total==0?0:Math.round(n*100f/total);week.addView(label(new SimpleDateFormat("EEE",Locale.US).format(c.getTime())+"   "+n+"/"+total+"   "+pct+"%",13,TEXT));}content.addView(week);
        LinearLayout cal=card();cal.addView(label("LAST 30 DAYS",11,MUTED));for(int i=29;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);String k=key(c);TextView r=label(new SimpleDateFormat("dd MMM",Locale.US).format(c.getTime())+"   "+(prefs.getBoolean("done_"+k,false)?"✓ COMPLETE":countFor(k)>0?"• PARTIAL":"— MISSED"),13,TEXT);cal.addView(r);}content.addView(cal);
        Button journal=button("📝  Daily Journal & Reflection");journal.setOnClickListener(v->journalDialog());content.addView(journal);
        LinearLayout badges=card();badges.addView(label("ACHIEVEMENTS",11,MUTED));int[] ms={1,7,30,50,100,150,200,309};for(int m:ms)if(completed>=m)badges.addView(label("🏆 "+m+" day milestone unlocked",14,TEXT));else badges.addView(label("🔒 "+m+" day milestone",14,MUTED));content.addView(badges);
    }
    private void journalDialog(){
        String today=prefs.getString("journal_"+key(),"");EditText e=new EditText(this);e.setHint("How was today? What did you learn?");e.setMinLines(5);e.setText(today);
        new AlertDialog.Builder(this).setTitle("📝 Today's Journal").setView(e).setPositiveButton("SAVE",(d,w)->{prefs.edit().putString("journal_"+key(),e.getText().toString().trim()).apply();toast("Journal saved 📝");}).setNegativeButton("CANCEL",null).show();
    }
    private String formatDate(Calendar c){return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(c.getTime());}
    private void completeDay(){
        String k=key();if(prefs.getBoolean("done_"+k,false)){toast("Today is already completed. 🔥");return;}
        if(countFor(k)!=totalTasks()){toast("Finish all "+totalTasks()+" tasks first.");return;}
        int streak=currentStreak()+1;int best=Math.max(bestStreak(),streak);prefs.edit().putBoolean("done_"+k,true).putInt("streak",streak).putInt("best",best).apply();
        toast("Day completed! +100 XP 🔥");showHome();
    }
    private void showReminders(){
        header("REMINDERS","Daily alarms that keep your plan on track.");
        Button add=button("+  ADD DAILY REMINDER");add.setOnClickListener(v->alarmDialog());content.addView(add);
        boolean found=false;for(String k:prefs.getAll().keySet())if(k.startsWith("alarm")){String v=prefs.getString(k,"");String[] p=v.split("\\|",-1);if(p.length==3){found=true;LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.addView(label("🔔 "+(p[0].isEmpty()?"Discipline reminder":p[0]),14,TEXT),new LinearLayout.LayoutParams(0,-2,1));row.addView(label(String.format(Locale.US,"%02d:%02d",Integer.parseInt(p[1]),Integer.parseInt(p[2])),14,ACCENT));content.addView(row);}}
        if(!found)content.addView(label("No reminders yet.",14,MUTED));content.addView(label("Reminders survive app restarts. Android may require exact-alarm and notification access.",12,MUTED));
    }
    private void alarmDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText name=new EditText(this);name.setHint("Reminder name");l.addView(name);TimePicker p=new TimePicker(this);p.setIs24HourView(true);l.addView(p);
        new AlertDialog.Builder(this).setTitle("Add daily reminder").setView(l).setPositiveButton("SAVE",(d,w)->{schedule(name.getText().toString(),p.getHour(),p.getMinute());showReminders();toast("Reminder saved");}).setNegativeButton("CANCEL",null).show();
    }
    private void schedule(String name,int h,int m){
        int id=(name+h+m).hashCode();Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
        scheduleStatic(this,name,h,m,id,c.getTimeInMillis());prefs.edit().putString("alarm"+id,name+"|"+h+"|"+m).apply();
    }
    public static void scheduleAll(Context context){
        SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);for(String k:p.getAll().keySet())if(k.startsWith("alarm")){String v=p.getString(k,null);if(v==null)continue;String[] a=v.split("\\|",-1);if(a.length==3)try{scheduleStatic(context,a[0],Integer.parseInt(a[1]),Integer.parseInt(a[2]),k.substring(5).hashCode(),-1);}catch(Exception ignored){}}
    }
    private static void scheduleStatic(Context c,String name,int h,int m,int id,long requested){
        Calendar x=Calendar.getInstance();if(requested>0)x.setTimeInMillis(requested);else{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,m);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())x.add(Calendar.DAY_OF_YEAR,1);}
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return;
        Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("title",name==null||name.trim().isEmpty()?"Discipline reminder":name);in.putExtra("msg","It's time. Start your next task. 🔥");
        PendingIntent pi=PendingIntent.getBroadcast(c,id,in,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);
    }
    private void chatDialog(){
        EditText input=new EditText(this);input.setHint("Ask Maya in Sinhala, Singlish or English...");input.setMinLines(2);
        new AlertDialog.Builder(this).setTitle("💬 Maya").setMessage("Ask anything about your routine, discipline or today.").setView(input).setPositiveButton("SEND",(d,w)->askMaya(input.getText().toString())).setNegativeButton("CLOSE",null).show();
    }
    private void askMaya(String q){
        String context="Today: "+countFor(key())+"/"+totalTasks()+" tasks. Day "+dayNumber()+"/309. Current streak "+currentStreak()+". Best streak "+bestStreak()+". XP "+xp()+". Journal: "+prefs.getString("journal_"+key(),"none");
        MayaAI.ask(this,q,context,"auto",reply->runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply).setPositiveButton("OK",null).show()));
    }
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Exception e){new AlertDialog.Builder(this).setTitle("Settings").setMessage("Open Settings from the app menu.").setPositiveButton("OK",null).show();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();sound(ToneGenerator.TONE_PROP_BEEP);}
    @Override protected void onResume(){super.onResume();applyTheme();}
    @Override protected void onDestroy(){if(voiceAssistant!=null)voiceAssistant.destroy();if(tone!=null)tone.release();super.onDestroy();}
}
