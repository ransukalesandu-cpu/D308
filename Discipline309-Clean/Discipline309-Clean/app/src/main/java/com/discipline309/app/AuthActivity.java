package com.discipline309.app;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.Locale;

public class AuthActivity extends Activity {
    private final int BG=0xFF061126, SURFACE=0xFF0B1B3A, TEXT=0xFFF5F8FF, MUTED=0xFF9CB2D9, ACCENT=0xFF2F7BFF;
    private LinearLayout root;
    private EditText email, password, name, invite;
    private Spinner accountType;
    private boolean registerMode=false;
    private static final int MAIN_PERMISSIONS=3090;
    private boolean permissionFlowStarted=false;

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    private TextView text(String s,float size,int color){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(dp(4),dp(5),dp(4),dp(5));
        return v;
    }

    private EditText field(String hint){
        EditText e=new EditText(this);
        e.setHint(hint); e.setTextColor(TEXT); e.setHintTextColor(MUTED);
        e.setSingleLine(true); e.setTextSize(15);
        e.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));
        p.setMargins(0,dp(5),0,dp(5)); e.setLayoutParams(p);
        return e;
    }

    private Button button(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false);
        b.setTextColor(TEXT); b.setTextSize(14); b.setMinHeight(dp(50));
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(0xFF102957); g.setCornerRadius(dp(16)); g.setStroke(dp(1),0xFF173D78);
        b.setBackground(g); return b;
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        if(SupabaseAccountManager.loggedIn(this)){routeAfterAuth();return;}
        show(false);
    }

    private void show(boolean register){
        registerMode=register;
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24),dp(34),dp(24),dp(32)); root.setBackgroundColor(BG);

        TextView logo=text("309",48,TEXT); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        logo.setGravity(Gravity.CENTER); root.addView(logo);
        TextView title=text(register?"Create your account":"Welcome back",25,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView sub=text(register?"Save your discipline progress securely in the cloud.":"Sign in to continue to your 309 Day Discipline.",13,MUTED);
        sub.setGravity(Gravity.CENTER); root.addView(sub);

        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(14));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(SURFACE); bg.setCornerRadius(dp(20)); bg.setStroke(dp(1),0xFF173D78);
        card.setBackground(bg);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,dp(24),0,dp(12));
        card.setLayoutParams(cp);

        email=field("Email address"); email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); card.addView(email);
        password=field("Password (6+ characters)"); password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); card.addView(password);

        if(register){
            name=field("Display name"); card.addView(name);
            accountType=new Spinner(this);
            String[] types={"Primary account","Sub account"};
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types);
            accountType.setAdapter(adapter); card.addView(accountType,new LinearLayout.LayoutParams(-1,dp(52)));
            invite=field("Primary invite code (Sub only)"); invite.setVisibility(View.GONE); card.addView(invite);
            accountType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){invite.setVisibility(pos==1?View.VISIBLE:View.GONE);}
                public void onNothingSelected(android.widget.AdapterView<?> p){}
            });
        }

        Button action=button(register?"✨ Create account":"🔐 Sign in");
        action.setOnClickListener(v->submit());
        card.addView(action);

        if(!register){
            TextView forgot=text("Forgot password?",13,ACCENT);
            forgot.setGravity(Gravity.CENTER); forgot.setPadding(0,dp(10),0,dp(10));
            forgot.setOnClickListener(v->showForgotPassword());
            card.addView(forgot);
        }

        Button switchMode=button(register?"Already have an account? Sign in":"New here? Create an account");
        switchMode.setOnClickListener(v->show(!registerMode)); card.addView(switchMode);

        root.addView(card);
        root.addView(text("🔒 Your password and session are handled through Supabase Auth. API secrets stay on the backend.",11,MUTED));
        sc.addView(root); setContentView(sc);
    }

    private void submit(){
        String em=email.getText().toString().trim();
        String pw=password.getText().toString();
        if(em.isEmpty() || !em.contains("@")){toast("Valid email ekak danna.");return;}
        if(pw.length()<6){toast("Password eka characters 6k wath one.");return;}

        if(!registerMode){
            setBusy(true);
            SupabaseAccountManager.signIn(this,em,pw,(ok,msg)->runOnUiThread(()->{
                setBusy(false); toast(msg);
                if(ok) routeAfterAuth();
            }));
            return;
        }

        String display=name.getText().toString().trim();
        if(display.isEmpty()) display="309 User";
        final boolean isSub=accountType.getSelectedItemPosition()==1;
        final String code=invite.getText().toString().trim().toUpperCase(Locale.US);
        if(isSub && code.length()<6){toast("Sub account ekata Primary invite code eka danna.");return;}

        setBusy(true);
        final String finalDisplay=display;
        SupabaseAccountManager.signUp(this,em,pw,(ok,msg)->runOnUiThread(()->{
            if(!ok){setBusy(false);toast(msg);return;}
            if(!SupabaseAccountManager.loggedIn(this)){
                setBusy(false);
                toast(msg);
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Email verification")
                    .setMessage("Supabase email confirmation on nam, email eka verify karala passe Login wenna.")
                    .setPositiveButton("OK",null).show();
                return;
            }
            if(isSub){
                SupabaseAccountManager.joinInvite(this,code,(joined,jmsg)->runOnUiThread(()->{
                    setBusy(false); toast(jmsg);
                    if(joined) routeAfterAuth();
                }));
            }else{
                SupabaseAccountManager.createPrimaryProfile(this,finalDisplay,(created,cmsg)->runOnUiThread(()->{
                    setBusy(false); toast(cmsg);
                    if(created) routeAfterAuth();
                }));
            }
        }));
    }

    private void showForgotPassword(){
        final EditText resetEmail=field("Email address");
        resetEmail.setText(email==null?"":email.getText().toString().trim());
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(8),dp(20),0);
        box.addView(resetEmail);
        final android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle("Reset password")
            .setMessage("Email address eka danna. Password reset link ekak email ekata yawannam.")
            .setView(box).setNegativeButton("Cancel",null).setPositiveButton("Send link",null).create();
        dialog.setOnShowListener(x->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String em=resetEmail.getText().toString().trim();
            if(em.isEmpty()||!em.contains("@")){toast("Valid email ekak danna.");return;}
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            SupabaseAccountManager.sendPasswordReset(this,em,(ok,msg)->runOnUiThread(()->{
                dialog.dismiss();
                new android.app.AlertDialog.Builder(this).setTitle(ok?"Email sent":"Reset failed").setMessage(msg).setPositiveButton("OK",null).show();
            }));
        }));
        dialog.show();
    }

    private void setBusy(boolean busy){
        if(email!=null) email.setEnabled(!busy);
        if(password!=null) password.setEnabled(!busy);
        if(name!=null) name.setEnabled(!busy);
        if(invite!=null) invite.setEnabled(!busy);
        if(accountType!=null) accountType.setEnabled(!busy);
    }

    private void routeAfterAuth(){
        if(!SupabaseAccountManager.loggedIn(this)){show(false);return;}
        String role=SupabaseAccountManager.role(this);
        if("primary".equals(role)||"sub".equals(role)){
            requestMainAppPermissionsThenOpen();
        }else{
            startActivity(new android.content.Intent(this,AccountsActivity.class));
            finish();
        }
    }

    private void requestMainAppPermissionsThenOpen(){
        if(permissionFlowStarted)return;
        permissionFlowStarted=true;
        if(!SupabaseAccountManager.loggedIn(this)){show(false);return;}
        java.util.ArrayList<String> needed=new java.util.ArrayList<>();
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO);
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA);
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS);

        if(needed.isEmpty()){openMainAfterPermissions();return;}

        boolean firstPermissionPrompt=!getSharedPreferences("settings",MODE_PRIVATE).getBoolean("main_permissions_prompt_shown",false);
        if(!firstPermissionPrompt){
            requestPermissions(needed.toArray(new String[0]),MAIN_PERMISSIONS);
            return;
        }

        getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("main_permissions_prompt_shown",true).apply();
        new android.app.AlertDialog.Builder(this)
            .setTitle("🎙️ Maya needs a few permissions")
            .setMessage("To use Maya voice, background listening, camera tools and important reminders, 309 Day Discipline needs Microphone, Camera and Notification permissions. You can choose Allow or Don't allow in Android's permission screen.")
            .setNegativeButton("Not now",(d,w)->openMainAfterPermissions())
            .setPositiveButton("Continue",(d,w)->requestPermissions(needed.toArray(new String[0]),MAIN_PERMISSIONS))
            .setCancelable(false).show();
    }

    private void openMainAfterPermissions(){
        startActivity(new android.content.Intent(this,MainActivity.class));
        finish();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==MAIN_PERMISSIONS) openMainAfterPermissions();
    }

    @Override protected void onResume(){
        super.onResume();
        if(!registerMode && SupabaseAccountManager.loggedIn(this) && !isFinishing()) routeAfterAuth();
    }

    private void toast(String s){Toast.makeText(this,s==null?"":s,Toast.LENGTH_SHORT).show();}
}
