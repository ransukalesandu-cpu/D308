package com.discipline309.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
public class MoodDayReceiver extends BroadcastReceiver {
 public static final String ACTION_FINALIZE="com.discipline309.app.FINALIZE_MOOD_DAY";
 @Override public void onReceive(Context c,Intent i){if(i!=null&&ACTION_FINALIZE.equals(i.getAction())){Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_YEAR,-1);DailyMoodStore.finalizeDay(c,d);MoodAlarm.scheduleNext(c);}}
}