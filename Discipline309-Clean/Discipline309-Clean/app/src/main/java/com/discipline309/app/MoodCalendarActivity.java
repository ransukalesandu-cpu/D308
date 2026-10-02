package com.discipline309.app;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoodCalendarActivity extends Activity {
 private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView tv(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(12),dp(10),dp(12),dp(10));return v;}
 @Override protected void onCreate(Bundle b){super.onCreate(b);build();}
 private void build(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(18));root.setBackgroundColor(Color.rgb(10,14,26));
  TextView title=tv("📅  MOOD CALENDAR",25,Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);
  root.addView(tv("Past days → tap a date to see Maya's saved mood summary.",14,0xFFA9A8C5));
  Button overview=new Button(this);overview.setText("📊  30-Day Mood Overview");overview.setAllCaps(false);overview.setOnClickListener(v->showOverview());root.addView(overview);
  CalendarView cal=new CalendarView(this);root.addView(cal,new LinearLayout.LayoutParams(-1,dp(340)));
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(14),dp(14),dp(14));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
  showDay(card,Calendar.getInstance());
  cal.setOnDateChangeListener((v,y,m,d)->{Calendar c=Calendar.getInstance();c.set(y,m,d);showDay(card,c);});
  Button back=new Button(this);back.setText("← Back");back.setAllCaps(false);back.setOnClickListener(v->finish());root.addView(back);
  setContentView(root);
 }
 private void showOverview(){
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(22),dp(18),dp(18));body.setBackgroundColor(Color.rgb(10,14,26));
  TextView title=tv("📊  30-DAY MOOD OVERVIEW",24,Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);body.addView(title);
  int[] counts=new int[6];String[] keys={"positive","neutral","tired","stressed","frustrated","sad"};String[] labels={"😊 Positive","😐 Neutral","😴 Tired","😰 Stressed","😤 Frustrated","😔 Sad"};int days=0;
  Calendar c=Calendar.getInstance();
  for(int i=0;i<30;i++){JSONObject d=DailyMoodStore.get(this,c);if(d!=null&&d.optBoolean("finalized",false)){days++;for(int j=0;j<keys.length;j++)counts[j]+=d.optInt(keys[j],0);}c.add(Calendar.DAY_OF_YEAR,-1);}
  body.addView(tv(days+" finalized days in the last 30 days",15,0xFFA9A8C5));
  int total=0;for(int n:counts)total+=n;
  for(int i=0;i<keys.length;i++){
   String pct=total==0?"0%":Math.round(counts[i]*100f/total)+"%";
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
   TextView label=tv(labels[i],15,Color.WHITE);row.addView(label,new LinearLayout.LayoutParams(dp(112),-2));
   ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
   bar.setMax(Math.max(1,total));bar.setProgress(counts[i]);row.addView(bar,new LinearLayout.LayoutParams(0,dp(18),1));
   row.addView(tv(" "+counts[i]+" • "+pct,14,0xFFD8D6EA),new LinearLayout.LayoutParams(dp(95),-2));
   body.addView(row);
  }
  body.addView(tv("Last 30 days • mood signals only • no raw conversation text is shown.",13,0xFFA9A8C5));
  body.addView(tv("📅 DAILY MOOD MAP",19,Color.WHITE));
  Calendar map=Calendar.getInstance();
  for(int r0=0;r0<5;r0++){
   LinearLayout line=new LinearLayout(this);
   for(int col=0;col<6;col++){
    JSONObject md=DailyMoodStore.get(this,map);
    TextView cell=tv(md!=null&&md.optBoolean("finalized",false)?moodEmoji(md.optString("mood","neutral")):"·",20,Color.WHITE);
    cell.setGravity(Gravity.CENTER);line.addView(cell,new LinearLayout.LayoutParams(0,dp(42),1));
    map.add(Calendar.DAY_OF_YEAR,-1);
   }
   body.addView(line);
  }
  Button back=new Button(this);back.setText("← Back to Calendar");back.setAllCaps(false);back.setOnClickListener(v->build());body.addView(back);
  setContentView(body);
 }

 private void showDay(LinearLayout card,Calendar c){
  card.removeAllViews();
  String date=new SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.US).format(c.getTime());
  TextView h=tv(date,19,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(h);
  JSONObject d=DailyMoodStore.get(this,c);
  if(d==null||!d.optBoolean("finalized",false)){card.addView(tv("No finalized mood summary for this day yet.",15,0xFFA9A8C5));return;}
  card.addView(tv(DailyMoodStore.displayMood(d.optString("mood","neutral")),22,Color.WHITE));
  card.addView(tv(d.optString("summary",""),15,0xFFD8D6EA));
  card.addView(tv("🧩 Why / context",17,Color.WHITE));
  card.addView(tv(DailyMoodStore.getReason(d),14,0xFFA9A8C5));

 }
 private String moodEmoji(String m){if("positive".equals(m))return "😊";if("sad".equals(m))return "😔";if("stressed".equals(m))return "😰";if("frustrated".equals(m))return "😤";if("tired".equals(m))return "😴";return "😐";}
}