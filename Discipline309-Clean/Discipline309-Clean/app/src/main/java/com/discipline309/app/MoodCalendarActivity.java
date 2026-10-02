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
  CalendarView cal=new CalendarView(this);root.addView(cal,new LinearLayout.LayoutParams(-1,dp(340)));
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(14),dp(14),dp(14));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
  showDay(card,Calendar.getInstance());
  cal.setOnDateChangeListener((v,y,m,d)->{Calendar c=Calendar.getInstance();c.set(y,m,d);showDay(card,c);});
  Button back=new Button(this);back.setText("← Back");back.setAllCaps(false);back.setOnClickListener(v->finish());root.addView(back);
  setContentView(root);
 }
 private void showDay(LinearLayout card,Calendar c){
  card.removeAllViews();
  String date=new SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.US).format(c.getTime());
  TextView h=tv(date,19,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(h);
  JSONObject d=DailyMoodStore.get(this,c);
  if(d==null||!d.optBoolean("finalized",false)){card.addView(tv("No finalized mood summary for this day yet.",15,0xFFA9A8C5));return;}
  card.addView(tv(DailyMoodStore.displayMood(d.optString("mood","neutral")),22,Color.WHITE));
  card.addView(tv(d.optString("summary",""),15,0xFFD8D6EA));
  String last=d.optString("last_text","");
  if(!last.isEmpty())card.addView(tv("Last mood signal: "+last,13,0xFFA9A8C5));
 }
}