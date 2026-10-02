package com.discipline309.app;

import android.app.Application;
import android.app.LocaleManager;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class Discipline309Application extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 33) {
            android.content.SharedPreferences p =
                    getSharedPreferences("ui_settings", MODE_PRIVATE);
            if (!p.getBoolean("language_selected", false)) {
                LocaleManager lm = (LocaleManager) getSystemService(LocaleManager.class);
                if (lm != null) {
                    lm.setApplicationLocales(new LocaleList(Locale.ENGLISH));
                }
            }
        }
    }
}
