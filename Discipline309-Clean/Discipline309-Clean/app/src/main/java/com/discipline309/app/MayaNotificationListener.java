package com.discipline309.app;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class MayaNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        try {
        if(sbn==null || !"com.whatsapp".equals(sbn.getPackageName())) return;
        Notification n=sbn.getNotification();
        if(n==null||n.extras==null)return;
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if(TextUtils.isEmpty(text)) return;
        String value=(title==null?"":title.toString())+": "+text.toString();
        getSharedPreferences("maya_notifications",MODE_PRIVATE).edit().putString("latest",value).apply();
        } catch (Exception ignored) {}
    }
}