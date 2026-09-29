package com.discipline309.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

public class VoiceAssistant {
    public static final String ASSISTANT_NAME = "Maya";
    private final Activity activity;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean listening = false;
    private boolean ttsReady = false;
    private String pendingSpeech = "";

    public VoiceAssistant(Activity activity) {
        this.activity = activity;
        try {
            tts = new TextToSpeech(activity, status -> {
                try {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        Locale si = new Locale("si", "LK");
                        int lang = tts.setLanguage(si);
                        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts.setLanguage(Locale.ENGLISH);
                        }
                        tts.setSpeechRate(.92f);
                        ttsReady = true;
                        if (!pendingSpeech.isEmpty()) {
                            String queued = pendingSpeech;
                            pendingSpeech = "";
                            speak(queued);
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) { tts = null; }
    }

    public void start() {
        if(SupabaseAccountManager.loggedIn(activity)&&!SupabaseAccountManager.can(activity,"can_use_maya")){
            Toast.makeText(activity,"Maya is disabled by your Primary account.",Toast.LENGTH_SHORT).show();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3099);
            Toast.makeText(activity, "Allow microphone access, then tap Maya again.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition is not available on this phone.", Toast.LENGTH_LONG).show();
            return;
        }

        if (listening) return;
        listening = true;
        Toast.makeText(activity, "🎙️ කියන්න… Maya අහගෙන ඉන්නවා.", Toast.LENGTH_SHORT).show();

        try {
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(Bundle p) {}
            public void onEvent(int t, Bundle p) {}
            public void onError(int e) {
                listening = false;
                if (e == SpeechRecognizer.ERROR_NO_MATCH ||
                    e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(activity, "Maya අහගෙන ඉන්නවා. ආයෙත් කියන්න. 🎙️", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(activity, "Maya voice එකට පොඩි issue එකක්. ආයෙත් try කරන්න. 🎙️", Toast.LENGTH_SHORT).show();
            }
            public void onResults(Bundle results) {
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (matches == null || matches.isEmpty()) ? "" : matches.get(0);
                handle(text);
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "si-LK");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Maya, then your question");
        try { recognizer.startListening(intent); }
        catch (Exception e) {
            listening = false;
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
            Toast.makeText(activity, "Maya voice start කරන්න බැරි වුණා.", Toast.LENGTH_SHORT).show();
        }
        } catch (Exception e) {
            listening = false;
            try { if (recognizer != null) recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
            Toast.makeText(activity, "Maya voice එක start කරන්න බැරි වුණා.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handle(String spoken) {
        String q = spoken == null ? "" : spoken.trim();
        String lower = q.toLowerCase(Locale.ROOT);

        // This class is already invoked by the "Talk to Maya" button,
        // so do not require the user to say "Maya" a second time.
        // Remove the wake word only when it was included.
        String question = lower
                .replace("maya", "")
                .replace("මායා", "")
                .replace("මායෝ", "")
                .replace("මයා", "")
                .trim();
        String reply;
        boolean funny=activity.getSharedPreferences("settings",0).getBoolean("mode_funny",true);
        boolean cute=activity.getSharedPreferences("settings",0).getBoolean("mode_cute",false);
        boolean sweet=activity.getSharedPreferences("settings",0).getBoolean("mode_sweet",false);
        if (activity instanceof MainActivity) {
            MainActivity m=(MainActivity)activity;
            if (question.contains("අද mission") || question.contains("mission එක මොකක්ද") || question.contains("mission eka mokakda") || question.contains("ada mission")) {
                speak("හරි 😄 "+m.mayaQuickStatus("mission")); return;
            }
            if (question.contains("xp කීයද") || question.contains("xp kiyadha") || question.contains("මගේ xp") || question.contains("මගේ ලෙවල්") || question.contains("level kiyadha")) {
                speak("හරි 😄 "+m.mayaQuickStatus("xp")); return;
            }
            if (question.contains("මගේ streak") || question.contains("streak එක කීයද") || question.contains("streak kiyadha") || question.contains("best streak")) {
                speak("ඔන්න 🔥 "+m.mayaQuickStatus("streak")); return;
            }
            if (question.contains("මම කීවෙනි දවසේද") || question.contains("අද කීවෙනි දවසද") || question.contains("day kiyadha") || question.contains("දවස කීයද")) {
                speak("හරි 📅 "+m.mayaQuickStatus("day")); return;
            }
            if (question.contains("මං කොහොමද") || question.contains("මම කොහොමද යන්නේ") || question.contains("progress eka kohomada") || question.contains("මගේ progress")) {
                speak("හරි 😄 "+m.mayaQuickStatus("progress")); return;
            }
            if (question.contains("අද මොන tasks") || question.contains("ada mona tasks") || question.contains("today tasks") || question.contains("tasks monawada")) {
                speak("හරි 😄 "+m.mayaQuickStatus("progress")); return;
            }
            if (question.contains("මට motivate") || question.contains("mata motivate") || question.contains("motivate me") || question.contains("මාව motivate")) {
                speak("හරි 🔥 එක පොඩි task එකක් දැන්ම පටන් ගමු. අද excuses වලට නිවාඩු! 😄"); return;
            }
            if (question.contains("achievements") || question.contains("achievement") || question.contains("ජයග්‍රහණ")) {
                speak("ඔයාගේ achievements බලන්න Stats එකේ Achievements open කරන්න. 🏆"); return;
            }
        }
        if (question.contains("reset mission") || question.contains("mission reset") || question.contains("mission eka reset") || question.contains("mission eka ain")) {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).resetTodayMissionFromMaya();
                speak("හරි 😄 අද mission එක reset කළා.");
            }
            return;
        } else if (question.contains("change mission") || question.contains("edit mission") || question.contains("mission change") || question.contains("mission eka wenas") || question.contains("mission eka venas") || question.contains("mission eka edit") || question.contains("mission eka hadanna")) {
            String requested = question;
            String[] markers={"change mission to","edit mission to","mission change to","mission eka wenas karanna","mission eka venas karanna","mission eka edit karanna","mission eka hadanna"};
            for(String marker:markers){int at=requested.indexOf(marker);if(at>=0){requested=requested.substring(at+marker.length()).trim();break;}}
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).editTodayMissionFromMaya(requested);
                speak(requested.isEmpty()?"හරි 😄 mission එක edit කරන්න box එක open කළා.":"හරි 😄 අද mission එක වෙනස් කරන්න box එක open කළා.");
            }
            return;
        } else if (question.isEmpty()) {
            reply = sweet ? "ඔව්, මං මෙතන. හෙමින් කියන්න, මං අහගෙන ඉන්නවා. 💛" : cute ? "ඔව්ව් 😄✨ Maya මෙතන! කියන්නකෝ." : "ඔව්, මං මෙතන. කියන්න, මොකද වෙන්නේ? 😄";
        } else if (question.contains("hello") || question.contains("hi") || question.contains("හෙලෝ")) {
            reply = funny ? "හෙලෝ! Maya online 😄 අද වැඩේ පටන් ගමුද, නැත්නම් excuses factory එක open කරමුද? 😂🔥" : cute ? "හෙලෝ! 🌸 Maya මෙතන. අදත් පොඩි step එකකින් පටන් ගමුද? ✨" : "හෙලෝ! මං Maya. අද වැඩේ පටන් ගමුද? 🔥";
        } else if (question.contains("motivat") || question.contains("වැඩ") || question.contains("බැහැ")) {
            reply = funny ? "Excuses වලට අද නිවාඩු 😂 පොඩි step එකක් දැන්ම කරමු! 🔥" : sweet ? "හරි, අමාරු දවසක් නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛" : cute ? "අපි පොඩි step එකක් කරමුකෝ 🌸✨ ඔයාට මේක පුළුවන්!" : "Excuses පස්සේ. පොඩි step එකක් දැන්ම කරමු. ඔයාට මේක පුළුවන්! 🔥";
        } else if (question.contains("sleep") || question.contains("නින්ද")) {
            reply = funny ? "Phone එකටත් දැන් bedtime 😂 පැත්තකින් තියලා rest ගන්න. 🌙" : sweet ? "හරි, phone එක පැත්තකින් තියලා හොඳට rest ගන්න. ඔයාට rest එකත් වැදගත්. 💛🌙" : "හරි, phone එක පැත්තකින් තියලා හොඳට rest ගන්න. 🌙";
        } else if (question.contains("thank") || question.contains("ස්තුති")) {
            reply = cute ? "Anytimeee 😄✨ දැන් අපේ next little step එකට යමු!" : "Anytime! දැන් වැඩේ continue කරමු. 😄🔥";
        } else {
            final String userQuestion = question;
            String memory = activity.getSharedPreferences("maya_memory", 0).getString("items", "[]");
            String liveContext = activity instanceof MainActivity ? ((MainActivity) activity).buildMayaContext() : "Live app state unavailable.";
            String personality = sweet ? "sweet/caring" : cute ? "cute" : funny ? "funny" : "normal";
            MayaAI.ask(activity, userQuestion, liveContext + " Saved ordinary memory: " + memory, personality, aiReply -> new Handler(Looper.getMainLooper()).post(() -> speak(aiReply)));
            return;
        }
        speak(reply);
    }

    private void speak(String text) {
        if (tts == null || !ttsReady) { pendingSpeech = text == null ? "" : text; return; }
        android.content.SharedPreferences p=activity.getSharedPreferences("settings",0);
        if(!p.getBoolean("auto_speak",true)) return;
        float rate=.65f+(p.getInt("speech_speed",50)/100f)*.85f;
        try {
        int lang=tts.setLanguage(new Locale("si","LK"));
        if(lang==TextToSpeech.LANG_MISSING_DATA||lang==TextToSpeech.LANG_NOT_SUPPORTED)tts.setLanguage(Locale.ENGLISH);
        tts.setSpeechRate(rate);
        String safe=naturalSinhala(text);
        if(safe.isEmpty()) return;
        try { tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null, "maya_" + System.currentTimeMillis()); }
        catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private String naturalSinhala(String text) {
        if(text==null || text.trim().isEmpty()) return "හරි 😄 කියන්න, මං අහගෙන ඉන්නවා.";
        String s=text.trim();
        s=s.replace("Today's mission:", "අද mission එක:");
        s=s.replace("You have ", "ඔයාට දැනට ");
        s=s.replace(" XP, Level ", " XP තියෙනවා, Level ");
        s=s.replace("Current streak: ", "දැනට streak එක ");
        s=s.replace(" days. Best streak: ", " දවස්. හොඳම streak එක ");
        s=s.replace("You're on Day ", "ඔයා දැන් Day ");
        s=s.replace(" of 309. ", " / 309. ");
        s=s.replace(" days remaining.", " දවස් ඉතුරුයි.");
        return s;
    }

    public void destroy() {
        listening=false;
        try { if (recognizer != null) recognizer.destroy(); } catch (Exception ignored) {}
        recognizer=null;
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
        tts=null;
        ttsReady=false;
        pendingSpeech="";
    }
}