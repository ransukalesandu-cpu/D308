package com.discipline309.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "discipline";
    private static final String[] TASKS = {
            "Wake up on time",
            "Study / learning",
            "Workout or active recovery",
            "Eat planned meals",
            "No-phone block",
            "Night review + prepare tomorrow"
    };

    private SharedPreferences preferences;
    private TextView stats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setPadding(8, 10, 8, 10);
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        return button;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private TextView label(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(dp(4), dp(4), dp(4), dp(4)); return v;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(25, 29, 39)); bg.setCornerRadius(dp(18));
        c.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7)); c.setLayoutParams(p);
        return c;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(11, 14, 20));
        scroll.addView(root); setContentView(scroll);

        TextView title = label("309 DAY DISCIPLINE", 27, Color.WHITE);
        title.setTypeface(null, 1); root.addView(title);
        root.addView(label("Build discipline. One day at a time.", 14, Color.rgb(170,178,195)));

        LinearLayout hero = card();
        hero.addView(label("TARGET", 12, Color.rgb(145,155,175)));
        hero.addView(label("01 AUG 2027", 23, Color.WHITE));
        stats = label("", 15, Color.rgb(220,225,235));
        stats.setPadding(4, dp(12), 4, dp(4)); hero.addView(stats);
        root.addView(hero);

        LinearLayout streak = card();
        streak.addView(label("🔥  STREAK", 12, Color.rgb(145,155,175)));
        TextView streakValue = label("", 20, Color.WHITE);
        streakValue.setTypeface(null,1); streak.setTag(streakValue); streak.addView(streakValue);
        streak.addView(label("Complete today's plan to keep your streak alive.", 13, Color.rgb(170,178,195)));
        root.addView(streak);

        TextView section = label("TODAY'S PLAN", 19, Color.WHITE);
        section.setTypeface(null,1); root.addView(section);

        for (int index = 0; index < TASKS.length; index++) {
            final int taskIndex = index;
            LinearLayout row = card();
            CheckBox cb = new CheckBox(this);
            cb.setText(TASKS[index]); cb.setTextColor(Color.WHITE); cb.setTextSize(15);
            cb.setChecked(preferences.getBoolean("t" + index + dateKey(), false));
            cb.setOnCheckedChangeListener((buttonView, checked) -> {
                preferences.edit().putBoolean("t" + taskIndex + dateKey(), checked).apply();
                refreshStats();
            });
            row.addView(cb); root.addView(row);
        }

        Button complete = button("✓  COMPLETE TODAY'S CHALLENGE");
        complete.setOnClickListener(view -> completeDay()); root.addView(complete);

        LinearLayout actions = card();
        actions.addView(label("QUICK ACTIONS", 12, Color.rgb(145,155,175)));
        Button alarms = button("⏰  Alarms & reminders");
        alarms.setOnClickListener(view -> alarmDialog()); actions.addView(alarms);
        Button assistant = button("💬  Discipline Assistant");
        assistant.setOnClickListener(view -> chatDialog()); actions.addView(assistant);
        Button settings = button("⚙  Settings");
        settings.setOnClickListener(view -> settingsDialog()); actions.addView(settings);
        root.addView(actions);
        refreshStats();
    }
    private String dateKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private void refreshStats() {
        int completed = 0;
        for (int index = 0; index < TASKS.length; index++) {
            if (preferences.getBoolean("t" + index + dateKey(), false)) completed++;
        }
        int percentage = Math.round(completed * 100f / TASKS.length);
        long daysLeft = Math.max(0L, (targetCalendar().getTimeInMillis() - System.currentTimeMillis()) / 86400000L);
        stats.setText("Today: " + percentage + "%  •  " + completed + "/" + TASKS.length
                + " tasks\nStreak: " + preferences.getInt("streak", 0)
                + " 🔥   Best: " + preferences.getInt("best", 0)
                + " 🏆\nDays until target: " + daysLeft);
    }

    private Calendar targetCalendar() {
        Calendar target = Calendar.getInstance();
        target.set(2027, Calendar.AUGUST, 1, 0, 0, 0);
        target.set(Calendar.MILLISECOND, 0);
        return target;
    }

    private void completeDay() {
        for (int index = 0; index < TASKS.length; index++) {
            if (!preferences.getBoolean("t" + index + dateKey(), false)) {
                toast("Finish all tasks first.");
                return;
            }
        }
        int streak = preferences.getInt("streak", 0) + 1;
        int best = Math.max(preferences.getInt("best", 0), streak);
        preferences.edit()
                .putInt("streak", streak)
                .putInt("best", best)
                .putBoolean("done" + dateKey(), true)
                .apply();
        toast("Day completed! 🔥");
        refreshStats();
    }

    private void alarmDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TimePicker picker = new TimePicker(this);
        picker.setIs24HourView(true);
        EditText name = new EditText(this);
        name.setHint("Alarm name");
        layout.addView(name);
        layout.addView(picker);

        new AlertDialog.Builder(this)
                .setTitle("Add daily alarm")
                .setView(layout)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    schedule(this, name.getText().toString(), picker.getHour(), picker.getMinute());
                    toast("Daily alarm saved");
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void schedule(Context context, String name, int hour, int minute) {
        int id = (name + hour + minute).hashCode();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleStatic(context, name, hour, minute, id, calendar.getTimeInMillis());
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("alarm" + id, name + "|" + hour + "|" + minute)
                .apply();
    }

    public static void scheduleAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("alarm")) continue;
            String value = prefs.getString(key, null);
            if (value == null) continue;
            String[] parts = value.split("\\|", -1);
            if (parts.length != 3) continue;
            try {
                int hour = Integer.parseInt(parts[1]);
                int minute = Integer.parseInt(parts[2]);
                int id = key.substring(5).hashCode();
                scheduleStatic(context, parts[0], hour, minute, id, -1L);
            } catch (Exception ignored) {
            }
        }
    }

    private static void scheduleStatic(Context context, String name, int hour, int minute,
                                       int id, long requestedTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (requestedTime > 0) {
            calendar.setTimeInMillis(requestedTime);
        } else if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("title", name == null || name.trim().isEmpty() ? "Discipline reminder" : name);
        intent.putExtra("msg", "නැගිටින්න! දැන්ම වැඩේ පටන් ගන්න. Do it! Do it! 🔥");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void chatDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView chat = text("Assistant: ආයුබෝවන්! අද වැඩේ පටන් ගමු. 💪\n", 15);
        EditText input = new EditText(this);
        input.setHint("සිංහලෙන් හෝ English වලින් type කරන්න...");
        layout.addView(chat);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("AI Discipline Assistant")
                .setView(layout)
                .setPositiveButton("SEND", (dialog, which) -> {
                    String question = input.getText().toString();
                    chat.setText("Assistant: " + localReply(question));
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private String localReply(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("නින්ද") || q.contains("sleep")) {
            return "දැන් phone එක පැත්තකින් තියලා නිදාගන්න. හෙට වෙලාවට නැගිටින්න. 💪";
        }
        if (q.contains("බැහැ") || q.contains("can't")) {
            return "එකපාරටම හැමදේම කරන්න ඕනේ නෑ. එක task එකක් දැන්ම පටන් ගන්න. Do it! 🔥";
        }
        return "හරි. දැන්ම පොඩි step එකක් ගන්න. ඔයාට මේක complete කරන්න පුළුවන්. Do it! Do it! 🔥";
    }

    private void settingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Voice & permissions")
                .setMessage("Sinhala voice depends on the Text-to-Speech voice installed on your Android device.\n\nFor reliable alarms, allow Notifications and Exact alarms when Android asks.\n\nThe assistant works offline with built-in coaching.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
