package com.discipline309.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class StrictBlockActivity extends Activity{
 private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.setPadding(dp(28),dp(28),dp(28),dp(28));l.setBackgroundColor(Color.rgb(10,14,26));
  TextView a=new TextView(this);a.setText("🔒  STRICT MODE");a.setTextColor(Color.WHITE);a.setTextSize(28);a.setGravity(Gravity.CENTER);l.addView(a);
  TextView m=new TextView(this);m.setText("Maya blocked this app because your Strict Mode limit is active.\n\nGo back to your discipline task instead.");m.setTextColor(Color.LTGRAY);m.setTextSize(17);m.setGravity(Gravity.CENTER);m.setPadding(0,dp(20),0,dp(20));l.addView(m);
  Button back=new Button(this);back.setText("🔥  BACK TO DISCIPLINE");back.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);finish();});l.addView(back);
  setContentView(l);
 }
 @Override public void onBackPressed(){startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}
}