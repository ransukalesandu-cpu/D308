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
    private final Runnable syncRunnable=new Runnable(){@Override public void run(){if(SupabaseAccountManager.loggedIn(MainActivity.this)&&"sub".equals(SupabaseAccountManager.role(MainActivity.this))){SupabaseAccountManager.syncLocalProgress(MainActivity.this,null);syncHandler.postDelayed(this,15000);}}};
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
    private int customCount(){return prefs.getInt("custom_count",0);}
    private int totalTasks(){return DEFAULT_TASKS.length+customCount();}
    private boolean checked(int i,String d){return prefs.getBoolean("task_"+i+"_"+d,false);}
    private void setChecked(int i,String d,boolean v){prefs.edit().putBoolean("task_"+i+"_"+d,v).apply();}
    private boolean allowed(String permission){return !SupabaseAccountManager.loggedIn(this)||SupabaseAccountManager.can(this,permission);}
    private int countFor(String d){int n=0;for(int i=0;i<totalTasks();i++)if(checked(i,d))n++;return n;}
    private int completedDays(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){if(prefs.getBoolean("done_"+key(c),false))n++;c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int xp(){return completedDays()*100+totalCompletedTasks()*20+prefs.getInt("xp_bonus",0);}
    private void awardXp(int amount,String reason){if(amount<=0)return;prefs.edit().putInt("xp_bonus",prefs.getInt("xp_bonus",0)+amount).apply();toast(reason+"  +"+amount+" XP");}
    private int totalCompletedTasks(){int n=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){n+=countFor(key(c));c.add(Calendar.DAY_OF_YEAR,1);}return n;}
    private int level(){return xp()/500+1;}
    private int currentStreak(){int n=0;Calendar c=Calendar.getInstance();if(!prefs.getBoolean("done_"+key(c),false))c.add(Calendar.DAY_OF_YEAR,-1);while(!c.before(startDate())&&prefs.getBoolean("done_"+key(c),false)){n++;c.add(Calendar.DAY_OF_YEAR,-1);}return n;}
    private int bestStreak(){int best=0,run=0;Calendar c=startDate();Calendar now=Calendar.getInstance();while(!c.after(now)&&!c.after(target())){if(prefs.getBoolean("done_"+key(c),false))run++;else run=0;best=Math.max(best,run);c.add(Calendar.DAY_OF_YEAR,1);}return Math.max(best,prefs.getInt("best",0));}

    @Override protected void onCreate(Bundle b){super.onCreate(b);try{prefs=getSharedPreferences(PREFS,MODE_PRIVATE);ensureProgramStart();applyTheme();buildShell();showHome();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}catch(Throwable e){android.util.Log.e("309DayDiscipline","Startup error",e);showStartupFallback();}}
    private void showStartupFallback(){try{LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(BG);TextView title=label("309 DAY DISCIPLINE",26,TEXT);title.setGravity(Gravity.CENTER);root.addView(title);TextView msg=label("Startup problem එකක් හඳුනාගත්තා. App එක crash නොවී safe mode එකෙන් open වුණා.",15,MUTED);msg.setGravity(Gravity.CENTER);root.addView(msg,new LinearLayout.LayoutParams(-1,-2));Button retry=button("↻  TRY AGAIN");retry.setOnClickListener(v->{try{buildShell();showHome();}catch(Throwable e){android.util.Log.e("309DayDiscipline","Retry startup error",e);}});root.addView(retry,new LinearLayout.LayoutParams(-1,dp(52)));setContentView(root);}catch(Throwable ignored){}}
    private void applyTheme(){String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");BG=0xFF061126;SURFACE=0xFF0B1B3A;TEXT=0xFFF5F8FF;MUTED=0xFF9CB2D9;ACCENT=0xFF2F7BFF;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
    private void buildShell(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(14),dp(18),dp(12));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(2),dp(4),dp(2),dp(4));nav.setBackground(shape(0xFF081A36,18));String[] names={"⌂\nHome","◷\nPlan","✓\nHabits","◫\nStats","⚙\nSettings"};for(int i=0;i<5;i++){final int n=i;Button b=button(names[i]);b.setTextSize(11);b.setPadding(0,0,0,0);b.setOnClickListener(v->{if(n==0)showHome();else if(n==1)showShortPlan();else if(n==2)showHabits();else if(n==3)showStats();else openSettings();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));}root.addView(nav);setContentView(root);}
    private void header(String title,String sub){content.removeAllViews();TextView t=label(title,26,TEXT);t.setTypeface(null,1);content.addView(t);content.addView(label(sub,13,MUTED));ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_discipline);icon.setContentDescription("Discipline");icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(64),dp(64));ip.gravity=Gravity.CENTER_HORIZONTAL;ip.topMargin=dp(5);content.addView(icon,ip);}
    private TextView title(String s){TextView t=label(s,19,TEXT);t.setTypeface(null,1);t.setPadding(dp(4),dp(14),dp(4),dp(5));return t;}
    private void addBar(LinearLayout box,int value,int max){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(Math.max(0,Math.min(max,value)));p.setProgressDrawable(getDrawable(android.R.drawable.progress_horizontal));box.addView(p,new LinearLayout.LayoutParams(-1,dp(10)));}
    private void openMayaDocument(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_DOCUMENT);}
    private void analyzeMayaDocument(Uri uri){if(uri==null)return;String type=getContentResolver().getType(uri);if(type!=null&&type.equals("application/pdf")){new Thread(()->{try{android.os.ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new Exception("fd");android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd);if(renderer.getPageCount()==0)throw new Exception("empty");android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0);Bitmap bitmap=Bitmap.createBitmap(page.getWidth()*2,page.getHeight()*2,Bitmap.Config.ARGB_8888);page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);page.close();renderer.close();fd.close();runOnUiThread(()->analyzeMayaBitmapWithQuestion(bitmap,"Read and summarize this document page. Extract visible important text and explain it."));}catch(Exception e){runOnUiThread(()->toast("PDF එක read කරන්න බැරි වුණා."));}}).start();}else{String text=MayaVision.documentText(this,uri);if(text.isEmpty()){toast("මේ document type එක තවම support වෙන්නේ නැහැ. Text file හෝ PDF එකක් තෝරන්න.");return;}String clipped=text.length()>12000?text.substring(0,12000):text;MayaAI.ask(this,"Analyze this document and summarize the important points:\n"+clipped,"Document provided by user","document analyst",reply->runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply).setPositiveButton("OK",null).show()));}}
    private void analyzeMayaBitmapWithQuestion(Bitmap bitmap,String question){try{java.io.File file=new java.io.File(getCacheDir(),"maya_doc_page.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();MayaVision.analyze(this,Uri.fromFile(file),question,reply->runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Maya 📄").setMessage(reply).setPositiveButton("OK",null).show()));}catch(Exception e){toast("Document page process කරන්න බැරි වුණා.");}}
    private void openMayaGallery(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MAYA_IMAGE);}
    private void openMayaCamera(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},33);return;}try{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAPTURE_MAYA_IMAGE);}catch(Exception e){toast("Camera open කරන්න බැරි වුණා.");}}
    private void analyzeMayaImage(Uri uri){if(uri==null)return;final EditText q=new EditText(this);q.setHint("Ask Maya about this image (optional)");new AlertDialog.Builder(this).setTitle("🖼️ Ask Maya about image").setView(q).setPositiveButton("ANALYZE",(d,w)->{String question=q.getText().toString().trim();toast("Maya image එක බලනවා… 🧠");MayaVision.analyze(this,uri,question,reply->runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Maya 🧠").setMessage(reply).setPositiveButton("OK",null).show()));}).setNegativeButton("CANCEL",null).show();}
    private void analyzeMayaBitmap(Bitmap bitmap){try{java.io.File file=new java.io.File(getCacheDir(),"maya_camera.jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);out.close();analyzeMayaImage(Uri.fromFile(file));}catch(Exception e){toast("Camera image process කරන්න බැරි වුණා.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;if(requestCode==PICK_MAYA_IMAGE)analyzeMayaImage(data.getData());else if(requestCode==PICK_MAYA_DOCUMENT)analyzeMayaDocument(data.getData());else if(requestCode==CAPTURE_MAYA_IMAGE){Bitmap b=data.getParcelableExtra("data");if(b!=null)analyzeMayaBitmap(b);}}

    private void showHome(){header("309 DAY DISCIPLINE","Build discipline. One day at a time. 🔥");int day=dayNumber(),done=countFor(key()),total=totalTasks(),pct=total==0?0:Math.round(done*100f/total);LinearLayout hero=card();hero.addView(label(day==0?"PROGRAM STARTS SOON":"DAY "+day+" / 309",12,MUTED));hero.addView(label(day==0?formatDate(startDate()):"Keep moving. "+Math.max(0,309-day)+" days remaining.",24,TEXT));addBar(hero,Math.max(0,day),309);hero.addView(label("Today  "+pct+"%   •   "+done+"/"+total+" tasks   •   🔥 "+currentStreak()+" day streak",13,MUTED));content.addView(hero);LinearLayout xpBox=card();xpBox.addView(label("LEVEL "+level(),11,MUTED));xpBox.addView(label(xp()+" XP",23,TEXT));addBar(xpBox,xp()%500,500);xpBox.addView(label((500-(xp()%500))+" XP to next level",12,MUTED));content.addView(xpBox);
        int planTotal=planCount(), planDone=0;
        for(int i=0;i<planTotal;i++) if(planTaskDone(i)) planDone++;
        String focus=prefs.getString("plan_"+key()+"_goal","");
        LinearLayout planSummary=card();
        planSummary.addView(label("📋 SHORT PLAN",11,MUTED));
        planSummary.addView(label(planTotal==0?"No short plan yet":planDone+"/"+planTotal+" planned tasks complete",18,TEXT));