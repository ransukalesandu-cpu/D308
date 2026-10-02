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
  try{SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);JSONObject all=new JSONObject(p.getString(DATA,"{}"));String day=dayKey(Calendar.getInstance());JSONObject d=all.optJSONObject(day);if(d==null)d=new JSONObject();d.put(tone,d.optInt(tone,0)+1);String context=contextOf(text);if(!context.isEmpty()){String old=d.optString("contexts","");java.util.LinkedHashSet<String> set=new java.util.LinkedHashSet<>();if(!old.isEmpty())for(String x:old.split("\\|"))if(!x.trim().isEmpty())set.add(x.trim());set.add(context);StringBuilder cb=new StringBuilder();for(String x:set){if(cb.length()>0)cb.append("|");cb.append(x);}d.put("contexts",cb.toString());}d.remove("last_text");d.put("updated",System.currentTimeMillis());all.put(day,d); pruneOld(all); p.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}
 }
 public static void finalizeDay(Context c,Calendar date){
  try{String day=dayKey(date);SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);JSONObject all=new JSONObject(p.getString(DATA,"{}"));JSONObject d=all.optJSONObject(day);if(d==null)d=new JSONObject();if(d.optBoolean("finalized",false))return;String mood=dominant(d);d.put("mood",mood);d.put("summary",summary(mood,d));
  d.put("reason",reason(mood,d));
  d.put("daily_insight",insight(mood,d));d.put("finalized",true);d.put("finalized_at",System.currentTimeMillis());all.put(day,d); pruneOld(all); p.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}
 }
 private static void pruneOld(JSONObject all){
  Calendar cutoff=Calendar.getInstance();
  cutoff.add(Calendar.DAY_OF_YEAR,-30);
  String key=dayKey(cutoff);
  java.util.Iterator<String> it=all.keys();
  while(it.hasNext()){String k=it.next();if(k.compareTo(key)<0)it.remove();}
 }
 public static JSONObject get(Context c,Calendar date){try{return new JSONObject(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(DATA,"{}")).optJSONObject(dayKey(date));}catch(Exception e){return null;}}
 public static String dayKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
 public static String displayMood(String m){if("positive".equals(m))return "😊 Positive";if("sad".equals(m))return "😔 Sad";if("stressed".equals(m))return "😰 Stressed";if("frustrated".equals(m))return "😤 Frustrated";if("tired".equals(m))return "😴 Tired";return "😐 Neutral";}
 private static String dominant(JSONObject d){String[] a={"positive","neutral","tired","stressed","frustrated","sad"};String b="neutral";int n=-1;for(String x:a){int v=d.optInt(x,0);if(v>n){n=v;b=x;}}return b;}
 public static String getReason(JSONObject d){return d==null?"":d.optString("reason","");}
 public static String getInsight(JSONObject d){return d==null?"":d.optString("daily_insight","");}
 private static String summary(String mood,JSONObject d){int n=0;for(String x:new String[]{"positive","neutral","tired","stressed","frustrated","sad"})n+=d.optInt(x,0);if(n==0)return "No mood conversations were recorded for this day.";return "Main mood: "+displayMood(mood)+". Maya recorded "+n+" mood-related conversation signals.";}
 private static String reason(String mood,JSONObject d){String c=d.optString("contexts","");if(c.isEmpty())return "No clear activity or conversation context was detected.";return "Likely context: "+c.replace("|",", ")+".";}
 private static String insight(String mood,JSONObject d){String c=d.optString("contexts","");if(c.isEmpty())return "No clear reason was detected from today's conversations.";if("positive".equals(mood))return "Today's positive mood was associated with: "+c.replace("|",", ")+". Keep those activities going.";if("sad".equals(mood))return "Today's conversations suggest the mood was affected by: "+c.replace("|",", ")+". A supportive, low-pressure day may help.";if("stressed".equals(mood))return "Today's stress signals were connected with: "+c.replace("|",", ")+".";if("frustrated".equals(mood))return "Today's frustration signals were connected with: "+c.replace("|",", ")+".;";if("tired".equals(mood))return "Today's tired signals were connected with: "+c.replace("|",", ")+". Rest and recovery were part of the day's context.";return "Today's mood signals appeared alongside: "+c.replace("|",", ")+".";}

}