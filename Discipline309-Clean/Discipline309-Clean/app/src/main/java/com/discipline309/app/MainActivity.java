package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Color;
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
    private static final String[] DEFAULT_TASKS={"Wake up on time","Study / learning","Workout or active recovery","Eat planned meals","No-phone block","Night review + prepare tomorrow"};
    private int BG=0xFF061126,SURFACE=0xFF0B1B3A,TEXT=0xFFF5F8FF,MUTED=0xFF9CB2D9,ACCENT=0xFF2F7BFF;
    private SharedPreferences prefs;
    private LinearLayout content;
    private VoiceAssistant voiceAssistant;
    private ToneGenerator tone;
    private final Handler syncHandler=new Handler(Looper.getMainLooper());
    private final Runnable syncRunnable=new Runnable(){@Override public void run(){try{if(isFinishing()||isDestroyed())return;if(SupabaseAccountManager.loggedIn(MainActivity.this)&&"sub".equals(SupabaseAccountManager.role(MainActivity.this))){SupabaseAccountManager.syncLocalProgress(MainActivity.this,null);syncHandler.postDelayed(this,15000);}}catch(Exception e){android.util.Log.e("309DayDiscipline","Periodic sync error",e);try{if(!isFinishing()&&!isDestroyed())syncHandler.postDelayed(this,30000);}catch(Exception ignored){}}}};
    private static final int PICK_MAYA_IMAGE=901,CAPTURE_MAYA_IMAGE=902,PICK_MAYA_DOCUMENT=903;

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void sound(int t){try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,65);tone.startTone(t,80);}catch(Exception ignored){}}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}
    private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),0xFF173D78);return g;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(shape(SURFACE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(48));b.setBackground(shape(0xFF102957,16));b.setPadding(dp(10),0,dp(10),0);b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP)sound(ToneGenerator.TONE_PROP_ACK);return false;});return b;}
    private String key(){return key(Calendar.getInstance());}
    private String key(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());}
    private void ensureProgramStart(){if(!prefs.contains("program_start")){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);prefs.edit().putLong("program_start",c.getTimeInMillis()).apply();}}
    private Calendar startDate(){Calendar c=Calendar.getInstance();c.setTimeInMillis(prefs.getLong("program_start",System.currentTimeMillis()));return c;}
    private Calendar target(){Calendar c=startDate();c.add(Calendar.DAY_OF_YEAR,308);return c;}
    private int daysFromStart(){return(int)((System.currentTimeMillis()-startDate().getTimeInMillis())/86400000L)+1;}
    private int dayNumber(){return Math.max(0,Math.min(309,daysFromStart()));}
    private String taskName(int i){return i<DEFAULT_TASKS.length?DEFAULT_TASKS[i]:prefs.getString("habit_"+i,"Habit");}
    private int customCount(){try{return Math.max(0,Math.min(100,prefs.getInt("custom_count",0)));}catch(Exception e){return 0;}}
    private int totalTasks(){return DEFAULT_TASKS.length+customCount();}
    private boolean checked(int i,String d){return prefs.getBoolean("task_"+i+"_"+d,false);}
    private void setChecked(int i,String d,boolean v){prefs.edit().putBoolean("task_"+i+"_"+d,v).apply();}
    private boolean allowed(String permission){return !SupabaseAccountManager.loggedIn(this)||SupabaseAccountManager.can(this,permission);}
    private int countFor(String d){int n=0;for(int i=0;i<totalTasks();i++)if(checked(i,d))n++;return n;}
    private int completedDays(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();int guard=0;while(!c.after(now)&&!c.after(target())&&guard++<309){if(prefs.getBoolean("done_"+key(c),false))n++;c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int xp(){return completedDays()*100+totalCompletedTasks()*20+prefs.getInt("xp_bonus",0);}
    private void awardXp(int amount,String reason){if(amount<=0)return;prefs.edit().putInt("xp_bonus",prefs.getInt("xp_bonus",0)+amount).apply();toast(reason+"  +"+amount+" XP");}
    private int totalCompletedTasks(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();int guard=0;while(!c.after(now)&&!c.after(target())&&guard++<309){n+=countFor(key(c));c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int level(){return xp()/500+1;}
    private int currentStreak(){int n=0;Calendar c=Calendar.getInstance();Calendar start=startDate();Calendar end=target();if(c.after(end))c.setTimeInMillis(end.getTimeInMillis());if(!prefs.getBoolean("done_"+key(c),false))c.add(Calendar.DAY_OF_YEAR,-1);while(!c.before(start)&&n<309&&prefs.getBoolean("done_"+key(c),false)){n++;c.add(Calendar.DAY_OF_YEAR,-1);}return n;}
    private int bestStreak(){int best=0,run=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){if(prefs.getBoolean("done_"+key(c),false))run++;else run=0;best=Math.max(best,run);c.add(Calendar.DAY_OF_YEAR,1);}return Math.max(best,prefs.getInt("best",0));}

    @Override protected void onCreate(Bundle b){super.onCreate(b);try{prefs=getSharedPreferences(PREFS,MODE_PRIVATE);ensureProgramStart();applyTheme();buildShell();showHome();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}catch(Throwable e){android.util.Log.e("309DayDiscipline","Startup error",e);showStartupFallback();}}
    private void showStartupFallback(){try{LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(BG);TextView title=label("309 DAY DISCIPLINE",26,TEXT);title.setGravity(Gravity.CENTER);root.addView(title);TextView msg=label("Startup problem එකක් හඳුනාගත්තා. App එක crash නොවී safe mode එකෙන් open වුණා.",15,MUTED);msg.setGravity(Gravity.CENTER);root.addView(msg,new LinearLayout.LayoutParams(-1,-2));Button retry=button("↻  TRY AGAIN");retry.setOnClickListener(v->{try{buildShell();showHome();}catch(Throwable e){android.util.Log.e("309DayDiscipline","Retry startup error",e);}});root.addView(retry,new LinearLayout.LayoutParams(-1,dp(52)));setContentView(root);}catch(Throwable ignored){}}
    private void applyTheme(){String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");BG=0xFF061126;SURFACE=0xFF0B1B3A;TEXT=0xFFF5F8FF;MUTED=0xFF9CB2D9;ACCENT=0xFF2F7BFF;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
    private void buildShell(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(14),dp(18),dp(12));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(2),dp(4),dp(2),dp(4));nav.setBackground(shape(0xFF081A36,18));String[] names={"⌂\nHome","◷\nPlan","✓\nHabits","◫\nStats","⚙\nSettings"};for(int i=0;i<5;i++){final int n=i;Button b=button(names[i]);b.setTextSize(11);b.setPadding(0,0,0,0);b.setOnClickListener(v->{if(n==0)showHome();else if(n==1)showShortPlan();else if(n==2)showHabits();else if(n==3)showStats();else openSettings();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));}root.addView(nav);setContentView(root);}
    private void header(String title,String sub){content.removeAllViews();TextView t=label(title,26,TEXT);t.setTypeface(null,1);content.addView(t);content.addView(label(sub,13,MUTED));ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_discipline);icon.setContentDescription("Discipline");icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(64),dp(64));ip.gravity=Gravity.CENTER_HORIZONTAL;ip.topMargin=dp(5);content.addView(icon,ip);}
    private TextView title(String s){TextView t=label(s,19,TEXT);t.setTypeface(null,1);t.setPadding(dp(4),dp(14),dp(4),dp(5));return t;}
    private void addBar(LinearLayout box,int value,int max){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(Math.max(0,Math.min(max,value)));p.setProgressDrawable(getDrawable(android.R.drawable.progress_horizontal));box.addView(p,new LinearLayout.LayoutParams(-1,dp(10)));}
    private void openMayaDocument(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_DOCUMENT);}catch(Exception e){toast("Document picker එක open කරන්න බැරි වුණා.");}}
    private void analyzeMayaDocument(Uri uri){if(uri==null)return;String type=getContentResolver().getType(uri);if(type!=null&&type.equals("application/pdf")){new Thread(()->{try{android.os.ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new Exception("fd");android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd);if(renderer.getPageCount()==0)throw new Exception("empty");android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0);Bitmap bitmap=Bitmap.createBitmap(page.getWidth()*2,page.getHeight()*2,Bitmap.Config.ARGB_8888);page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);page.close();renderer.close();fd.close();runOnUiThread(()->analyzeMayaBitmapWithQuestion(bitmap,"Read and summarize this document page. Extract visible important text and explain it."));}catch(Exception e){runOnUiThread(()->toast("PDF එක read කරන්න බැරි වුණා."));}}).start();}else{String text=MayaVision.documentText(this,uri);if(text.isEmpty()){toast("මේ document type එක තවම support වෙන්නේ නැහැ. Text file හෝ PDF එකක් තෝරන්න.");return;}String clipped=text.length()>12000?text.substring(0,12000):text;MayaAI.ask(this,"Analyze this document and summarize the important points:\n"+clipped,"Document provided by user","document analyst",reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}}
    private void analyzeMayaBitmapWithQuestion(Bitmap bitmap,String question){try{java.io.File file=new java.io.File(getCacheDir(),"maya_doc_page.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();MayaVision.analyze(this,Uri.fromFile(file),question,reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}catch(Exception e){toast("Document page process කරන්න බැරි වුණා.");}}
    private void openMayaGallery(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_IMAGE);}catch(Exception e){toast("Gallery picker එක open කරන්න බැරි වුණා.");}}
    private void openMayaCamera(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},33);return;}try{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAPTURE_MAYA_IMAGE);}catch(Exception e){toast("Camera open කරන්න බැරි වුණා.");}}
    private void analyzeMayaImage(Uri uri){if(uri==null)return;final EditText q=new EditText(this);q.setHint("Ask Maya about this image (optional)");new AlertDialog.Builder(this).setTitle("🖼️ Ask Maya about image").setView(q).setPositiveButton("ANALYZE",(d,w)->{String question=q.getText().toString().trim();toast("Maya image එක බලනවා… 🧠");MayaVision.analyze(this,uri,question,reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}).setNegativeButton("CANCEL",null).show();}
    private void analyzeMayaBitmap(Bitmap bitmap){try{java.io.File file=new java.io.File(getCacheDir(),"maya_camera.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();analyzeMayaImage(Uri.fromFile(file));}catch(Exception e){toast("Camera image process කරන්න බැරි වුණා.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;try{if(requestCode==PICK_MAYA_IMAGE){Uri uri=data.getData();if(uri!=null)analyzeMayaImage(uri);}else if(requestCode==PICK_MAYA_DOCUMENT){Uri uri=data.getData();if(uri!=null)analyzeMayaDocument(uri);}else if(requestCode==CAPTURE_MAYA_IMAGE){Bitmap b=data.getParcelableExtra("data");if(b!=null)analyzeMayaBitmap(b);}}catch(Exception e){toast("Selected file/image process කරන්න බැරි වුණා.");}}

    private void showHome(){header("309 DAY DISCIPLINE","Build discipline. One day at a time. 🔥");int day=dayNumber(),done=countFor(key()),total=totalTasks(),pct=total==0?0:Math.round(done*100f/total);LinearLayout hero=card();hero.addView(label(day==0?"PROGRAM STARTS SOON":"DAY "+day+" / 309",12,MUTED));hero.addView(label(day==0?formatDate(startDate()):"Keep moving. "+Math.max(0,309-day)+" days remaining.",24,TEXT));addBar(hero,Math.max(0,day),309);hero.addView(label("Today  "+pct+"%   •   "+done+"/"+total+" tasks   •   🔥 "+currentStreak()+" day streak",13,MUTED));content.addView(hero);LinearLayout xpBox=card();xpBox.addView(label("LEVEL "+level(),11,MUTED));xpBox.addView(label(xp()+" XP",23,TEXT));addBar(xpBox,xp()%500,500);xpBox.addView(label((500-(xp()%500))+" XP to next level",12,MUTED));content.addView(xpBox);
        int planTotal=planCount(), planDone=0;
        for(int i=0;i<planTotal;i++) if(planTaskDone(i)) planDone++;
        String focus=prefs.getString("plan_"+key()+"_goal","");
        LinearLayout planSummary=card();
        planSummary.addView(label("📋 SHORT PLAN",11,MUTED));
        planSummary.addView(label(planTotal==0?"No short plan yet":planDone+"/"+planTotal+" planned tasks complete",18,TEXT));
        if(!focus.isEmpty()) planSummary.addView(label("🎯 "+focus,13,MUTED));
        if(planTotal>0) addBar(planSummary,planDone,planTotal);
        Button openPlan=button(planTotal==0?"＋  Create Today's Plan":"📋  Open Today's Plan");
        openPlan.setOnClickListener(v->showShortPlan());
        planSummary.addView(openPlan);
        content.addView(planSummary);
        LinearLayout mayaBrief=card();
        mayaBrief.addView(label("🧠 MAYA'S DAILY BRIEF",11,MUTED));
        String brief;
        if(planTotal>0 && planDone<planTotal) brief="You have "+(planTotal-planDone)+" short-plan task"+((planTotal-planDone)==1?"":"s")+" left. Pick the next one and start.";
        else if(pct<100) brief="Finish today's habits first. Small actions count. 🔥";
        else if(!prefs.getBoolean("mission_"+key()+"_done",false)) brief="Today's habits are done. Now finish your mission. 🎯";
        else brief="Everything planned for today is complete. Nice work. 🔥";
        mayaBrief.addView(label(brief,15,TEXT));
        Button askBrief=button("💬  Ask Maya");
        askBrief.setOnClickListener(v->chatDialog());
        mayaBrief.addView(askBrief);
        content.addView(mayaBrief);content.addView(title("TODAY'S MISSION"));LinearLayout mission=card();mission.addView(label("🎯 DAILY CHALLENGE",11,MUTED));String missionKey="mission_"+key();String[] missions={"Complete every planned task today","Finish one focused study session","Do your routine before entertainment","Write a 3-line evening reflection","Complete today without skipping a habit"};int missionIndex=Math.abs(key().hashCode())%missions.length;String missionText=prefs.getString(missionKey,missions[missionIndex]);mission.addView(label(missionText,18,TEXT));mission.addView(label("Reward: +50 XP  •  Resets tomorrow",12,MUTED));CheckBox missionDone=new CheckBox(this);missionDone.setText("Mission complete");missionDone.setTextColor(TEXT);missionDone.setChecked(prefs.getBoolean(missionKey+"_done",false));missionDone.setEnabled(allowed("can_edit_mission"));missionDone.setOnCheckedChangeListener((v,checked)->{if(!allowed("can_edit_mission")){v.setChecked(!checked);toast("Primary account has disabled mission editing.");return;}prefs.edit().putBoolean(missionKey+"_done",checked).apply();if(checked&&!prefs.getBoolean(missionKey+"_rewarded",false)){prefs.edit().putBoolean(missionKey+"_rewarded",true).apply();awardXp(50,"Mission complete! 🎯");}});mission.addView(missionDone);content.addView(mission);content.addView(title("TODAY'S HABITS"));for(int i=0;i<total;i++)addTaskRow(i,key());Button complete=button("✓  COMPLETE TODAY'S CHALLENGE");complete.setOnClickListener(v->completeDay());content.addView(complete);LinearLayout quick=card();quick.addView(label("MAYA + QUICK ACTIONS",11,MUTED));Button plan=button("📋  Short Planning Mode");plan.setBackground(shape(0xFF1769FF,18));plan.setOnClickListener(v->showShortPlan());quick.addView(plan);Button maya=button("🎙️  Talk to Maya");maya.setOnClickListener(v->{if(voiceAssistant==null)voiceAssistant=new VoiceAssistant(this);voiceAssistant.start();});quick.addView(maya);Button image=button("🖼️  Ask Maya about a photo");image.setOnClickListener(v->openMayaGallery());quick.addView(image);Button camera=button("📷  Ask Maya with camera");camera.setOnClickListener(v->openMayaCamera());quick.addView(camera);Button document=button("📄  Ask Maya about a document");document.setOnClickListener(v->openMayaDocument());quick.addView(document);Button journal=button("📝  Write today's journal");journal.setOnClickListener(v->journalDialog());quick.addView(journal);Button remind=button("⏰  Manage reminders");remind.setOnClickListener(v->showReminders());quick.addView(remind);content.addView(quick);showMilestoneCard();}
    private void addTaskRow(int i,String d){LinearLayout row=card();row.setPadding(dp(10),dp(8),dp(10),dp(8));CheckBox cb=new CheckBox(this);cb.setText(taskName(i));cb.setTextColor(TEXT);cb.setTextSize(15);cb.setChecked(checked(i,d));cb.setEnabled(allowed("can_edit_habits"));cb.setOnCheckedChangeListener((v,c)->{if(allowed("can_edit_habits")){setChecked(i,d,c);if(SupabaseAccountManager.loggedIn(this)&&"sub".equals(SupabaseAccountManager.role(this))){syncHandler.removeCallbacks(syncRunnable);syncHandler.postDelayed(syncRunnable,1500);}}else v.setChecked(!c);});row.addView(cb);content.addView(row);}
    public void editTodayMissionFromMaya(String requested){final EditText e=new EditText(this);e.setHint("e.g. Study for 30 minutes");e.setSingleLine(false);if(!allowed("can_edit_mission")){toast("Primary account has disabled mission editing.");return;}String current=prefs.getString("mission_"+key(),"");if(requested!=null&&!requested.trim().isEmpty())e.setText(requested.trim());else if(!current.isEmpty())e.setText(current);new AlertDialog.Builder(this).setTitle("🎯 Edit Today's Mission").setMessage("Maya can change today's mission. The new mission will be saved for today.").setView(e).setPositiveButton("SAVE",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){prefs.edit().putString("mission_"+key(),s).apply();toast("Today's mission updated by Maya 🎯");showHome();}}).setNegativeButton("CANCEL",null).show();}
    public void resetTodayMissionFromMaya(){if(!allowed("can_edit_mission")){toast("Primary account has disabled mission editing.");return;}String k="mission_"+key();prefs.edit().remove(k).remove(k+"_done").apply();toast("Today's mission reset 🎯");showHome();}
    private void showMilestoneCard(){int days=completedDays();String next=days<7?"7 days":days<30?"30 days":days<50?"50 days":days<100?"100 days":days<150?"150 days":days<200?"200 days":days<309?"309 days":"ALL 309 DAYS";LinearLayout m=card();m.addView(label("NEXT MILESTONE",11,MUTED));m.addView(label("🏆 "+next,20,TEXT));content.addView(m);}
    private String planDate(){return key();}

    private int planCount(){try{return Math.max(0,Math.min(50,prefs.getInt("plan_count_"+planDate(),0)));}catch(Exception e){return 0;}}

    private boolean validPlanIndex(int i){return i>=0&&i<planCount();}
    private String planTaskName(int i){try{return validPlanIndex(i)?prefs.getString("plan_"+planDate()+"_"+i+"_name","Task"):"Task";}catch(Exception e){return "Task";}}

    private String planTaskTime(int i){try{return validPlanIndex(i)?prefs.getString("plan_"+planDate()+"_"+i+"_time","Anytime"):"Anytime";}catch(Exception e){return "Anytime";}}

    private String planTaskPriority(int i){try{return validPlanIndex(i)?prefs.getString("plan_"+planDate()+"_"+i+"_priority","Medium"):"Medium";}catch(Exception e){return "Medium";}}

    private boolean planTaskDone(int i){try{return validPlanIndex(i)&&prefs.getBoolean("plan_"+planDate()+"_"+i+"_done",false);}catch(Exception e){return false;}}

    private void showShortPlan(){
        content.removeAllViews();
        TextView top=label("Short Planning",26,TEXT);top.setTypeface(null,1);content.addView(top);
        content.addView(label("Quick plan. Big results. Stay focused.",13,MUTED));

        int count=planCount(),done=0;        for(int i=0;i<count;i++)if(planTaskDone(i))done++;
        int pct=count==0?0:Math.round(done*100f/count);

        LinearLayout progress=card();
        progress.setBackground(shape(0xFF0D234A,20));
        progress.addView(label("TODAY'S PLAN",11,MUTED));
        TextView p=label(done+"/"+count+" tasks completed",22,TEXT);p.setTypeface(null,1);progress.addView(p);
        addBar(progress,done,Math.max(1,count));
        progress.addView(label(pct+"% complete  •  "+(count-done)+" remaining",12,MUTED));
        content.addView(progress);

        Button add=button("＋  CREATE / ADD TASK");
        add.setTextColor(Color.WHITE);
        add.setBackground(shape(0xFF1769FF,18));
        add.setOnClickListener(v->addPlanTaskDialog());
        content.addView(add);

        if(count==0){
            LinearLayout empty=card();
            TextView e=label("📅\n\nNo plan for today yet.",19,TEXT);e.setGravity(Gravity.CENTER);
            empty.addView(e);
            empty.addView(label("Add a few important tasks and keep the plan short.",13,MUTED));
            content.addView(empty);
        }else{
            content.addView(title("TODAY'S TASKS"));
            for(int i=0;i<count;i++)addPlanTaskRow(i);
            Button clear=button("✓  MARK ALL AS DONE");
            clear.setOnClickListener(v->{for(int i=0;i<count;i++)prefs.edit().putBoolean("plan_"+planDate()+"_"+i+"_done",true).apply();showShortPlan();});
            content.addView(clear);
        }

        LinearLayout focus=card();
        focus.addView(label("FOCUS GOAL",11,MUTED));
        String goal=prefs.getString("plan_"+planDate()+"_goal","");
        focus.addView(label(goal.isEmpty()?"Set one main goal for today.":goal,17,TEXT));
        Button goalBtn=button("🎯  Set / Edit Goal");
        goalBtn.setOnClickListener(v->editPlanGoalDialog());
        focus.addView(goalBtn);
        content.addView(focus);

        Button journey=button("▦  View 309-Day Journey");
        journey.setOnClickListener(v->showJourney());
        content.addView(journey);
    }

    private void addPlanTaskRow(int i){
        LinearLayout row=card();row.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox cb=new CheckBox(this);cb.setChecked(planTaskDone(i));cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        cb.setOnCheckedChangeListener((v,x)->{prefs.edit().putBoolean("plan_"+planDate()+"_"+i+"_done",x).apply();});
        line.addView(cb,new LinearLayout.LayoutParams(dp(42),dp(48)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        TextView name=label(planTaskName(i),16,TEXT);name.setTypeface(null,1);info.addView(name);
        info.addView(label(planTaskTime(i)+"   •   "+planTaskPriority(i),12,MUTED));
        line.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        Button edit=button("⋮");edit.setMinWidth(dp(44));edit.setOnClickListener(v->editPlanTaskDialog(i));
        line.addView(edit);
        row.addView(line);
        content.addView(row);
    }

    private void addPlanTaskDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(4),0,dp(4),0);
        EditText name=new EditText(this);name.setHint("Task name");name.setSingleLine(true);box.addView(name);
        EditText time=new EditText(this);time.setHint("Time (e.g. 5:00 PM - 6:00 PM)");time.setSingleLine(true);box.addView(time);
        Spinner priority=new Spinner(this);String[] ps={"High","Medium","Low"};priority.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ps));box.addView(priority);
        new AlertDialog.Builder(this).setTitle("Create Short Plan Task").setView(box).setPositiveButton("ADD",(d,w)->{
            String n=name.getText().toString().trim();if(n.isEmpty())return;
            int i=planCount();if(i>=50){toast("Plan task limit reached.");return;}SharedPreferences.Editor e=prefs.edit();
            e.putInt("plan_count_"+planDate(),i+1).putString("plan_"+planDate()+"_"+i+"_name",n)
             .putString("plan_"+planDate()+"_"+i+"_time",time.getText().toString().trim().isEmpty()?"Anytime":time.getText().toString().trim())
             .putString("plan_"+planDate()+"_"+i+"_priority",ps[priority.getSelectedItemPosition()]).apply();
            showShortPlan();
        }).setNegativeButton("CANCEL",null).show();
    }

    private void editPlanTaskDialog(int i){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText name=new EditText(this);name.setText(planTaskName(i));box.addView(name);
        EditText time=new EditText(this);time.setText(planTaskTime(i));box.addView(time);
        Spinner priority=new Spinner(this);String[] ps={"High","Medium","Low"};priority.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ps));
        for(int j=0;j<ps.length;j++)if(ps[j].equals(planTaskPriority(i)))priority.setSelection(j);
        box.addView(priority);
        new AlertDialog.Builder(this).setTitle("Edit Task").setView(box).setPositiveButton("SAVE",(d,w)->{
            prefs.edit().putString("plan_"+planDate()+"_"+i+"_name",name.getText().toString().trim())
             .putString("plan_"+planDate()+"_"+i+"_time",time.getText().toString().trim())
             .putString("plan_"+planDate()+"_"+i+"_priority",ps[priority.getSelectedItemPosition()]).apply();showShortPlan();
        }).setNeutralButton("DELETE",(d,w)->deletePlanTask(i)).setNegativeButton("CANCEL",null).show();
    }

    private void deletePlanTask(int index){
        int n=planCount();SharedPreferences.Editor e=prefs.edit();
        for(int i=index;i<n-1;i++){
            e.putString("plan_"+planDate()+"_"+i+"_name",prefs.getString("plan_"+planDate()+"_"+(i+1)+"_name","Task"));
            e.putString("plan_"+planDate()+"_"+i+"_time",prefs.getString("plan_"+planDate()+"_"+(i+1)+"_time","Anytime"));
            e.putString("plan_"+planDate()+"_"+i+"_priority",prefs.getString("plan_"+planDate()+"_"+(i+1)+"_priority","Medium"));
            e.putBoolean("plan_"+planDate()+"_"+i+"_done",prefs.getBoolean("plan_"+planDate()+"_"+(i+1)+"_done",false));
        }
        e.remove("plan_"+planDate()+"_"+(n-1)+"_name").remove("plan_"+planDate()+"_"+(n-1)+"_time").remove("plan_"+planDate()+"_"+(n-1)+"_priority").remove("plan_"+planDate()+"_"+(n-1)+"_done");
        e.putInt("plan_count_"+planDate(),Math.max(0,n-1)).apply();showShortPlan();
    }

    private void editPlanGoalDialog(){
        EditText e=new EditText(this);e.setHint("e.g. Finish my study session");e.setText(prefs.getString("plan_"+planDate()+"_goal",""));
        new AlertDialog.Builder(this).setTitle("Today's Focus Goal").setView(e).setPositiveButton("SAVE",(d,w)->{prefs.edit().putString("plan_"+planDate()+"_goal",e.getText().toString().trim()).apply();showShortPlan();}).setNegativeButton("CANCEL",null).show();
    }

    private void showJourney(){header("309-DAY JOURNEY","Your complete discipline timeline.");int d=dayNumber();LinearLayout top=card();top.addView(label("CURRENT",11,MUTED));top.addView(label(d==0?"Not started":"Day "+d+" of 309",25,TEXT));top.addView(label(completedDays()+" completed days  •  "+xp()+" XP",13,MUTED));content.addView(top);Calendar c=startDate();Calendar now=Calendar.getInstance();int index=0;while(index<309){LinearLayout week=card();week.setOrientation(LinearLayout.HORIZONTAL);for(int j=0;j<7&&index<309;j++,index++){String k=key(c);int done=countFor(k);int bg=prefs.getBoolean("done_"+k,false)?ACCENT:(done>0?0xFF8A7A32:(c.before(now)?0xFF343B4A:0xFF202633));TextView cell=label((index+1)+"",11,TEXT);cell.setGravity(Gravity.CENTER);cell.setBackground(shape(bg,10));week.addView(cell,new LinearLayout.LayoutParams(0,dp(34),1));c.add(Calendar.DAY_OF_YEAR,1);}content.addView(week);}content.addView(label("Green = complete • Gold = partial • Grey = upcoming/missed.",12,MUTED));}
    private void showHabits(){header("HABITS & MISSIONS","Build your own daily system.");Button add=button("+  ADD CUSTOM HABIT");add.setEnabled(allowed("can_edit_habits"));add.setOnClickListener(v->addHabitDialog());content.addView(add);content.addView(title("TODAY"));for(int i=0;i<totalTasks();i++)addTaskRow(i,key());content.addView(label("Long-press a custom habit below to rename or delete it.",12,MUTED));for(int i=DEFAULT_TASKS.length;i<totalTasks();i++){final int idx=i;Button manage=button("⚙  "+taskName(i)+"  •  Edit / Delete");manage.setEnabled(allowed("can_edit_habits"));manage.setOnLongClickListener(v->{editHabitDialog(idx);return true;});content.addView(manage);}}
    private void addHabitDialog(){if(!allowed("can_edit_habits")){toast("Primary account has disabled habit editing.");return;}if(customCount()>=100){toast("Custom habit limit reached.");return;}EditText e=new EditText(this);e.setHint("e.g. Read 20 minutes");new AlertDialog.Builder(this).setTitle("Add custom habit").setView(e).setPositiveButton("ADD",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()){int n=customCount();if(n>=100){toast("Custom habit limit reached.");return;}prefs.edit().putInt("custom_count",n+1).putString("habit_"+(DEFAULT_TASKS.length+n),s).apply();showHabits();}}).setNegativeButton("CANCEL",null).show();}
    private void editHabitDialog(int idx){if(!allowed("can_edit_habits")){toast("Primary account has disabled habit editing.");return;}EditText e=new EditText(this);e.setText(taskName(idx));new AlertDialog.Builder(this).setTitle("Edit habit").setView(e).setPositiveButton("SAVE",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())prefs.edit().putString("habit_"+idx,s).apply();showHabits();}).setNeutralButton("DELETE",(d,w)->deleteHabit(idx)).setNegativeButton("CANCEL",null).show();}
    private void deleteHabit(int idx){if(!allowed("can_edit_habits")){toast("Primary account has disabled habit editing.");return;}int count=customCount();int first=DEFAULT_TASKS.length;int last=first+count-1;if(count<=0||idx<first||idx>last){showHabits();return;}if(idx!=last){String name=prefs.getString("habit_"+last,"Habit");prefs.edit().putString("habit_"+idx,name).remove("habit_"+last).putInt("custom_count",Math.max(0,count-1)).apply();}else prefs.edit().remove("habit_"+idx).putInt("custom_count",Math.max(0,count-1)).apply();showHabits();}
    private void showAchievements(){header("ACHIEVEMENTS","Milestones earned through consistency.");int days=completedDays(),streak=bestStreak(),missions=0;for(String k:prefs.getAll().keySet())if(k.startsWith("mission_")&&k.endsWith("_rewarded")&&prefs.getBoolean(k,false))missions++;LinearLayout summary=card();summary.addView(label("🏆 "+days+" completed days",22,TEXT));summary.addView(label("🔥 Best streak: "+streak+" days  •  🎯 Missions: "+missions,13,MUTED));content.addView(summary);int[] milestones={1,3,7,14,30,50,100,150,200,309};String[] names={"First Step","3-Day Spark","One Week","Two Weeks","30-Day Discipline","50-Day Warrior","100-Day Mastery","150-Day Elite","200-Day Relentless","309-Day Legend"};for(int i=0;i<milestones.length;i++){int m=milestones[i];boolean u=days>=m;LinearLayout a=card();a.addView(label(u?"🏆 "+names[i]:"🔒 "+names[i],17,u?TEXT:MUTED));a.addView(label(m+" completed days",12,MUTED));content.addView(a);}int[] streaks={3,7,14,30};String[] sn={"3-Day Streak","7-Day Streak","14-Day Streak","30-Day Streak"};for(int i=0;i<streaks.length;i++){boolean u=streak>=streaks[i];LinearLayout a=card();a.addView(label(u?"🔥 "+sn[i]:"🔒 "+sn[i],17,u?TEXT:MUTED));a.addView(label(streaks[i]+" consecutive completed days",12,MUTED));content.addView(a);}LinearLayout m=card();m.addView(label("🎯 MISSION ACHIEVEMENTS",11,MUTED));int[] mm={1,7,30};for(int x:mm)m.addView(label(missions>=x?"🏆 "+x+" daily missions completed":"🔒 "+x+" daily missions",14,missions>=x?TEXT:MUTED));content.addView(m);}
    private void showStats(){header("PROGRESS & STATS","See the full picture, not just today's streak.");int completed=completedDays(),best=bestStreak(),today=countFor(key()),total=totalTasks();LinearLayout summary=card();summary.addView(label("LEVEL "+level(),11,MUTED));summary.addView(label(completed+" completed days",23,TEXT));summary.addView(label("🔥 "+currentStreak()+" current  •  🏆 "+best+" best streak",14,MUTED));summary.addView(label("⚡ "+xp()+" total XP  •  "+totalCompletedTasks()+" completed tasks",14,MUTED));content.addView(summary);LinearLayout week=card();week.addView(label("LAST 7 DAYS",11,MUTED));for(int i=6;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);int n=countFor(key(c));int pct=total==0?0:Math.round(n*100f/total);week.addView(label(new SimpleDateFormat("EEE",Locale.US).format(c.getTime())+"   "+n+"/"+total+"   "+pct+"%",13,TEXT));}content.addView(week);
        LinearLayout planWeek=card();planWeek.addView(label("SHORT PLAN • LAST 7 DAYS",11,MUTED));
        int planned=0,plannedDone=0,plannedDays=0;
        for(int i=6;i>=0;i--){Calendar pc=Calendar.getInstance();pc.add(Calendar.DAY_OF_YEAR,-i);String pk=key(pc);int pcnt;try{pcnt=Math.max(0,Math.min(50,prefs.getInt("plan_count_"+pk,0)));}catch(Exception e){pcnt=0;}int pdone=0;for(int j=0;j<pcnt;j++)if(prefs.getBoolean("plan_"+pk+"_"+j+"_done",false))pdone++;planned+=pcnt;plannedDone+=pdone;if(pcnt>0)plannedDays++;int ppct=pcnt==0?0:Math.round(pdone*100f/pcnt);planWeek.addView(label(new SimpleDateFormat("EEE",Locale.US).format(pc.getTime())+"   "+pdone+"/"+pcnt+"   "+ppct+"%",13,TEXT));}
        planWeek.addView(label(plannedDone+"/"+planned+" planned tasks completed  •  "+plannedDays+"/7 days planned",12,MUTED));
        content.addView(planWeek);LinearLayout cal=card();cal.addView(label("LAST 30 DAYS",11,MUTED));for(int i=29;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);String k=key(c);TextView r=label(new SimpleDateFormat("dd MMM",Locale.US).format(c.getTime())+"   "+(prefs.getBoolean("done_"+k,false)?"✓ COMPLETE":countFor(k)>0?"• PARTIAL":"— MISSED"),13,TEXT);cal.addView(r);}content.addView(cal);Button achievements=button("🏆  View Achievements");achievements.setOnClickListener(v->showAchievements());content.addView(achievements);Button chat=button("💬  Chat with Maya");chat.setOnClickListener(v->chatDialog());content.addView(chat);Button journal=button("📝  Daily Journal & Reflection");journal.setOnClickListener(v->journalDialog());content.addView(journal);LinearLayout badges=card();badges.addView(label("ACHIEVEMENTS",11,MUTED));int[] ms={1,7,30,50,100,150,200,309};for(int m:ms)if(completed>=m)badges.addView(label("🏆 "+m+" day milestone unlocked",14,TEXT));else badges.addView(label("🔒 "+m+" day milestone",14,MUTED));content.addView(badges);}
    private void journalDialog(){String today=prefs.getString("journal_"+key(),"");EditText e=new EditText(this);e.setHint("How was today? What did you learn?");e.setMinLines(5);e.setText(today);new AlertDialog.Builder(this).setTitle("📝 Today's Journal").setView(e).setPositiveButton("SAVE",(d,w)->{prefs.edit().putString("journal_"+key(),e.getText().toString().trim()).apply();toast("Journal saved 📝");}).setNegativeButton("CANCEL",null).show();}    private String formatDate(Calendar c){return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(c.getTime());}
    private void completeDay(){String k=key();if(prefs.getBoolean("done_"+k,false)){toast("Today is already completed. 🔥");return;}if(countFor(k)!=totalTasks()){toast("Finish all "+totalTasks()+" tasks first.");return;}int streak=currentStreak()+1;int best=Math.max(bestStreak(),streak);prefs.edit().putBoolean("done_"+k,true).putInt("streak",streak).putInt("best",best).apply();toast("Day completed! +100 XP 🔥");showHome();}
    private void showReminders(){header("REMINDERS","Daily alarms that keep your plan on track.");Button add=button("+  ADD DAILY REMINDER");add.setOnClickListener(v->alarmDialog());content.addView(add);boolean found=false;for(String k:prefs.getAll().keySet())if(k.startsWith("alarm")){try{String v=prefs.getString(k,"");String[] p=v.split("\\|",-1);if(p.length==3){int h=Integer.parseInt(p[1]),m=Integer.parseInt(p[2]);if(h<0||h>23||m<0||m>59)continue;found=true;LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.addView(label("🔔 "+(p[0].isEmpty()?"Discipline reminder":p[0]),14,TEXT),new LinearLayout.LayoutParams(0,-2,1));row.addView(label(String.format(Locale.US,"%02d:%02d",h,m),14,ACCENT));content.addView(row);}}catch(Exception ignored){}}if(!found)content.addView(label("No reminders yet.",14,MUTED));content.addView(label("Reminders survive app restarts. Android may require exact-alarm and notification access.",12,MUTED));}
    private void alarmDialog(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText name=new EditText(this);name.setHint("Reminder name");l.addView(name);TimePicker p=new TimePicker(this);p.setIs24HourView(true);l.addView(p);new AlertDialog.Builder(this).setTitle("Add daily reminder").setView(l).setPositiveButton("SAVE",(d,w)->{schedule(name.getText().toString(),p.getHour(),p.getMinute());showReminders();toast("Reminder saved");}).setNegativeButton("CANCEL",null).show();}
    private void schedule(String name,int h,int m){int id=(name+"|"+h+"|"+m).hashCode();Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);scheduleStatic(this,name,h,m,id,c.getTimeInMillis());prefs.edit().putString("alarm"+id,name+"|"+h+"|"+m).apply();}
    public static void scheduleAll(Context context){SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);for(String k:p.getAll().keySet())if(k.startsWith("alarm")){String v=p.getString(k,null);if(v==null)continue;String[] a=v.split("\\|",-1);if(a.length==3)try{int id=Integer.parseInt(k.substring(5));scheduleStatic(context,a[0],Integer.parseInt(a[1]),Integer.parseInt(a[2]),id,-1);}catch(Exception ignored){}}}
    private static void scheduleStatic(Context c,String name,int h,int m,int id,long requested){Calendar x=Calendar.getInstance();if(requested>0)x.setTimeInMillis(requested);else{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,m);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())x.add(Calendar.DAY_OF_YEAR,1);}AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return;Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("title",name==null||name.trim().isEmpty()?"Discipline reminder":name);in.putExtra("msg","It's time. Start your next task. 🔥");PendingIntent pi=PendingIntent.getBroadcast(c,id,in,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);}catch(SecurityException ignored){}catch(RuntimeException ignored){}}
    private void chatDialog(){EditText input=new EditText(this);input.setHint("Ask Maya in Sinhala, Singlish or English...");input.setMinLines(2);new AlertDialog.Builder(this).setTitle("💬 Maya").setMessage("Ask anything about your routine, discipline or today.").setView(input).setPositiveButton("SEND",(d,w)->askMaya(input.getText().toString())).setNegativeButton("CLOSE",null).show();}
    public String mayaQuickStatus(String type){String k=key();String mission=prefs.getString("mission_"+k,"");if(mission.isEmpty()){String[] missions={"Complete every planned task today","Finish one focused study session","Do your routine before entertainment","Write a 3-line evening reflection","Complete today without skipping a habit"};mission=missions[Math.abs(k.hashCode())%missions.length];}if(type.equals("mission"))return "Today's mission: "+mission+(prefs.getBoolean("mission_"+k+"_done",false)?" — completed! 🎯":" — not completed yet.");if(type.equals("xp"))return "You have "+xp()+" XP, Level "+level()+". "+(500-(xp()%500))+" XP until the next level.";if(type.equals("streak"))return "Current streak: "+currentStreak()+" days. Best streak: "+bestStreak()+" days.";if(type.equals("day"))return "You're on Day "+dayNumber()+" of 309. "+Math.max(0,309-dayNumber())+" days remaining.";if(type.equals("progress"))return "Today: "+countFor(k)+"/"+totalTasks()+" tasks completed. Current streak: "+currentStreak()+" days. XP: "+xp()+".";return "Day "+dayNumber()+"/309 • "+countFor(k)+"/"+totalTasks()+" tasks • "+currentStreak()+" day streak • "+xp()+" XP.";}
    public String buildMayaContext(){return MayaContextProvider.build(this);}
    private void askMaya(String q){String context=buildMayaContext();MayaAI.ask(this,q,context,"auto",reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Exception e){new AlertDialog.Builder(this).setTitle("Settings").setMessage("Open Settings from the app menu.").setPositiveButton("OK",null).show();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();sound(ToneGenerator.TONE_PROP_BEEP);}
    @Override protected void onPause(){super.onPause();syncHandler.removeCallbacks(syncRunnable);try{SupabaseAccountManager.syncLocalProgress(this,null);}catch(Throwable e){android.util.Log.e("309DayDiscipline","Background sync error",e);}}
    @Override protected void onResume(){super.onResume();try{applyTheme();if(SupabaseAccountManager.loggedIn(this)&&"sub".equals(SupabaseAccountManager.role(this))){syncHandler.removeCallbacks(syncRunnable);syncHandler.postDelayed(syncRunnable,3000);}}catch(Throwable e){android.util.Log.e("309DayDiscipline","Resume error",e);}}
    @Override protected void onDestroy(){syncHandler.removeCallbacks(syncRunnable);try{if(voiceAssistant!=null)voiceAssistant.destroy();}catch(Throwable e){android.util.Log.e("309DayDiscipline","Voice cleanup error",e);}try{if(tone!=null)tone.release();}catch(Throwable ignored){}super.onDestroy();}
}