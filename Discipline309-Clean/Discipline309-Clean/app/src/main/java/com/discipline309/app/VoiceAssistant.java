package com.discipline309.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
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
    public boolean isListening() { return listening; }
    private boolean ttsReady = false;
    private String pendingSpeech = "";
    private String pendingAction = "NONE";
    private int recognitionRetryCount = 0;
    private final Handler voiceHandler = new Handler(Looper.getMainLooper());
    private Runnable voiceStateListener;
    public void setVoiceStateListener(Runnable listener) { voiceStateListener = listener; }
    private void notifyVoiceEnded() { try { if (voiceStateListener != null) voiceStateListener.run(); } catch (Exception ignored) {} }

    public VoiceAssistant(Activity activity) {
        this.activity = activity;
        try {
            tts = new TextToSpeech(activity, status -> {
                try {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        String selected=activity.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("maya_language","auto");
                        String country=Locale.getDefault().getCountry();
                        Locale target=("si".equals(selected) || ("auto".equals(selected) && "LK".equalsIgnoreCase(country)))
                                ? new Locale("si","LK") : Locale.ENGLISH;
                        int lang=tts.setLanguage(target);
                        if(lang==TextToSpeech.LANG_MISSING_DATA || lang==TextToSpeech.LANG_NOT_SUPPORTED){
                            tts.setLanguage(Locale.ENGLISH);
                        }
                        selectMayaVoice(target);
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

    public void stopListening() {
        listening=false;
        notifyVoiceEnded();
        recognitionRetryCount=0;
        voiceHandler.removeCallbacksAndMessages(null);
        try { if(recognizer!=null) recognizer.cancel(); } catch(Exception ignored) {}
        try { if(recognizer!=null) recognizer.destroy(); } catch(Exception ignored) {}
        recognizer=null;
    }

    public boolean start() {
        if(SupabaseAccountManager.loggedIn(activity)&&!SupabaseAccountManager.can(activity,"can_use_maya")){
            Toast.makeText(activity,"Maya is disabled by your Primary account.",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 3099);
            Toast.makeText(activity, "Allow microphone access, then tap Maya again.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition is not available on this phone.", Toast.LENGTH_LONG).show();
            return false;
        }

        if (listening) return true;
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
                    if (recognitionRetryCount < 1) {
                        recognitionRetryCount++;
                        voiceHandler.postDelayed(() -> { if (!listening) start(); }, 350);
                    } else {
                        recognitionRetryCount = 0;
                        Toast.makeText(activity, "Maya අහගෙන ඉන්නවා. ආයෙත් කියන්න. 🎙️", Toast.LENGTH_SHORT).show();
                        notifyVoiceEnded();
                    }
                    return;
                }

                // Transient network/provider/recognizer errors are retried instead of
                // immediately showing the generic voice-error message.
                if (e == SpeechRecognizer.ERROR_AUDIO ||
                    e == SpeechRecognizer.ERROR_NETWORK ||
                    e == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                    e == SpeechRecognizer.ERROR_SERVER ||
                    e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    e == SpeechRecognizer.ERROR_CLIENT ||
                    e == SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) {
                    if (recognitionRetryCount < 2) {
                        recognitionRetryCount++;
                        long delay = e == SpeechRecognizer.ERROR_AUDIO ? 900L : (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 900L : 700L);
                        voiceHandler.postDelayed(() -> { if (!listening) start(); }, delay);
                    } else {
                        recognitionRetryCount = 0;
                        Toast.makeText(activity, "Maya voice service එකට connect වෙන්න බැරි වුණා. Microphone + Google voice recognition check කරලා ආයෙත් try කරන්න. 🎙️", Toast.LENGTH_LONG).show();
                        notifyVoiceEnded();
                    }
                    return;
                }

                if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    Toast.makeText(activity, "Mayaට microphone permission එක ඕන. Phone Settings වලින් allow කරන්න. 🎙️", Toast.LENGTH_LONG).show();
                    notifyVoiceEnded();
                    return;
                }

                if (e == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    e == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                    Toast.makeText(activity, "මේ voice language එක phone එකේ available නැහැ. Maya language එක Auto/English කරලා try කරන්න.", Toast.LENGTH_LONG).show();
                    notifyVoiceEnded();
                    return;
                }

                Toast.makeText(activity, "Maya voice recognition error. Microphone එක busy ද, Google voice service එක available ද බලලා ආයෙත් try කරන්න. 🎙️", Toast.LENGTH_LONG).show();
                notifyVoiceEnded();
            }
            public void onResults(Bundle results) {
                listening = false;
                recognitionRetryCount = 0;
                notifyVoiceEnded();
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (matches == null || matches.isEmpty()) ? "" : matches.get(0);
                handle(text);
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // The Maya button already starts the assistant, so speech recognition
        // should listen directly for the user's question in the selected language.
        String selectedLanguage=activity.getSharedPreferences("settings",Context.MODE_PRIVATE)
                .getString("maya_language","auto");
        String country=Locale.getDefault().getCountry();
        boolean sinhala="si".equals(selectedLanguage)
                || ("auto".equals(selectedLanguage) && "LK".equalsIgnoreCase(country));
        String recognitionLocale=sinhala ? "si-LK" : "en-LK";
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLocale);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionLocale);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, sinhala
                ? "ඔයාට Mayaගෙන් අහන්න ඕන දේ කියන්න"
                : "Tell Maya what you need");
        try { recognizer.startListening(intent); }
        catch (Exception e) {
            listening = false;
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
            Toast.makeText(activity, "Maya voice start කරන්න බැරි වුණා.", Toast.LENGTH_SHORT).show();
            notifyVoiceEnded();
            return false;
        }
        } catch (Exception e) {
            listening = false;
            try { if (recognizer != null) recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
            Toast.makeText(activity, "Maya voice එක start කරන්න බැරි වුණා.", Toast.LENGTH_SHORT).show();
            notifyVoiceEnded();
            return false;
        }
        return true;
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
        if(question.equals("maya stop") || question.equals("stop maya") || question.equals("maya pause") || question.equals("live stop")){
            try{
                Intent i=new Intent(activity,MayaAssistantService.class);
                i.setAction(MayaAssistantService.ACTION_STOP_WORKOUT_LIVE);
                if(android.os.Build.VERSION.SDK_INT>=26) activity.startForegroundService(i); else activity.startService(i);
            }catch(Exception ignored){}
            speak("හරි 😄 Maya Live Conversation OFF.");
            return;
        }
        // Auto-complete a Today's Habit only when the user clearly reports completion.
        if (isCompletedActivityStatement(question) && activity instanceof MainActivity) {
            String completed = ((MainActivity) activity).completeActivityFromMaya(question);
            if (completed != null) {
                speak(completed);
                return;
            }
        }

        String action=MayaToolRouter.action(question);
        if (question.contains("live conversation") || question.contains("live mode") || question.contains("live on") ||
                question.contains("live conversation on") || question.contains("workout live") ||
                question.contains("workout කරනකොට maya") || question.contains("workout ekedi maya")) {
            startWorkoutLiveIfEnabled();
            speak("හරි 🔥 Workout Live Conversation ON. Workout කරන ගමන් මට කතා කරන්න.");
            return;
        }
        if (MayaToolRouter.requiresConfirmation(action)) {
            if (isConfirmation(question)) {
                String pending=activity.getSharedPreferences("maya_action",Context.MODE_PRIVATE).getString("pending","NONE");
                if (!"NONE".equals(pending)) {
                    activity.getSharedPreferences("maya_action",Context.MODE_PRIVATE).edit().remove("pending").apply();
                    if ("START_WORKOUT".equals(pending)) {
                        startWorkoutLiveIfEnabled();
                        speak("හරි 🔥 workout එක start කරන්න ready. Home screen එකෙන් workout routine එක open කරලා පටන් ගමු.");
                    } else if ("COMPLETE_TASK".equals(pending)) {
                        speak("හරි ✅ task එක complete කරන්න ready. අද task list එකෙන් complete කරන්න.");
                    }
                    return;
                }
            } else if (!"NONE".equals(action)) {
                activity.getSharedPreferences("maya_action",Context.MODE_PRIVATE).edit().putString("pending",action).apply();
                speak("හරි 😄 "+("START_WORKOUT".equals(action) ? "workout එක start කරන්නද?" : "task එක complete කරන්නද?")+" Yes කියන්න.");
                return;
            }
        }
        String mode=activity.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("maya_mode","motivative");
        if("auto".equals(mode)) mode=resolveAutoMode(question);
        boolean romance="romance".equals(mode);
        boolean caring="caring".equals(mode);
        boolean angry="angry".equals(mode);
        boolean motivative="motivative".equals(mode);
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
            reply = caring ? "ඔව්, මං මෙතන. හෙමින් කියන්න, මං අහගෙන ඉන්නවා. 💛" : romance ? "ඔව්, Maya මෙතන. කියන්න, මං අහගෙන ඉන්නවා. 💗" : angry ? "ඔව්. කියන්න. දැන් target එකට යමු. 😤" : motivative ? "ඔව්! Maya ready 🔥 කියන්න, අද target එක ගමු." : "ඔව්, මං මෙතන. කියන්න, මොකද වෙන්නේ? 😄";
        } else if (question.contains("hello") || question.contains("hi") || question.contains("හෙලෝ")) {
            reply = romance ? "හෙලෝ 😄 අද කොහොමද? මොකද වෙන්නේ කියලා කියන්න." : caring ? "හෙලෝ 💛 අද කොහොමද? දවස කොහොම ගියා?" : angry ? "හෙලෝ 😤 හරි, අද situation එක කියන්න. මොකද වෙන්නේ?" : "හෙලෝ 😄 Maya මෙතන. අද කොහොමද? මොනවාද වෙන්නේ?" ;
        } else if (question.contains("motivat") || question.contains("වැඩ") || question.contains("බැහැ")) {
            reply = angry ? "Excuses නවත්තමු. 😤 දැන් එක target එකක් තෝරගෙන කරමු!" : caring ? "අමාරු දවසක් නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛" : romance ? "හරි 💗 පොඩි step එකකින් පටන් ගමු. Discipline + fitness target එක අතාරින්න එපා." : "එක පොඩි step එකක් දැන්ම කරමු. 🔥";
        } else if (question.contains("sleep") || question.contains("නින්ද")) {
            reply = caring || romance ? "හරි, phone එක පැත්තකින් තියලා හොඳට rest ගන්න. Rest එකත් training එකේ කොටසක්. 💛🌙" : angry ? "හරි. Recovery එකත් training එකේ කොටසක්. 😤🌙" : "Phone එක පැත්තකින් තියලා rest ගන්න. Recovery එක වැදගත්. 🌙";
        } else if (question.contains("thank") || question.contains("ස්තුති")) {
            reply = caring || romance ? "Anytime 💛 දැන් next step එකට යමු." : angry ? "හරි. 😤 දැන් next task එකට." : "Anytime! දැන් next step එක continue කරමු. 🔥";
        } else {
            final String userQuestion = question;
            String memory = activity.getSharedPreferences("maya_memory", 0).getString("items", "[]");
            String liveContext = activity instanceof MainActivity ? ((MainActivity) activity).buildMayaContext() : "Live app state unavailable.";
            String personality = "Maya mode: "+mode+"; primary target: discipline and fitness training";
            MayaAI.ask(activity, userQuestion, liveContext + " Saved ordinary memory: " + memory, personality, aiReply -> new Handler(Looper.getMainLooper()).post(() -> speak(aiReply)));
            return;
        }
        speak(reply);
    }

    private void startWorkoutLiveIfEnabled(){
        try{
            Intent i=new Intent(activity,MayaAssistantService.class);
            i.setAction(MayaAssistantService.ACTION_START_WORKOUT_LIVE);
            if(android.os.Build.VERSION.SDK_INT>=26) activity.startForegroundService(i); else activity.startService(i);
        }catch(Exception ignored){}
    }

    private boolean isConfirmation(String q){
        if(q==null)return false;
        String s=q.toLowerCase(Locale.ROOT).trim();
        return s.equals("yes") || s.equals("yeah") || s.equals("ok") || s.equals("okay")
                || s.equals("confirm") || s.equals("do it") || s.equals("හරි") || s.equals("ඔව්")
                || s.equals("ඔව් කරන්න") || s.equals("කරන්න");
    }

    private String resolveAutoMode(String question){
        String q=question==null?"":question.toLowerCase(Locale.ROOT);
        if(q.contains("tired")||q.contains("sad")||q.contains("බැහැ")||q.contains("අමාරු")||q.contains("stress")||q.contains("stressed")) return "caring";
        if(q.contains("late")||q.contains("lazy")||q.contains("lazy")||q.contains("excuse")||q.contains("වැඩ නැහැ")) return "angry";
        if(q.contains("workout")||q.contains("gym")||q.contains("training")||q.contains("exercise")||q.contains("fitness")||q.contains("workout")) return "motivative";
        if(q.contains("love")||q.contains("romance")||q.contains("romantic")) return "romance";
        return "motivative";
    }

    private void speak(String text) {
        if (tts == null || !ttsReady) { pendingSpeech = text == null ? "" : text; return; }
        android.content.SharedPreferences p=activity.getSharedPreferences("settings",Context.MODE_PRIVATE);
        if(!p.getBoolean("auto_speak",true)) return;
        float rate=.65f+(p.getInt("speech_speed",50)/100f)*.85f;
        try {
        String selectedLanguage=p.getString("maya_language","auto");
        String country=Locale.getDefault().getCountry();
        Locale target=("si".equals(selectedLanguage) || ("auto".equals(selectedLanguage) && "LK".equalsIgnoreCase(country)))
                ? new Locale("si","LK") : Locale.ENGLISH;
        int lang=tts.setLanguage(target);
        if(lang==TextToSpeech.LANG_MISSING_DATA||lang==TextToSpeech.LANG_NOT_SUPPORTED)tts.setLanguage(Locale.ENGLISH);
        selectMayaVoice(target);
        tts.setSpeechRate(rate);
        String safe=naturalSinhala(text);
        if(safe.isEmpty()) return;
        try { tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null, "maya_" + System.currentTimeMillis()); }
        catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void selectMayaVoice(Locale target) {
        if(tts==null)return;
        try{
            int choice=Math.max(0,Math.min(2,activity.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("maya_voice",0)));
            java.util.ArrayList<android.speech.tts.Voice> female=new java.util.ArrayList<>();
            java.util.ArrayList<android.speech.tts.Voice> all=new java.util.ArrayList<>();
            java.util.Set<android.speech.tts.Voice> voices=tts.getVoices();
            if(voices!=null)for(android.speech.tts.Voice voice:voices){
                if(voice==null||voice.getLocale()==null||!voice.getLocale().getLanguage().equals(target.getLanguage()))continue;
                if(voice.isNetworkConnectionRequired())continue;
                all.add(voice);
                String n=voice.getName()==null?"":voice.getName().toLowerCase(Locale.ROOT);
                if(n.contains("female")||n.contains("fem")||n.contains("woman")||n.contains("girl"))female.add(voice);
            }
            java.util.ArrayList<android.speech.tts.Voice> pool=female.size()>=3?female:all;
            if(!pool.isEmpty()){
                java.util.Collections.sort(pool,(a,b)->{
                    int q=Integer.compare(b.getQuality(),a.getQuality());
                    return q!=0?q:a.getName().compareToIgnoreCase(b.getName());
                });
                int index=pool.size()>=3?(choice==0?0:(choice==1?pool.size()/2:pool.size()-1)):Math.min(choice,pool.size()-1);
                tts.setVoice(pool.get(index));
            }
            float[] pitches={1.08f,1.02f,0.98f};
            float[] rates={0.88f,0.94f,1.00f};
            tts.setPitch(pitches[choice]);
            tts.setSpeechRate(rates[choice]);
        }catch(Exception ignored){}
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
        voiceHandler.removeCallbacksAndMessages(null);
        recognitionRetryCount=0;
        try { if (recognizer != null) recognizer.destroy(); } catch (Exception ignored) {}
        recognizer=null;
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
        tts=null;
        ttsReady=false;
        pendingSpeech="";
    }    private boolean isCompletedActivityStatement(String q) {
        if (q == null || q.trim().isEmpty()) return false;
        String s = q.toLowerCase(Locale.ROOT).trim();
        String[] markers = {
                "i did ", "i have done ", "i completed ", "i finished ", "just did ",
                "done ", "finished ", "completed ", "did my ",
                "මම කළා", "මම කලා", "මම කරලා ඉවරයි", "කරලා ඉවරයි",
                "කළා", "කලා", "ඉවරයි", "complete කළා", "complete කලා",
                "finish කළා", "finish කලා"
        };
        for (String marker : markers) if (s.contains(marker)) return true;
        return s.endsWith(" done") || s.endsWith(" finished") || s.endsWith(" completed");
    }


}