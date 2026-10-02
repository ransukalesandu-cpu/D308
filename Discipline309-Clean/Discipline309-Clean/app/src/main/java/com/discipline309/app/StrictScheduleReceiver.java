package com.discipline309.app;
import android.content.*;
public class StrictScheduleReceiver extends BroadcastReceiver{
 public static final String ON="com.discipline309.STRICT_ON",OFF="com.discipline309.STRICT_OFF";
 @Override public void onReceive(Context c,Intent i){
   if(ON.equals(i.getAction()))StrictModeManager.setEnabled(c,true);
   else if(OFF.equals(i.getAction()))StrictModeManager.setEnabled(c,false);
   StrictModeManager.reschedule(c);
 }
}