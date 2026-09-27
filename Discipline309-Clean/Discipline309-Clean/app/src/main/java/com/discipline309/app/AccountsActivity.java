package com.discipline309.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class AccountsActivity extends Activity {
    private int BG=0xFF0B0E14,SURFACE=0xFF191D27,TEXT=0xFFF7F8FC,MUTED=0xFFAAB2C3,ACCENT=0xFF63E6BE;
    private LinearLayout root;
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(12),dp(16),dp(12));android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(SURFACE);g.setCornerRadius(dp(18));l.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));l.setLayoutParams(p);return l;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}

    @Override protected void onCreate(Bundle b){super.onCreate(b);applyTheme();build();}
    private void applyTheme(){
        String t=getSharedPreferences("ui_settings",MODE_PRIVATE).getString("theme","midnight");
        if("neon".equals(t)){BG=0xFF05050A;SURFACE=0xFF101525;TEXT=0xFFFFFFFF;MUTED=0xFF9CA8C7;ACCENT=0xFF00E5FF;}
        else if("soft".equals(t)){BG=0xFFF6F3F8;SURFACE=0xFFFFFFFF;TEXT=0xFF25222B;MUTED=0xFF77727F;ACCENT=0xFFB56CFF;}
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
    }
    private void build(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackgroundColor(BG);
        root.addView(label("👥  Accounts",27,TEXT));root.addView(label("Primary + Sub accounts • cloud progress sync",13,MUTED));
        if(!SupabaseAccountManager.loggedIn(this))showAuth();
        else showDashboard();
        sc.addView(root);setContentView(sc);
    }
    private void showAuth(){
        LinearLayout c=card();c.addView(label("☁️ CLOUD ACCOUNT",11,MUTED));
        c.addView(label("Sign in to sync your 309 progress between phones. Use an email + password.",13,TEXT));
        EditText email=new EditText(this);email.setHint("Email");email.setSingleLine(true);email.setInputType(33);c.addView(email);
        EditText pass=new EditText(this);pass.setHint("Password");pass.setSingleLine(true);pass.setInputType(129);c.addView(pass);
        Button login=button("🔐 Sign in");login.setOnClickListener(v->{
            if(email.getText().toString().trim().isEmpty()||pass.getText().toString().isEmpty()){toast("Enter email and password.");return;}
            SupabaseAccountManager.signIn(this,email.getText().toString().trim(),pass.getText().toString(),(ok,msg)->runOnUiThread(()->{toast(msg);if(ok)build();}));
        });c.addView(login);
        Button signup=button("✨ Create account");signup.setOnClickListener(v->{
            if(email.getText().toString().trim().isEmpty()||pass.getText().toString().length()<6){toast("Use an email and a password with at least 6 characters.");return;}
            SupabaseAccountManager.signUp(this,email.getText().toString().trim(),pass.getText().toString(),(ok,msg)->runOnUiThread(()->toast(msg)));
        });c.addView(signup);
        c.addView(label("After creating an account, sign in. If email confirmation is enabled, verify the email first.",11,MUTED));
        root.addView(c);
    }
    private void showDashboard(){
        String role=SupabaseAccountManager.role(this);
        LinearLayout me=card();me.addView(label("YOUR ACCOUNT",11,MUTED));
        me.addView(label("👤 "+(SupabaseAccountManager.displayName(this).isEmpty()?"309 User":SupabaseAccountManager.displayName(this)),19,TEXT));
        me.addView(label("Role: "+(role.isEmpty()?"not set":role.toUpperCase(Locale.US)),13,ACCENT));
        if(role.isEmpty()){
            Button primary=button("👑 Make this a Primary account");primary.setOnClickListener(v->{
                EditText name=new EditText(this);name.setHint("Your name");new AlertDialog.Builder(this).setTitle("Create Primary profile").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->SupabaseAccountManager.createPrimaryProfile(this,name.getText().toString(),(ok,msg)->runOnUiThread(()->{toast(msg);if(ok)build();}))).show();
            });me.addView(primary);
        }
        Button sync=button("☁️ Sync my progress now");sync.setOnClickListener(v->SupabaseAccountManager.syncLocalProgress(this,(ok,msg)->runOnUiThread(()->toast(msg))));me.addView(sync);
        Button logout=button("↪ Sign out");logout.setOnClickListener(v->{SupabaseAccountManager.signOut(this);build();});me.addView(logout);
        root.addView(me);
        if("primary".equals(role))showPrimary();
        else if("sub".equals(role))showSub();
        else showJoin();
    }
    private void showPrimary(){
        LinearLayout c=card();c.addView(label("👑 PRIMARY DASHBOARD",11,MUTED));
        c.addView(label("Create an invite code and see linked members' live cloud progress.",12,MUTED));
        Button invite=button("➕ Create Sub Account invite");invite.setOnClickListener(v->SupabaseAccountManager.createInvite(this,(ok,msg)->runOnUiThread(()->{
            if(ok)new AlertDialog.Builder(this).setTitle("Invite code").setMessage(msg+"\n\nGive this code to your brother. It expires in 7 days and can be used once.").setPositiveButton("OK",null).show();else toast(msg);
        })));c.addView(invite);
        root.addView(c);
        LinearLayout list=card();list.addView(label("SUB ACCOUNTS",11,MUTED));list.addView(label("Loading…",12,MUTED));root.addView(list);
        SupabaseAccountManager.loadLinked(this,(ok,msg)->runOnUiThread(()->{
            list.removeAllViews();list.addView(label("SUB ACCOUNTS",11,MUTED));
            if(!ok){list.addView(label(msg,12,MUTED));return;}
            try{
                JSONArray a=new JSONArray(msg);
                if(a.length()==0){list.addView(label("No linked sub accounts yet. Create an invite above.",13,MUTED));return;}
                for(int i=0;i<a.length();i++)addSubCard(list,a.getJSONObject(i));
            }catch(Exception e){list.addView(label("Could not read progress.",12,MUTED));}
        }));
    }
    private void addSubCard(LinearLayout list,JSONObject x)throws Exception{
        String name=x.optString("display_name","Sub Account");
        LinearLayout c=card();c.setPadding(dp(14),dp(10),dp(14),dp(10));
        c.addView(label("👤 "+name,18,TEXT));
        JSONObject s=x.optJSONObject("snapshot");
        if(s==null)c.addView(label("No progress synced yet.",12,MUTED));
        else{
            int day=s.optInt("day",0),xp=s.optInt("xp",0),streak=s.optInt("currentStreak",0),best=s.optInt("bestStreak",0),done=s.optInt("completedDays",0);
            c.addView(label("Day "+day+"/309  •  "+done+" completed days",13,ACCENT));
            c.addView(label("XP "+xp+"  •  Level "+s.optInt("level",1)+"  •  Current streak "+streak+"  •  Best "+best,12,TEXT));
            c.addView(label("Today: "+s.optInt("todayTasks",0)+"/"+s.optInt("totalTasks",0)+" tasks",12,MUTED));
            if(!s.optString("todayMission","").isEmpty())c.addView(label("Mission: "+s.optString("todayMission"),12,MUTED));
            c.addView(label("Last sync: "+x.optString("updated_at","unknown"),10,MUTED));
            addPermissionSwitch(c,x,"can_view_progress","View progress");
            addPermissionSwitch(c,x,"can_edit_habits","Edit habits");
            addPermissionSwitch(c,x,"can_edit_mission","Edit mission");
            addPermissionSwitch(c,x,"can_reset_progress","Reset progress");
            addPermissionSwitch(c,x,"can_use_maya","Use Maya");
            addPermissionSwitch(c,x,"can_access_settings","Access settings");
            addPermissionSwitch(c,x,"can_sync_progress","Cloud sync");
            addPermissionSwitch(c,x,"can_manage_account","Manage account");
        }
        list.addView(c);
    }
    private void addPermissionSwitch(LinearLayout card,JSONObject x,String key,String title){
        JSONObject perms=x.optJSONObject("permissions");
        boolean checked=perms==null||perms.optBoolean(key,true);
        Switch sw=new Switch(this);sw.setText(title);sw.setTextColor(TEXT);sw.setChecked(checked);
        sw.setOnCheckedChangeListener((b,v)->SupabaseAccountManager.setPermission(this,x.optString("id"),key,v,(ok,msg)->runOnUiThread(()->{
            if(!ok){sw.setChecked(!v);toast(msg);}else toast(title+": "+(v?"ON":"OFF"));}
        )));
        card.addView(sw);
    }
    private void showSub(){
        LinearLayout c=card();c.addView(label("🔗 SUB ACCOUNT",11,MUTED));c.addView(label("Your progress syncs to your Primary account.",13,TEXT));
        String parent= getSharedPreferences("supabase_account",MODE_PRIVATE).getString("parent_id","");
        c.addView(label(parent.isEmpty()?"Linked Primary not available.":"Linked Primary: "+parent,11,MUTED));root.addView(c);
    }
    private void showJoin(){
        LinearLayout c=card();c.addView(label("🔗 JOIN A PRIMARY",11,MUTED));c.addView(label("If this account is for a brother/sub user, enter the invite code from the Primary account.",12,TEXT));
        EditText code=new EditText(this);code.setHint("8-character invite code");code.setSingleLine(true);c.addView(code);
        Button join=button("🔗 Join Primary");join.setOnClickListener(v->{String s=code.getText().toString().trim();if(s.length()<6){toast("Enter the invite code.");return;}SupabaseAccountManager.joinInvite(this,s,(ok,msg)->runOnUiThread(()->{toast(msg);if(ok)build();}));});c.addView(join);root.addView(c);
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
