package com.discipline309.app;

import android.app.*;
import android.content.*;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    private static final int MAYA_BOOT_NOTIFICATION_ID = 3101;

    @Override public void onReceive(Context c, Intent i) {
        try { MainActivity.scheduleAll(c); } catch (Exception ignored) {}
        try { MoodAlarm.scheduleNext(c); } catch (Exception ignored) {}
        try { StrictModeManager.reschedule(c); } catch (Exception ignored) {}
        try { StrictAlarmManager.rescheduleAll(c); } catch (Exception ignored) {}

        String action = i == null ? null : i.getAction();
        boolean bootEvent =
                Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);

        if (!bootEvent) return;

        try {
            android.content.SharedPreferences maya =
                    c.getSharedPreferences("maya_settings", Context.MODE_PRIVATE);
            boolean enabled = maya.getBoolean("enabled", false);

            if (enabled &&
                (!SupabaseAccountManager.loggedIn(c) ||
                 SupabaseAccountManager.can(c, "can_use_maya"))) {
                showMayaBootNotification(c);
            }
        } catch (Exception ignored) {}
    }

    private void showMayaBootNotification(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        "maya_boot",
                        "Maya after restart",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Lets you reactivate Maya after the phone restarts.");
                nm.createNotificationChannel(channel);
            }
        }

        Intent open = new Intent(c, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                c, 3101, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, "maya_boot")
                : new Notification.Builder(c);

        builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Maya is ready")
                .setContentText("Tap to reactivate Maya after the phone restart.")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(Notification.PRIORITY_DEFAULT);

        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(MAYA_BOOT_NOTIFICATION_ID, builder.build());
    }
}
