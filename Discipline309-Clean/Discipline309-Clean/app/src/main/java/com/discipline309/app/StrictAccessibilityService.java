package com.discipline309.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.*;
import android.os.*;
import java.util.*;

public class StrictAccessibilityService extends AccessibilityService{
 private String current="";
 private long entered=0;
 private Handler h=new Handler(Looper.getMainLooper());
 private final Runnable minute=new Runnable(){public void run(){
   if(!StrictModeManager.isEnabled(StrictAccessibilityService.this)||current.isEmpty()){h.removeCallbacks(this);return;}
   if(StrictModeManager.isSelected(StrictAccessibilityService.this,current)){
     StrictModeManager.addMinute(StrictAccessibilityService.this,current);
     if(StrictModeManager.limitReached(StrictAccessibilityService.this,current)){
       Intent i=new Intent(StrictAccessibilityService.this,StrictBlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
       startActivity(i);
     }
   }
   h.postDelayed(this,60000L);
 }};
 @Override public void onAccessibilityEvent(AccessibilityEvent e){
   if(!StrictModeManager.isEnabled(this)||e==null)return;
   if(e.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;
   CharSequence p=e.getPackageName();if(p==null)return;
   String pkg=p.toString();
   if(pkg.equals(getPackageName())||pkg.equals("com.android.systemui")||pkg.equals("com.android.settings")||StrictModeManager.whitelisted(this).contains(pkg))return;
   current=pkg;entered=System.currentTimeMillis();h.removeCallbacks(minute);h.postDelayed(minute,60000L);
   if(StrictModeManager.limitReached(this,pkg)){
     Intent i=new Intent(this,StrictBlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);
   }
 }
 @Override public void onInterrupt(){h.removeCallbacksAndMessages(null);}
 @Override public void onDestroy(){h.removeCallbacksAndMessages(null);super.onDestroy();}
}