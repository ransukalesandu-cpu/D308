package com.discipline309.app;

import android.content.*;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        try { MainActivity.scheduleAll(c); } catch (Exception ignored) {}

        String action = i == null ? null : i.getAction();
        boolean bootEvent =
                Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);

        if (bootEvent) {
            try {
                android.content.SharedPreferences maya =
                        c.getSharedPreferences("maya_settings", Context.MODE_PRIVATE);
                boolean enabled = maya.getBoolean("enabled", false);

                if (enabled &&
                    (!SupabaseAccountManager.loggedIn(c) ||
                     SupabaseAccountManager.can(c, "can_use_maya"))) {
                    Intent service = new Intent(c, MayaAssistantService.class);
                    if (Build.VERSION.SDK_INT >= 26) {
                        c.startForegroundService(service);
                    } else {
                        c.startService(service);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
