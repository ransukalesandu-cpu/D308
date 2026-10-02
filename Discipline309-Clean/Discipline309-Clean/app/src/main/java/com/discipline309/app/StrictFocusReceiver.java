package com.discipline309.app;
import android.content.*;
public class StrictFocusReceiver extends BroadcastReceiver{
 public static final String ACTION="com.discipline309.STRICT_FOCUS_END";
 @Override public void onReceive(Context c,Intent i){if(ACTION.equals(i.getAction())) StrictModeManager.stopFocus(c);}
}