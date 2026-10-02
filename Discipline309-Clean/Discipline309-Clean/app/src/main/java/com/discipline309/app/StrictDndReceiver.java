package com.discipline309.app;
import android.content.*;
public class StrictDndReceiver extends BroadcastReceiver{
 public static final String ACTION="com.discipline309.STRICT_DND_OFF";
 @Override public void onReceive(Context c,Intent i){if(ACTION.equals(i.getAction())) StrictModeManager.setDnd(c,false);}
}