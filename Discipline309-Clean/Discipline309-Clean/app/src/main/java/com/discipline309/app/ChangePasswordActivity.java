package com.discipline309.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ChangePasswordActivity extends Activity {
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int BG=0xFF061126, SURFACE=0xFF0B1B3A, TEXT=0xFFF5F8FF, MUTED=0xFF9CB2D9;

    private TextView label(String s,float size,int color){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(dp(4),dp(6),dp(4),dp(6));
        return v;
    }

    private Button button(String text){
        Button b=new Button(this);
        b.setText(text); b.setTextColor(TEXT); b.setAllCaps(false); b.setTextSize(14);
        b.setMinHeight(dp(50));
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(0xFF102957); g.setCornerRadius(dp(16)); g.setStroke(dp(1),0xFF173D78);
        b.setBackground(g);
        return b;
    }

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        if(!SupabaseAccountManager.loggedIn(this)){
            Toast.makeText(this,"Please sign in first.",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(24),dp(20),dp(28));
        root.setBackgroundColor(BG);

        TextView title=label("🔐  Change Password",27,TEXT);
        title.setTypeface(null,1);
        root.addView(title);
        root.addView(label("Set a new password for your signed-in account. No reset email is required.",13,MUTED));

        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(16),dp(16),dp(16));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(SURFACE); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1),0xFF173D78);
        card.setBackground(bg);

        EditText password=new EditText(this);
        password.setHint("New password");
        password.setTextColor(TEXT); password.setHintTextColor(MUTED);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(password,new LinearLayout.LayoutParams(-1,dp(58)));

        EditText confirm=new EditText(this);
        confirm.setHint("Confirm new password");
        confirm.setTextColor(TEXT); confirm.setHintTextColor(MUTED);
        confirm.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(58));
        cp.topMargin=dp(8); card.addView(confirm,cp);

        Button save=button("Save New Password");
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));
        sp.topMargin=dp(14); card.addView(save,sp);

        TextView note=label("Use a strong password you can remember. Do not share it in chat.",12,MUTED);
        card.addView(note);

        root.addView(card,new LinearLayout.LayoutParams(-1,-2));

        Button back=button("←  Back");
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(50));
        bp.topMargin=dp(14); root.addView(back,bp);
        back.setOnClickListener(v->finish());

        save.setOnClickListener(v->{
            String p=password.getText().toString();
            String c=confirm.getText().toString();
            if(p.length()<6){password.setError("Password must be at least 6 characters.");return;}
            if(!p.equals(c)){confirm.setError("Passwords do not match.");return;}
            save.setEnabled(false);
            SupabaseAccountManager.updatePassword(SupabaseAccountManager.accessToken(this),p,(ok,msg)->
                runOnUiThread(()->{
                    save.setEnabled(true);
                    if(ok){
                        new AlertDialog.Builder(this)
                            .setTitle("Password changed")
                            .setMessage("Your Supabase account password has been updated.")
                            .setPositiveButton("OK",(d,w)->finish())
                            .setCancelable(false).show();
                        password.setText(""); confirm.setText("");
                    }else Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
                }));
        });

        setContentView(root);
    }
}
