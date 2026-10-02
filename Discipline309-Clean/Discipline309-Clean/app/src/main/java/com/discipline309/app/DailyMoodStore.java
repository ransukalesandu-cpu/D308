package com.discipline309.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;
public final class DailyMoodStore {
 private static final String PREF="maya_daily_mood",DATA="days";
 private DailyMoodStore(){}
 public static void record(Context c,String tone,String text){
  if(tone==null||tone.isEmpty())tone="neutral";
  try{SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);JSONObject all=new JSONObject(p.getString(DATA,"{}"));String day=dayKey(Calendar.getInstance());JSONObject d=all.optJSONObject(day);if(d==null)d=new JSONObject();d.put(tone,d.optInt(tone,0)+1);d.put("last_text",safe(text,180));d.put("updated",System.currentTimeMillis());all.put(day,d);p.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}
 }
 public static void finalizeDay(Context c,Calendar date){
  try{String day=dayKey(date);SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);JSONObject all=new JSONObject(p.getString(DATA,"{}"));JSONObject d=all.optJSONObject(day);if(d==null)d=new JSONObject();if(d.optBoolean("finalized",false))return;String mood=dominant(d);d.put("mood",mood);d.put("summary",summary(mood,d));d.put("finalized",true);d.put("finalized_at",System.currentTimeMillis());all.put(day,d);p.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}
 }
 public static JSONObject get(Context c,Calendar date){try{return new JSONObject(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(DATA,"{}")).optJSONObject(dayKey(date));}catch(Exception e){return null;}}
 public static String dayKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
 public static String displayMood(String m){if("positive".equals(m))return "😊 Positive";if("sad".equals(m))return "😔 Sad";if("stressed".equals(m))return "😰 Stressed";if("frustrated".equals(m))return "😤 Frustrated";if("tired".equals(m))return "😴 Tired";return "😐 Neutral";}
 private static String dominant(JSONObject d){String[] a={"positive","neutral","tired","stressed","frustrated","sad"};String b="neutral";int n=-1;for(String x:a){int v=d.optInt(x,0);if(v>n){n=v;b=x;}}return b;}
 private static String summary(String mood,JSONObject d){int n=0;for(String x:new String[]{"positive","neutral","tired","stressed","frustrated","sad"})n+=d.optInt(x,0);if(n==0)return "No mood conversations were recorded for this day.";return "Main mood: "+displayMood(mood)+". Maya recorded "+n+" mood-related conversation signals.";}
 private static String safe(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n):s;}
}