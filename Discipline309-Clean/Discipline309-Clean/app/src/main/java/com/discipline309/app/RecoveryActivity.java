package com.discipline309.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;

public class RecoveryActivity extends Activity {
    private final int BG=0xFF061126, TEXT=0xFFF5F8FF, MUTED=0xFF9CB2D9;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(4),dp(6),dp(4),dp(6));return v;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);e.setPadding(dp(14),dp(8),dp(14),dp(8));return e;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);show();}
    private void show(){
        String fragment=getIntent().getData()==null?"":getIntent().getData().getFragment();
        String access=parse(fragment,"access_token");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(24),dp(60),dp(24),dp(30));root.setBackgroundColor(BG);
        TextView title=text("Reset password",26,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);root.addView(text("New password eka set karanna.",14,MUTED));
        EditText pass=field("New password (6+ characters)"),confirm=field("Confirm new password");root.addView(pass,new LinearLayout.LayoutParams(-1,dp(56)));root.addView(confirm,new LinearLayout.LayoutParams(-1,dp(56)));
        Button update=new Button(this);update.setText("Update password");update.setAllCaps(false);root.addView(update,new LinearLayout.LayoutParams(-1,dp(54)));setContentView(root);
        if(access.isEmpty()){update.setEnabled(false);new android.app.AlertDialog.Builder(this).setTitle("Invalid reset link").setMessage("Reset link eka expired hari invalid. Login page eken aluth reset link ekak request karanna.").setPositiveButton("OK",(d,w)->finish()).show();return;}
        update.setOnClickListener(v->{String p=pass.getText().toString(),c=confirm.getText().toString();if(p.length()<6){toast("Password eka characters 6k wath one.");return;}if(!p.equals(c)){toast("Passwords dekama same wenna one.");return;}update.setEnabled(false);SupabaseAccountManager.updatePassword(access,p,(ok,msg)->runOnUiThread(()->{if(ok){new android.app.AlertDialog.Builder(this).setTitle("Password updated").setMessage("Aluth password eka successfully set kala. Dan login wenna puluwan.").setPositiveButton("Login",(d,w)->{startActivity(new android.content.Intent(this,AuthActivity.class));finish();}).show();}else{update.setEnabled(true);toast(msg);}}));});
    }
    private String parse(String fragment,String key){if(fragment==null)return "";for(String part:fragment.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&key.equals(kv[0]))try{return java.net.URLDecoder.decode(kv[1],"UTF-8");}catch(Exception ignored){}}return "";}
    private void toast(String s){Toast.makeText(this,s==null?"":s,Toast.LENGTH_SHORT).show();}
}
