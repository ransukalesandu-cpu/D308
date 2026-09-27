package com.discipline309.app;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class MayaNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(!"com.whatsapp".equals(sbn.getPackageName())) return;
        Notification n=sbn.getNotification();
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if(TextUtils.isEmpty(text)) return;
        String value=(title==null?"":title.toString())+": "+text.toString();
        getSharedPreferences("maya_notifications",MODE_PRIVATE).edit().putString("latest",value).apply();
    }
}