package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.*;
import android.view.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS="discipline";
    private static final String[] DEFAULT_TASKS={"Wake up on time","Study / learning","Workout or active recovery","Eat planned meals","No-phone block","Night review + prepare tomorrow"};
    private int BG=0xFF070B18,SURFACE=0xFF11182E,TEXT=0xFFF7F7FF,MUTED=0xFFA9A8C5,ACCENT=0xFFA78BFA;
    private SharedPreferences prefs;
    private LinearLayout content;
    private VoiceAssistant voiceAssistant;
    private ToneGenerator tone;
    private final Handler syncHandler=new Handler(Looper.getMainLooper());
    private boolean screenIntroDone=false;
    private final Runnable syncRunnable=new Runnable(){@Override public void run(){try{if(isFinishing()||isDestroyed())return;if(SupabaseAccountManager.loggedIn(MainActivity.this)&&"sub".equals(SupabaseAccountManager.role(MainActivity.this))){SupabaseAccountManager.syncLocalProgress(MainActivity.this,null);syncHandler.postDelayed(this,60000);}}catch(Exception e){android.util.Log.e("309DayDiscipline","Periodic sync error",e);try{if(!isFinishing()&&!isDestroyed())syncHandler.postDelayed(this,60000);}catch(Exception ignored){}}}};
    private static final int PICK_MAYA_IMAGE=901,CAPTURE_MAYA_IMAGE=902,PICK_MAYA_DOCUMENT=903;

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void sound(int t){try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,65);tone.startTone(t,80);}catch(Exception ignored){}}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(z>=22?Typeface.create("sans-serif",Typeface.BOLD):z>=16?Typeface.create("sans-serif-medium",Typeface.NORMAL):Typeface.create("sans-serif",Typeface.NORMAL));v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}
    private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),0xFF3A315F);return g;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(shape(SURFACE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(48));b.setBackground(shape(0xFF211B45,16));b.setPadding(dp(10),0,dp(10),0);b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP)sound(ToneGenerator.TONE_PROP_ACK);return false;});return b;}
    private String key(){return key(Calendar.getInstance());}
    private String key(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());}
    private void ensureProgramStart(){try{long saved=prefs.getLong("program_start",0L);if(saved<=0L){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);prefs.edit().putLong("program_start",c.getTimeInMillis()).apply();}}catch(Exception e){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);prefs.edit().putLong("program_start",c.getTimeInMillis()).apply();}}
    private Calendar startDate(){Calendar c=Calendar.getInstance();try{long saved=prefs.getLong("program_start",0L);if(saved<=0L)saved=System.currentTimeMillis();c.setTimeInMillis(saved);}catch(Exception ignored){}return c;}
    private Calendar target(){Calendar c=startDate();c.add(Calendar.DAY_OF_YEAR,308);return c;}
    private int daysFromStart(){return(int)((System.currentTimeMillis()-startDate().getTimeInMillis())/86400000L)+1;}
    private int dayNumber(){return Math.max(0,Math.min(309,daysFromStart()));}
    private String taskName(int i){try{if(i<0||i>=totalTasks())return "Habit";String value=i<DEFAULT_TASKS.length?DEFAULT_TASKS[i]:prefs.getString("habit_"+i,"Habit");if(value==null||value.trim().isEmpty())return "Habit";return value.trim();}catch(Exception e){return "Habit";}}
    private int customCount(){try{return Math.max(0,Math.min(100,prefs.getInt("custom_count",0)));}catch(Exception e){return 0;}}
    private int totalTasks(){return DEFAULT_TASKS.length+customCount();}
    private boolean checked(int i,String d){try{return i>=0&&i<totalTasks()&&d!=null&&!d.isEmpty()&&prefs.getBoolean("task_"+i+"_"+d,false);}catch(Exception e){return false;}}
    private void setChecked(int i,String d,boolean v){try{if(i>=0&&i<totalTasks()&&d!=null&&!d.isEmpty())prefs.edit().putBoolean("task_"+i+"_"+d,v).apply();}catch(Exception ignored){}}
    private boolean allowed(String permission){return !SupabaseAccountManager.loggedIn(this)||SupabaseAccountManager.can(this,permission);}
    private int countFor(String d){int n=0;for(int i=0;i<totalTasks();i++)if(checked(i,d))n++;return n;}
    private int completedDays(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();int guard=0;while(!c.after(now)&&!c.after(target())&&guard++<309){if(prefs.getBoolean("done_"+key(c),false))n++;c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int xp(){try{int bonus=Math.max(0,Math.min(1000000,prefs.getInt("xp_bonus",0)));long total=(long)completedDays()*100L+(long)totalCompletedTasks()*20L+bonus;return (int)Math.min(Integer.MAX_VALUE,total);}catch(Exception e){return completedDays()*100+totalCompletedTasks()*20;}}
    private void awardXp(int amount,String reason){if(amount<=0)return;try{int current=Math.max(0,Math.min(1000000,prefs.getInt("xp_bonus",0)));int next=(int)Math.min(1000000L,(long)current+amount);prefs.edit().putInt("xp_bonus",next).apply();toast(reason+"  +"+amount+" XP");}catch(Exception ignored){}}
    private int totalCompletedTasks(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();int guard=0;while(!c.after(now)&&!c.after(target())&&guard++<309){n+=countFor(key(c));c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int level(){return xp()/500+1;}
    private int currentStreak(){int n=0;Calendar c=Calendar.getInstance();Calendar start=startDate();Calendar end=target();if(c.after(end))c.setTimeInMillis(end.getTimeInMillis());if(!prefs.getBoolean("done_"+key(c),false))c.add(Calendar.DAY_OF_YEAR,-1);while(!c.before(start)&&n<309&&prefs.getBoolean("done_"+key(c),false)){n++;c.add(Calendar.DAY_OF_YEAR,-1);}return n;}
    private int bestStreak(){int best=0,run=0;try{Calendar c=startDate();Calendar now=Calendar.getInstance();int guard=0;while(!c.after(now)&&!c.after(target())&&guard++<309){if(prefs.getBoolean("done_"+key(c),false))run++;else run=0;best=Math.max(best,run);c.add(Calendar.DAY_OF_YEAR,1);}int saved=Math.max(0,Math.min(309,prefs.getInt("best",0)));return Math.max(best,saved);}catch(Exception e){return Math.max(0,Math.min(309,best));}}

    @Override protected void onCreate(Bundle b){super.onCreate(b);try{prefs=getSharedPreferences(PREFS,MODE_PRIVATE);ensureProgramStart();applyTheme();buildShell();showHome();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}catch(Throwable e){android.util.Log.e("309DayDiscipline","Startup error",e);showStartupFallback();}}
    private void showStartupFallback(){try{LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(BG);TextView title=label("309 DAY DISCIPLINE",26,TEXT);title.setGravity(Gravity.CENTER);root.addView(title);TextView msg=label("Startup problem එකක් හඳුනාගත්තා. App එක crash නොවී safe mode එකෙන් open වුණා.",15,MUTED);msg.setGravity(Gravity.CENTER);root.addView(msg,new LinearLayout.LayoutParams(-1,-2));Button retry=button("↻  TRY AGAIN");retry.setOnClickListener(v->{try{buildShell();showHome();}catch(Throwable e){android.util.Log.e("309DayDiscipline","Retry startup error",e);}});root.addView(retry,new LinearLayout.LayoutParams(-1,dp(52)));setContentView(root);}catch(Throwable ignored){}}
    private void applyTheme(){String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");BG=0xFF070B18;SURFACE=0xFF11182E;TEXT=0xFFF7F7FF;MUTED=0xFFA9A8C5;ACCENT=0xFFA78BFA;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
    private Button[] navButtons;
    private int selectedNav=0;

    private void updateNavSelection(int selected){
        selectedNav=selected;
        if(navButtons==null)return;
        for(int i=0;i<navButtons.length;i++){
            Button b=navButtons[i];
            boolean active=i==selected;
            b.setTextColor(active?Color.WHITE:MUTED);
            b.setTypeface(active?Typeface.create("sans-serif-medium",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
            b.setBackground(shape(active?0xFF2A2256:0x00000000,14));
            b.setElevation(active?dp(3):0);
            b.setScaleX(active?1.16f:0.90f);
            b.setScaleY(active?1.16f:0.90f);
            b.animate().scaleX(active?1.16f:0.90f).scaleY(active?1.16f:0.90f).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            String[] navLabels={"Home","Plan","Habits","Notes","Stats"};
            b.setContentDescription(navLabels[i] + (active?" selected":""));
        }
    }

    private void buildShell(){
    FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
    ScrollView sc=new ScrollView(this);sc.setOverScrollMode(View.OVER_SCROLL_ALWAYS);sc.setSmoothScrollingEnabled(true);
    content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(28),dp(18),dp(110));sc.addView(content);
    root.addView(sc,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(shape(0xFF0C1226,18));
    String[] names={"⌂","◷","✓","📝","◫"};
    navButtons=new Button[5];
    for(int i=0;i<5;i++){
        final int n=i;
        Button nb=button(names[i]);
        nb.setTextSize(22);
        nb.setPadding(0,0,0,0);
        nb.setMinHeight(dp(48));
        nb.setGravity(Gravity.CENTER);
        nb.setOnClickListener(v->{
            updateNavSelection(n);
            if(n==0)showHome();else if(n==1)showShortPlan();else if(n==2)showHabits();else if(n==3)showNotes();else showStats();
        });
        navButtons[i]=nb;
        nav.addView(nb,new LinearLayout.LayoutParams(0,dp(58),1));
    }
    Button settings=button("⚙\nSettings");
    settings.setTextSize(13);
    settings.setTextColor(MUTED);
    settings.setGravity(Gravity.CENTER);
    settings.setContentDescription("Settings");
    settings.setOnClickListener(v->openSettings());
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(74),dp(58));
    sp.setMargins(dp(6),0,0,0);
    nav.addView(settings,sp);
    updateNavSelection(0);
    FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM);
    np.setMargins(dp(8),0,dp(8),dp(8));
    root.addView(nav,np);
    Button fab=button("＋");fab.setTextSize(30);fab.setTextColor(Color.WHITE);fab.setGravity(Gravity.CENTER);fab.setPadding(0,0,0,0);fab.setBackground(shape(0xFFA78BFA,100));fab.setElevation(dp(12));fab.setContentDescription("Create note");fab.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)v.animate().scaleX(.88f).scaleY(.88f).setDuration(90).start();else if(e.getAction()==MotionEvent.ACTION_UP){v.animate().scaleX(1f).scaleY(1f).setDuration(180).start();noteEditor(-1);}else if(e.getAction()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(180).start();return true;});
    FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(62),dp(62),Gravity.RIGHT|Gravity.BOTTOM);fp.setMargins(0,0,dp(22),dp(88));root.addView(fab,fp);
    setContentView(root);
}
    private void header(String title,String sub){content.removeAllViews();
    LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.HORIZONTAL);heading.setGravity(Gravity.CENTER_VERTICAL);
    String pageIcon="";if(title.contains("HOME")||title.contains("309 DAY"))pageIcon="⌂";else if(title.contains("PLAN"))pageIcon="◷";else if(title.contains("HABITS"))pageIcon="✓";else if(title.contains("NOTES"))pageIcon="📝";else if(title.contains("STATS")||title.contains("ACHIEVEMENTS"))pageIcon="◫";
    if(!pageIcon.isEmpty()){TextView i=label(pageIcon,25,ACCENT);i.setGravity(Gravity.CENTER);heading.addView(i,new LinearLayout.LayoutParams(dp(38),dp(42)));}
    TextView t=label(title,26,TEXT);t.setTypeface(null,1);heading.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));Button settings=button("⚙");settings.setTextSize(22);settings.setTextColor(MUTED);settings.setGravity(Gravity.CENTER);settings.setPadding(0,0,0,0);settings.setContentDescription("Settings");settings.setBackground(shape(0x0011182E,50));settings.setOnClickListener(v->openSettings());heading.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));content.addView(heading);
    content.addView(label(sub,13,MUTED));
    ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_discipline);icon.setContentDescription("Discipline");icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(52),dp(52));ip.gravity=Gravity.CENTER_HORIZONTAL;ip.topMargin=dp(5);content.addView(icon,ip);
}
    private TextView title(String s){TextView t=label(s,19,TEXT);t.setTypeface(null,1);t.setPadding(dp(4),dp(14),dp(4),dp(5));return t;}
    private void addBar(LinearLayout box,int value,int max){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(Math.max(0,Math.min(max,value)));p.setProgressDrawable(getDrawable(android.R.drawable.progress_horizontal));box.addView(p,new LinearLayout.LayoutParams(-1,dp(10)));}
    private void showMayaToolsMenu(){String[] items={"🖼️  Photo","📷  Camera","📄  Document","📝  Journal","⏰  Reminders"};new AlertDialog.Builder(this).setTitle("Maya Tools").setItems(items,(d,w)->{if(w==0)openMayaGallery();else if(w==1)openMayaCamera();else if(w==2)openMayaDocument();else if(w==3)journalDialog();else showReminders();}).show();}
    private void openMayaDocument(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_DOCUMENT);}catch(Exception e){toast("Document picker එක open කරන්න බැරි වුණා.");}}
    private void analyzeMayaDocument(Uri uri){if(uri==null)return;String type=getContentResolver().getType(uri);if(type!=null&&type.equals("application/pdf")){new Thread(()->{try{android.os.ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new Exception("fd");android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd);if(renderer.getPageCount()==0)throw new Exception("empty");android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0);int width=Math.min(Math.max(1,page.getWidth()*2),2400),height=Math.min(Math.max(1,page.getHeight()*2),2400);Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);page.close();renderer.close();fd.close();runOnUiThread(()->analyzeMayaBitmapWithQuestion(bitmap,"Read and summarize this document page. Extract visible important text and explain it."));}catch(Exception e){runOnUiThread(()->toast("PDF එක read කරන්න බැරි වුණා."));}}).start();}else{String text=MayaVision.documentText(this,uri);if(text.isEmpty()){toast("මේ document type එක තවම support වෙන්නේ නැහැ. Text file හෝ PDF එකක් තෝරන්න.");return;}String clipped=text.length()>12000?text.substring(0,12000):text;MayaAI.ask(this,"Analyze this document and summarize the important points:\n"+clipped,"Document provided by user","document analyst",reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}}
    private void analyzeMayaBitmapWithQuestion(Bitmap bitmap,String question){try{java.io.File file=new java.io.File(getCacheDir(),"maya_doc_page.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();MayaVision.analyze(this,Uri.fromFile(file),question,reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}catch(Exception e){toast("Document page process කරන්න බැරි වුණා.");}}
    private void openMayaGallery(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_IMAGE);}catch(Exception e){toast("Gallery picker එක open කරන්න බැරි වුණා.");}}
    private void openMayaCamera(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},33);return;}try{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAPTURE_MAYA_IMAGE);}catch(Exception e){toast("Camera open කරන්න බැරි වුණා.");}}
    private void analyzeMayaImage(Uri uri){if(uri==null)return;final EditText q=new EditText(this);q.setHint("Ask Maya about this image (optional)");new AlertDialog.Builder(this).setTitle("🖼️ Ask Maya about image").setView(q).setPositiveButton("ANALYZE",(d,w)->{String question=q.getText().toString().trim();toast("Maya image එක බලනවා… 🧠");MayaVision.analyze(this,uri,question,reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}).setNegativeButton("CANCEL",null).show();}
    private void analyzeMayaBitmap(Bitmap bitmap){try{java.io.File file=new java.io.File(getCacheDir(),"maya_camera.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();analyzeMayaImage(Uri.fromFile(file));}catch(Exception e){toast("Camera image process කරන්න බැරි වුණා.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;try{if(requestCode==PICK_MAYA_IMAGE){Uri uri=data.getData();if(uri!=null)analyzeMayaImage(uri);}else if(requestCode==PICK_MAYA_DOCUMENT){Uri uri=data.getData();if(uri!=null)analyzeMayaDocument(uri);}else if(requestCode==CAPTURE_MAYA_IMAGE){Bitmap b=data.getParcelableExtra("data");if(b!=null)analyzeMayaBitmap(b);}}catch(Exception e){toast("Selected file/image process කරන්න බැරි වුණා.");}}

    private void showHome(){header("309 DAY DISCIPLINE","Build discipline. One day at a time. 🔥");int day=dayNumber(),done=countFor(key()),total=totalTasks(),pct=total==0?0:Math.round(done*100f/total);LinearLayout hero=card();hero.addView(label(day==0?"PROGRAM STARTS SOON":"DAY "+day+" / 309",12,MUTED));hero.addView(label(day==0?formatDate(startDate()):"Keep moving. "+Math.max(0,309-day)+" days remaining.",24,TEXT));addBar(hero,Math.max(0,day),309);hero.addView(label("Today  "+pct+"%   •   "+done+"/"+total+" tasks   •   🔥 "+currentStreak()+" day streak",13,MUTED));content.addView(hero);        int planTotal=planCount(), planDone=0;
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
content.addView(title("TODAY'S MISSION"));LinearLayout mission=card();mission.addView(label("🎯 DAILY CHALLENGE",11,MUTED));String missionKey="mission_"+key();String[] missions={"Complete every planned task today","Finish one focused study session","Do your routine before entertainment","Write a 3-line evening reflection","Complete today without skipping a habit"};int missionIndex=(key().hashCode()&0x7fffffff)%missions.length;String missionText=prefs.getString(missionKey,missions[missionIndex]);if(missionText==null||missionText.trim().isEmpty())missionText=missions[missionIndex];mission.addView(label(missionText,18,TEXT));mission.addView(label("Reward: +50 XP  •  Resets tomorrow",12,MUTED));CheckBox missionDone=new CheckBox(this);missionDone.setText("Mission complete");missionDone.setTextColor(TEXT);missionDone.setChecked(prefs.getBoolean(missionKey+"_done",false));missionDone.setEnabled(allowed("can_edit_mission"));missionDone.setOnCheckedChangeListener((v,checked)->{if(!allowed("can_edit_mission")){v.setChecked(!checked);toast("Primary account has disabled mission editing.");return;}prefs.edit().putBoolean(missionKey+"_done",checked).apply();if(checked&&!prefs.getBoolean(missionKey+"_rewarded",false)){prefs.edit().putBoolean(missionKey+"_rewarded",true).apply();awardXp(50,"Mission complete! 🎯");}});mission.addView(missionDone);content.addView(mission);content.addView(title("TODAY'S HABITS"));for(int i=0;i<total;i++)addTaskRow(i,key());Button complete=button("✓  COMPLETE TODAY'S CHALLENGE");complete.setOnClickListener(v->completeDay());content.addView(complete);LinearLayout quick=card();quick.addView(label("MAYA + QUICK ACTIONS",11,MUTED));Button plan=button("📋  Short Planning Mode");plan.setBackground(shape(0xFF1769FF,18));plan.setOnClickListener(v->showShortPlan());quick.addView(plan);Button maya=button("🎙️\nTalk to Maya");maya.setTextSize(18);maya.setTypeface(null,1);maya.setGravity(Gravity.CENTER);maya.setPadding(0,0,0,0);maya.setBackground(shape(0xFF1769FF,100));maya.setElevation(dp(10));maya.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){v.animate().scaleX(.90f).scaleY(.90f).setDuration(90).start();}else if(e.getAction()==MotionEvent.ACTION_UP){v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();if(voiceAssistant==null)voiceAssistant=new VoiceAssistant(this);voiceAssistant.start();}else if(e.getAction()==MotionEvent.ACTION_CANCEL){v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();}return true;});quick.addView(maya,new LinearLayout.LayoutParams(-1,dp(92)));Button mayaTools=button("🧰  Maya Tools");mayaTools.setTextSize(15);mayaTools.setGravity(Gravity.CENTER);mayaTools.setContentDescription("Maya tools");mayaTools.setOnClickListener(v->showMayaToolsMenu());quick.addView(mayaTools);content.addView(quick);showMilestoneCard();animateScreenIntro();}

    private void animateScreenIntro(){
        if(screenIntroDone){return;} screenIntroDone=true; for(int i=0;i<content.getChildCount();i++){ View v=content.getChildAt(i); v.setAlpha(0f); v.setTranslationY(dp(18)); v.setScaleX(.98f); v.setScaleY(.98f); v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(Math.min(i*35,280)).setDuration(360).setInterpolator(new android.view.animation.DecelerateInterpolator()).start(); } }
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
        if(!validPlanIndex(i)){showShortPlan();return;}
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
        int n=planCount();
        if(n<=0||index<0||index>=n){showShortPlan();return;}
        SharedPreferences.Editor e=prefs.edit();
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
    private void editHabitDialog(int idx){if(idx<DEFAULT_TASKS.length||idx>=totalTasks()){showHabits();return;}if(!allowed("can_edit_habits")){toast("Primary account has disabled habit editing.");return;}EditText e=new EditText(this);e.setText(taskName(idx));new AlertDialog.Builder(this).setTitle("Edit habit").setView(e).setPositiveButton("SAVE",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())prefs.edit().putString("habit_"+idx,s).apply();showHabits();}).setNeutralButton("DELETE",(d,w)->deleteHabit(idx)).setNegativeButton("CANCEL",null).show();}
    private void deleteHabit(int idx){if(!allowed("can_edit_habits")){toast("Primary account has disabled habit editing.");return;}int count=customCount();int first=DEFAULT_TASKS.length;int last=first+count-1;if(count<=0||idx<first||idx>last){showHabits();return;}if(idx!=last){String name=prefs.getString("habit_"+last,"Habit");prefs.edit().putString("habit_"+idx,name).remove("habit_"+last).putInt("custom_count",Math.max(0,count-1)).apply();}else prefs.edit().remove("habit_"+idx).putInt("custom_count",Math.max(0,count-1)).apply();showHabits();}
    private int notesCount(){try{return Math.max(0,Math.min(500,prefs.getInt("notes_count",0)));}catch(Exception e){return 0;}}
    private String noteTitle(int i){return prefs.getString("note_"+i+"_title","Untitled note");}
    private String noteBody(int i){return prefs.getString("note_"+i+"_body","");}
    private long noteTime(int i){return prefs.getLong("note_"+i+"_time",0L);}
    private boolean notePinned(int i){return prefs.getBoolean("note_"+i+"_pinned",false);}

    private String noteCategory(int i){String c=prefs.getString("note_"+i+"_category","Personal");return Arrays.asList("Personal","Study","Goals").contains(c)?c:"Personal";}
    private String noteRemoteId(int i){return prefs.getString("note_"+i+"_remote_id","");}

    private void showNotes(){
        header("NOTES","Capture ideas fast. Maya can organize, summarize and plan. 📝");
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        Button add=button("＋ NEW NOTE");add.setOnClickListener(v->noteEditor(-1));top.addView(add,new LinearLayout.LayoutParams(0,dp(50),1));
        if("primary".equals(SupabaseAccountManager.role(this))){Button family=button("👥 FAMILY NOTES");family.setOnClickListener(v->showFamilyNotes());LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(145),dp(50));fp.setMargins(dp(8),0,0,0);top.addView(family,fp);}
        content.addView(top);
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search notes…");search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(15);search.setPadding(dp(14),0,dp(14),0);search.setBackground(shape(SURFACE,16));content.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        content.addView(label("🏷 PERSONAL  •  STUDY  •  GOALS",11,MUTED));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable render=()->{
            list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.US);ArrayList<Integer> ids=new ArrayList<>();
            for(int i=0;i<notesCount();i++){String h=(noteTitle(i)+" "+noteBody(i)+" "+noteCategory(i)).toLowerCase(Locale.US);if(q.isEmpty()||h.contains(q))ids.add(i);}
            Collections.sort(ids,(x,y)->{if(notePinned(x)!=notePinned(y))return notePinned(x)?-1:1;return Long.compare(noteTime(y),noteTime(x));});
            if(ids.isEmpty()){list.addView(label(q.isEmpty()?"No notes yet. Tap NEW NOTE or +.":"No matching notes.",14,MUTED));return;}
            for(int id:ids){LinearLayout nc=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView tt=label((notePinned(id)?"📌 ":"")+noteTitle(id),18,TEXT);tt.setTypeface(null,1);row.addView(tt,new LinearLayout.LayoutParams(0,-2,1));Button more=button("⋮");more.setOnClickListener(v->noteMenu(id));row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(44)));nc.addView(row);nc.addView(label("🏷 "+noteCategory(id),11,ACCENT));String body=noteBody(id).trim();if(!body.isEmpty()){String preview=body.replace("\n"," ");if(preview.length()>180)preview=preview.substring(0,180)+"...";nc.addView(label(preview,14,TEXT));}if(noteTime(id)>0)nc.addView(label(new SimpleDateFormat("dd MMM yyyy • HH:mm",Locale.US).format(new Date(noteTime(id))),11,MUTED));nc.setOnClickListener(v->noteEditor(id));list.addView(nc);}
        };
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int d){}public void onTextChanged(CharSequence s,int a,int b,int d){render.run();}public void afterTextChanged(android.text.Editable e){}});
        render.run();if(SupabaseAccountManager.loggedIn(this))SupabaseAccountManager.syncLocalNotes(this,null);
    }

    private void noteEditor(int id){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText title=new EditText(this);title.setSingleLine(true);title.setHint("Title");title.setText(id>=0?noteTitle(id):"");box.addView(title);
        Spinner category=new Spinner(this);String[] cats={"Personal","Study","Goals"};category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));if(id>=0)for(int i=0;i<cats.length;i++)if(cats[i].equals(noteCategory(id)))category.setSelection(i);box.addView(category);
        EditText body=new EditText(this);body.setHint("Type your note here…");body.setMinLines(9);body.setGravity(Gravity.TOP);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);body.setText(id>=0?noteBody(id):"");box.addView(body,new LinearLayout.LayoutParams(-1,dp(220)));
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER);Button voice=button("🎙 VOICE INPUT");voice.setOnClickListener(v->startNoteVoice(body));tools.addView(voice,new LinearLayout.LayoutParams(0,dp(50),1));Button summarize=button("🧠 SUMMARIZE");summarize.setOnClickListener(v->summarizeText(body.getText().toString()));tools.addView(summarize,new LinearLayout.LayoutParams(0,dp(50),1));box.addView(tools);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(id>=0?"📝 Edit Note":"📝 New Note").setView(box).setPositiveButton("SAVE",null).setNegativeButton("CANCEL",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String t=title.getText().toString().trim(),b=body.getText().toString().trim();if(t.isEmpty()&&b.isEmpty()){toast("Write something first.");return;}if(t.isEmpty())t="Untitled note";int idx=id;if(idx<0){idx=notesCount();if(idx>=500){toast("Notes limit reached.");return;}prefs.edit().putInt("notes_count",idx+1).apply();}prefs.edit().putString("note_"+idx+"_title",t).putString("note_"+idx+"_body",b).putString("note_"+idx+"_category",cats[category.getSelectedItemPosition()]).putLong("note_"+idx+"_time",System.currentTimeMillis()).apply();d.dismiss();showNotes();if(SupabaseAccountManager.loggedIn(this))SupabaseAccountManager.syncLocalNotes(this,null);}));
        d.show();
    }

    private void startNoteVoice(EditText target){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},44);toast("Microphone permission allow කරන්න.");return;}
        try{if(!SpeechRecognizer.isRecognitionAvailable(this)){toast("Voice recognition available නැහැ.");return;}SpeechRecognizer sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){toast("කියන්න… 🎙️");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}public void onError(int e){try{sr.destroy();}catch(Exception ignored){}toast("Voice input එක try කරන්න.");}public void onResults(Bundle b){ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty()){String old=target.getText().toString().trim();target.setText(old.isEmpty()?a.get(0):old+"\n"+a.get(0));target.setSelection(target.length());}try{sr.destroy();}catch(Exception ignored){}}});Intent in=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);in.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"si-LK");in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"si-LK");sr.startListening(in);}catch(Exception e){toast("Voice input start කරන්න බැරි වුණා.");}
    }

    private void summarizeText(String text){
        String q=text==null?"":text.trim();if(q.isEmpty()){toast("Summarize කරන්න text එකක් දාන්න.");return;}String clipped=q.length()>12000?q.substring(0,12000):q;toast("Maya summary එක හදනවා… 🧠");MayaAI.ask(this,"Summarize this note into 3-5 short bullet points. Keep only the key ideas.\n\n"+clipped,"User note","clear concise note assistant",reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("🧠 Maya Summary").setMessage(reply==null?"No summary received.":reply).setPositiveButton("OK",null).show();}));
    }

    private void convertNoteToTask(int id){
        String body=noteBody(id).trim();if(body.isEmpty()){toast("Task එක හදන්න note body එකක් ඕන.");return;}String clipped=body.length()>8000?body.substring(0,8000):body;toast("Maya actionable task එක හොයනවා… 🧠");MayaAI.ask(this,"From this note, identify ONE actionable task. Reply with only the task text, no bullets, no explanation. If there is no actionable task, reply with NONE.\n\n"+clipped,"User note","task extractor",reply->runOnUiThread(()->{String task=reply==null?"":reply.trim().replace("\n"," ");if(task.isEmpty()||task.equalsIgnoreCase("NONE")){toast("Actionable task එකක් හමු වුණේ නැහැ.");return;}new AlertDialog.Builder(this).setTitle("➕ Add to Daily Planner").setMessage(task).setNegativeButton("CANCEL",null).setPositiveButton("ADD TASK",(d,w)->addPlannerTaskFromNote(task)).show();}));
    }

    private void addPlannerTaskFromNote(String task){int n=planCount();if(n>=50){toast("Daily Planner task limit reached.");return;}prefs.edit().putInt("plan_count_"+planDate(),n+1).putString("plan_"+planDate()+"_"+n+"_name",task).putString("plan_"+planDate()+"_"+n+"_time","Anytime").putString("plan_"+planDate()+"_"+n+"_priority","High").apply();toast("Task added to today's planner. ✅");showShortPlan();}

    private void showFamilyNotes(){
        if(!"primary".equals(SupabaseAccountManager.role(this))){showNotes();return;}header("FAMILY NOTES","Read-only linked sub-account notes. 👥");content.addView(label("Primary can read linked notes. Editing and deleting are disabled.",12,MUTED));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        SupabaseAccountManager.loadLinkedNotes(this,(ok,message)->runOnUiThread(()->{list.removeAllViews();if(!ok){list.addView(label("Could not load family notes.",14,MUTED));return;}try{org.json.JSONArray a=new org.json.JSONArray(message);if(a.length()==0){list.addView(label("No linked notes yet.",14,MUTED));return;}for(int i=0;i<a.length();i++){org.json.JSONObject n=a.getJSONObject(i);LinearLayout nc=card();nc.addView(label("👤 "+n.optString("owner_name","Sub-account"),12,ACCENT));nc.addView(label(n.optString("title","Untitled note"),18,TEXT));nc.addView(label("🏷 "+n.optString("category","Personal"),11,MUTED));nc.addView(label(n.optString("body",""),14,TEXT));list.addView(nc);}}catch(Exception e){list.addView(label("Family notes data could not be displayed.",14,MUTED));}}));
    }

    private void noteMenu(int id){
        String[] a={"Edit","📌 "+(notePinned(id)?"Unpin":"Pin"),"🧠 Summarize","➕ Convert to Daily Task","Share","Delete"};
        new AlertDialog.Builder(this).setTitle(noteTitle(id)).setItems(a,(d,w)->{if(w==0)noteEditor(id);else if(w==1){prefs.edit().putBoolean("note_"+id+"_pinned",!notePinned(id)).apply();showNotes();}else if(w==2)summarizeText(noteBody(id));else if(w==3)convertNoteToTask(id);else if(w==4){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,noteTitle(id));i.putExtra(Intent.EXTRA_TEXT,noteTitle(id)+"\n\n"+noteBody(id));try{startActivity(Intent.createChooser(i,"Share note"));}catch(Exception ignored){}}else new AlertDialog.Builder(this).setTitle("Delete note?").setMessage("This note will be removed from this device and synced storage.").setPositiveButton("DELETE",(dd,ww)->deleteNote(id)).setNegativeButton("CANCEL",null).show();}).show();
    }

    private void deleteNote(int id){
        int n=notesCount();if(id<0||id>=n){showNotes();return;}String remote=noteRemoteId(id);SharedPreferences.Editor e=prefs.edit();for(int i=id;i<n-1;i++){e.putString("note_"+i+"_title",prefs.getString("note_"+(i+1)+"_title","Untitled note")).putString("note_"+i+"_body",prefs.getString("note_"+(i+1)+"_body","")).putString("note_"+i+"_category",prefs.getString("note_"+(i+1)+"_category","Personal")).putLong("note_"+i+"_time",prefs.getLong("note_"+(i+1)+"_time",0L)).putBoolean("note_"+i+"_pinned",prefs.getBoolean("note_"+(i+1)+"_pinned",false)).putString("note_"+i+"_remote_id",prefs.getString("note_"+(i+1)+"_remote_id",""));}e.remove("note_"+(n-1)+"_title").remove("note_"+(n-1)+"_body").remove("note_"+(n-1)+"_category").remove("note_"+(n-1)+"_time").remove("note_"+(n-1)+"_pinned").remove("note_"+(n-1)+"_remote_id").putInt("notes_count",n-1).apply();if(!remote.isEmpty())SupabaseAccountManager.deleteRemoteNote(this,remote,null);showNotes();
    }

    private void showAchievements(){header("ACHIEVEMENTS","Milestones earned through consistency.");int days=completedDays(),streak=bestStreak(),missions=0;for(String k:prefs.getAll().keySet())if(k.startsWith("mission_")&&k.endsWith("_rewarded")&&prefs.getBoolean(k,false))missions++;LinearLayout summary=card();summary.addView(label("🏆 "+days+" completed days",22,TEXT));summary.addView(label("🔥 Best streak: "+streak+" days  •  🎯 Missions: "+missions,13,MUTED));content.addView(summary);int[] milestones={1,3,7,14,30,50,100,150,200,309};String[] names={"First Step","3-Day Spark","One Week","Two Weeks","30-Day Discipline","50-Day Warrior","100-Day Mastery","150-Day Elite","200-Day Relentless","309-Day Legend"};for(int i=0;i<milestones.length;i++){int m=milestones[i];boolean u=days>=m;LinearLayout a=card();a.addView(label(u?"🏆 "+names[i]:"🔒 "+names[i],17,u?TEXT:MUTED));a.addView(label(m+" completed days",12,MUTED));content.addView(a);}int[] streaks={3,7,14,30};String[] sn={"3-Day Streak","7-Day Streak","14-Day Streak","30-Day Streak"};for(int i=0;i<streaks.length;i++){boolean u=streak>=streaks[i];LinearLayout a=card();a.addView(label(u?"🔥 "+sn[i]:"🔒 "+sn[i],17,u?TEXT:MUTED));a.addView(label(streaks[i]+" consecutive completed days",12,MUTED));content.addView(a);}LinearLayout m=card();m.addView(label("🎯 MISSION ACHIEVEMENTS",11,MUTED));int[] mm={1,7,30};for(int x:mm)m.addView(label(missions>=x?"🏆 "+x+" daily missions completed":"🔒 "+x+" daily missions",14,missions>=x?TEXT:MUTED));content.addView(m);}
    private void showStats(){header("PROGRESS & STATS","See the full picture, not just today's streak.");int completed=completedDays(),best=bestStreak(),today=countFor(key()),total=totalTasks();LinearLayout summary=card();summary.addView(label("LEVEL "+level(),11,MUTED));summary.addView(label(completed+" completed days",23,TEXT));summary.addView(label("🔥 "+currentStreak()+" current  •  🏆 "+best+" best streak",14,MUTED));summary.addView(label("⚡ "+xp()+" total XP  •  "+totalCompletedTasks()+" completed tasks",14,MUTED));content.addView(summary);LinearLayout week=card();week.addView(label("LAST 7 DAYS",11,MUTED));for(int i=6;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);int n=countFor(key(c));int pct=total==0?0:Math.round(n*100f/total);week.addView(label(new SimpleDateFormat("EEE",Locale.US).format(c.getTime())+"   "+n+"/"+total+"   "+pct+"%",13,TEXT));}content.addView(week);
        LinearLayout planWeek=card();planWeek.addView(label("SHORT PLAN • LAST 7 DAYS",11,MUTED));
        int planned=0,plannedDone=0,plannedDays=0;
        for(int i=6;i>=0;i--){Calendar pc=Calendar.getInstance();pc.add(Calendar.DAY_OF_YEAR,-i);String pk=key(pc);int pcnt;try{pcnt=Math.max(0,Math.min(50,prefs.getInt("plan_count_"+pk,0)));}catch(Exception e){pcnt=0;}int pdone=0;for(int j=0;j<pcnt;j++)if(prefs.getBoolean("plan_"+pk+"_"+j+"_done",false))pdone++;planned+=pcnt;plannedDone+=pdone;if(pcnt>0)plannedDays++;int ppct=pcnt==0?0:Math.round(pdone*100f/pcnt);planWeek.addView(label(new SimpleDateFormat("EEE",Locale.US).format(pc.getTime())+"   "+pdone+"/"+pcnt+"   "+ppct+"%",13,TEXT));}
        planWeek.addView(label(plannedDone+"/"+planned+" planned tasks completed  •  "+plannedDays+"/7 days planned",12,MUTED));
        content.addView(planWeek);LinearLayout cal=card();cal.addView(label("LAST 30 DAYS",11,MUTED));for(int i=29;i>=0;i--){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-i);String k=key(c);TextView r=label(new SimpleDateFormat("dd MMM",Locale.US).format(c.getTime())+"   "+(prefs.getBoolean("done_"+k,false)?"✓ COMPLETE":countFor(k)>0?"• PARTIAL":"— MISSED"),13,TEXT);cal.addView(r);}content.addView(cal);Button achievements=button("🏆  View Achievements");achievements.setOnClickListener(v->showAchievements());content.addView(achievements);Button chat=button("💬  Chat with Maya");chat.setOnClickListener(v->chatDialog());content.addView(chat);Button journal=button("📝  Daily Journal & Reflection");journal.setOnClickListener(v->journalDialog());content.addView(journal);LinearLayout badges=card();badges.addView(label("ACHIEVEMENTS",11,MUTED));int[] ms={1,7,30,50,100,150,200,309};for(int m:ms)if(completed>=m)badges.addView(label("🏆 "+m+" day milestone unlocked",14,TEXT));else badges.addView(label("🔒 "+m+" day milestone",14,MUTED));content.addView(badges);}
    private void journalDialog(){String today=prefs.getString("journal_"+key(),"");EditText e=new EditText(this);e.setHint("How was today? What did you learn?");e.setMinLines(5);e.setText(today);new AlertDialog.Builder(this).setTitle("📝 Today's Journal").setView(e).setPositiveButton("SAVE",(d,w)->{prefs.edit().putString("journal_"+key(),e.getText().toString().trim()).apply();toast("Journal saved 📝");}).setNegativeButton("CANCEL",null).show();}    private String formatDate(Calendar c){return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(c.getTime());}
    private void completeDay(){int day=dayNumber();if(day<=0||day>309){toast(day<=0?"The 309-day program has not started yet.":"The 309-day program is already complete.");return;}String k=key();if(prefs.getBoolean("done_"+k,false)){toast("Today is already completed. 🔥");return;}if(countFor(k)!=totalTasks()){toast("Finish all "+totalTasks()+" tasks first.");return;}int streak=currentStreak()+1;int best=Math.max(bestStreak(),streak);prefs.edit().putBoolean("done_"+k,true).putInt("streak",streak).putInt("best",best).apply();toast("Day completed! +100 XP 🔥");showHome();}
    private void showReminders(){header("REMINDERS","Daily alarms that keep your plan on track.");Button add=button("+  ADD DAILY REMINDER");add.setOnClickListener(v->alarmDialog());content.addView(add);boolean found=false;for(String k:prefs.getAll().keySet())if(k.startsWith("alarm")){try{String v=prefs.getString(k,"");String[] p=v.split("\\|",-1);if(p.length==3){int h=Integer.parseInt(p[1]),m=Integer.parseInt(p[2]);if(h<0||h>23||m<0||m>59)continue;found=true;LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.addView(label("🔔 "+(p[0].isEmpty()?"Discipline reminder":p[0]),14,TEXT),new LinearLayout.LayoutParams(0,-2,1));row.addView(label(String.format(Locale.US,"%02d:%02d",h,m),14,ACCENT));content.addView(row);}}catch(Exception ignored){}}if(!found)content.addView(label("No reminders yet.",14,MUTED));content.addView(label("Reminders survive app restarts. Android may require exact-alarm and notification access.",12,MUTED));}
    private void alarmDialog(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText name=new EditText(this);name.setHint("Reminder name");l.addView(name);TimePicker p=new TimePicker(this);p.setIs24HourView(true);l.addView(p);new AlertDialog.Builder(this).setTitle("Add daily reminder").setView(l).setPositiveButton("SAVE",(d,w)->{schedule(name.getText().toString(),p.getHour(),p.getMinute());showReminders();toast("Reminder saved");}).setNegativeButton("CANCEL",null).show();}
    private void schedule(String name,int h,int m){int id=(name+"|"+h+"|"+m).hashCode();Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);scheduleStatic(this,name,h,m,id,c.getTimeInMillis());prefs.edit().putString("alarm"+id,name+"|"+h+"|"+m).apply();}
    public static void scheduleAll(Context context){SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);for(String k:p.getAll().keySet())if(k.startsWith("alarm")){String v=p.getString(k,null);if(v==null)continue;String[] a=v.split("\\|",-1);if(a.length==3)try{int id=Integer.parseInt(k.substring(5));scheduleStatic(context,a[0],Integer.parseInt(a[1]),Integer.parseInt(a[2]),id,-1);}catch(Exception ignored){}}}
    private static void scheduleStatic(Context c,String name,int h,int m,int id,long requested){Calendar x=Calendar.getInstance();if(requested>0)x.setTimeInMillis(requested);else{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,m);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())x.add(Calendar.DAY_OF_YEAR,1);}AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return;Intent in=new Intent(c,AlarmReceiver.class);in.putExtra("title",name==null||name.trim().isEmpty()?"Discipline reminder":name);in.putExtra("msg","It's time. Start your next task. 🔥");PendingIntent pi=PendingIntent.getBroadcast(c,id,in,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);}catch(SecurityException ignored){}catch(RuntimeException ignored){}}
    private void chatDialog(){EditText input=new EditText(this);input.setHint("Ask Maya in Sinhala, Singlish or English...");input.setMinLines(2);new AlertDialog.Builder(this).setTitle("💬 Maya").setMessage("Ask anything about your routine, discipline or today.").setView(input).setPositiveButton("SEND",(d,w)->askMaya(input.getText().toString())).setNegativeButton("CLOSE",null).show();}
    public String mayaQuickStatus(String type){if(type==null)type="";String k=key();String mission=prefs.getString("mission_"+k,"");if(mission==null)mission="";if(mission.isEmpty()){String[] missions={"Complete every planned task today","Finish one focused study session","Do your routine before entertainment","Write a 3-line evening reflection","Complete today without skipping a habit"};mission=missions[(k.hashCode()&0x7fffffff)%missions.length];}if(type.equals("mission"))return "Today's mission: "+mission+(prefs.getBoolean("mission_"+k+"_done",false)?" — completed! 🎯":" — not completed yet.");if(type.equals("xp"))return "You have "+xp()+" XP, Level "+level()+". "+(500-(xp()%500))+" XP until the next level.";if(type.equals("streak"))return "Current streak: "+currentStreak()+" days. Best streak: "+bestStreak()+" days.";if(type.equals("day"))return "You're on Day "+dayNumber()+" of 309. "+Math.max(0,309-dayNumber())+" days remaining.";if(type.equals("progress"))return "Today: "+countFor(k)+"/"+totalTasks()+" tasks completed. Current streak: "+currentStreak()+" days. XP: "+xp()+".";return "Day "+dayNumber()+"/309 • "+countFor(k)+"/"+totalTasks()+" tasks • "+currentStreak()+" day streak • "+xp()+" XP.";}
    public String buildMayaContext(){return MayaContextProvider.build(this);}
    private void askMaya(String q){String context=buildMayaContext();MayaAI.ask(this,q,context,"auto",reply->runOnUiThread(()->{if(isFinishing()||isDestroyed())return;try{new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply==null?"No response received.":reply).setPositiveButton("OK",null).show();}catch(Exception ignored){}}));}
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Exception e){new AlertDialog.Builder(this).setTitle("Settings").setMessage("Open Settings from the app menu.").setPositiveButton("OK",null).show();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();sound(ToneGenerator.TONE_PROP_BEEP);}
    @Override protected void onPause(){super.onPause();try{syncHandler.removeCallbacks(syncRunnable);}catch(Throwable e){android.util.Log.e("309DayDiscipline","Background sync cleanup error",e);}}
    @Override protected void onResume(){super.onResume();try{applyTheme();if(SupabaseAccountManager.loggedIn(this)&&"sub".equals(SupabaseAccountManager.role(this))){syncHandler.removeCallbacks(syncRunnable);syncHandler.postDelayed(syncRunnable,3000);}}catch(Throwable e){android.util.Log.e("309DayDiscipline","Resume error",e);}}
    @Override protected void onDestroy(){syncHandler.removeCallbacks(syncRunnable);try{if(voiceAssistant!=null)voiceAssistant.destroy();}catch(Throwable e){android.util.Log.e("309DayDiscipline","Voice cleanup error",e);}try{if(tone!=null)tone.release();}catch(Throwable ignored){}super.onDestroy();}
}
