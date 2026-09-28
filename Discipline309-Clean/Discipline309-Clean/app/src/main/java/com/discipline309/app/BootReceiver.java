package com.discipline309.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        try { MainActivity.scheduleAll(c); } catch (Exception ignored) {}
    }
}
