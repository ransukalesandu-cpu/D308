package com.discipline309.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.CalendarContract;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MayaAssistantService extends Service {
    public static final String ACTION_PAUSE_LIVE_MIC = "com.discipline309.app.PAUSE_LIVE_MIC";
    public static final String ACTION_RESUME_LIVE_MIC = "com.discipline309.app.RESUME_LIVE_MIC";
    public static final String ACTION_START_WORKOUT_LIVE = "com.discipline309.app.START_WORKOUT_LIVE";
    public static final String ACTION_STOP_WORKOUT_LIVE = "com.discipline309.app.STOP_WORKOUT_LIVE";
    // Wake-word architecture:
    // This service currently uses Android SpeechRecognizer for command capture.
    // A provider-independent WakeWordEngine hook lets us add Porcupine/openWakeWord
    // later without changing the command-routing code.
    private boolean wakeWordEnabled = true;
    private boolean wakeWordDetected = false;
    private boolean realWakeWordActive = false;
    private boolean conversationMode = false;
    private boolean workoutLiveMode = false;
    private OpenWakeWordAdapter wakeWordAdapter;

    private static final int ID=3099;
    private SpeechRecognizer recognizer;
    // ChatGPT-style barge-in uses one lightweight AudioRecord VAD while TTS is speaking.
    // It never creates a second SpeechRecognizer, avoiding the previous microphone-busy race.
    private AudioRecord bargeInAudio;
    private Thread bargeInThread;
    private volatile boolean bargeInListening=false;
    private volatile boolean bargeInStopRequested=false;
    private long ttsStartedAt=0L;
    private long lastBargeInAt=0L;
    private long microphoneReleaseAt=0L;
    private static final long MIC_RELEASE_GUARD_MS=900L;
    private TextToSpeech tts;
    private boolean ready=false, stopping=false;
    private Handler handler;
    private MayaMemory memory;
    private int speechErrorCount=0;
    private boolean listening=false;
private boolean fallbackListening=false;
    private boolean ttsSpeaking=false;
    private boolean pendingWakeWordResponse=false;
    private boolean pendingAssistantInvocation=false;
    private String lastSpokenText="";
    private long lastSpokenAt=0L;
    private static final long CONVERSATION_SILENCE_MS=5000L;
    private final Runnable conversationSilenceRunnable=new Runnable(){
        @Override public void run(){
            if(conversationMode && !workoutLiveMode && !stopping && !listening && !ttsSpeaking){
                endConversationMode();
            }
        }
    };
    private final BroadcastReceiver screenStateReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                endConversationMode();
            }
        }
    };
    // Lightweight in-session context for short follow-up replies.
    private String lastUserQuery="";
    private String lastMayaReply="";

    @Override public void onCreate(){
        super.onCreate();
        if(!mayaAllowed()){ stopSelf(); return; }
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ stopSelf(); return; }
        try{
        memory=new MayaMemory(this);
        createChannel();
        Intent open=new Intent(this,SettingsActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"maya_assistant")
            .setContentTitle("Maya is active")
            .setContentText("Waiting for the Maya wake word • tap to manage")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).setContentIntent(pi).build();
        startForeground(ID,n);
        handler=new Handler(Looper.getMainLooper());
        try{
            IntentFilter screenFilter=new IntentFilter();
            screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(screenStateReceiver,screenFilter);
        }catch(Exception ignored){}
        tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){
            String selected=getSharedPreferences("settings",MODE_PRIVATE).getString("maya_language","auto");
            String country=Locale.getDefault().getCountry();
            Locale target=("si".equals(selected) || ("auto".equals(selected) && "LK".equalsIgnoreCase(country)))
                    ? new Locale("si","LK") : Locale.ENGLISH;
            int lang=tts.setLanguage(target);
            if(lang==TextToSpeech.LANG_MISSING_DATA || lang==TextToSpeech.LANG_NOT_SUPPORTED){ tts.setLanguage(Locale.ENGLISH); }
            tts.setSpeechRate(.94f); tts.setPitch(1.02f); ready=true; if(pendingWakeWordResponse && !stopping){ pendingWakeWordResponse=false; handler.post(this::respondToWakeWord); } if(pendingAssistantInvocation && !stopping){ pendingAssistantInvocation=false; handler.post(this::handleAssistantInvocation); }}});
        handler.postDelayed(wakeWordRunnable,1200);\n        // Background Maya auto-talk is opt-in through Settings and uses sparse check-ins.\n        scheduleProactiveCheckIn();
        // Battery saving: do not start proactive background speech. Maya waits for the wake word.
        }catch(Exception e){
            ready=false;
            stopping=true;
            try{ if(handler!=null) handler.removeCallbacksAndMessages(null); }catch(Exception ignored){}
            try{ if(recognizer!=null) recognizer.destroy(); }catch(Exception ignored){}
            try{ if(wakeWordAdapter!=null) wakeWordAdapter.stop(); }catch(Exception ignored){}
            try{ if(tts!=null){ tts.stop(); tts.shutdown(); } }catch(Exception ignored){}
            stopSelf();
        }
    }

    private void startWorkoutLiveConversation(){
        if(stopping || !mayaAllowed()) return;
        workoutLiveMode=true;
        conversationMode=true;
        wakeWordDetected=false;
        realWakeWordActive=false;
        try{ if(wakeWordAdapter!=null) wakeWordAdapter.stop(); }catch(Exception ignored){}
        if(handler!=null){ handler.removeCallbacks(wakeWordRunnable); handler.removeCallbacks(conversationSilenceRunnable); }
        speak("හරි 🔥 Workout Live Conversation ON. Workout කරන ගමන් මට කතා කරන්න. ඕන වෙලාවක 'Maya stop' කියන්න.");
    }

    private void stopWorkoutLiveConversation(){
        workoutLiveMode=false;
        endConversationMode();
        speak("හරි 😄 Workout Live Conversation OFF.");
    }

    private void pauseForLiveButton(){
        try{
            if(handler!=null){ handler.removeCallbacks(wakeWordRunnable); handler.removeCallbacks(listenRunnable); handler.removeCallbacks(conversationSilenceRunnable); }
            conversationMode=false; workoutLiveMode=false; realWakeWordActive=false; wakeWordDetected=false; fallbackListening=false; listening=false;
            if(recognizer!=null){ try{recognizer.cancel();}catch(Exception ignored){} try{recognizer.destroy();}catch(Exception ignored){} recognizer=null; }
            if(wakeWordAdapter!=null){ try{wakeWordAdapter.stop();}catch(Exception ignored){} wakeWordAdapter=null; }
            if(tts!=null && ttsSpeaking){ try{tts.stop();}catch(Exception ignored){} ttsSpeaking=false; }
        }catch(Exception ignored){}
    }
    private void resumeAfterLiveButton(){
        if(stopping || !mayaAllowed() || handler==null)return;
        handler.removeCallbacks(wakeWordRunnable); handler.postDelayed(wakeWordRunnable,700L);
    }

    private boolean mayaAllowed(){
        return !SupabaseAccountManager.loggedIn(this) || SupabaseAccountManager.can(this,"can_use_maya");
    }

    private static final long PROACTIVE_INTERVAL_MS=2L*60L*60L*1000L;
    private final Object PROACTIVE_TOKEN=new Object();

    private void scheduleProactiveCheckIn(){
        if(stopping || handler==null) return;
        handler.removeCallbacksAndMessages(PROACTIVE_TOKEN);
        handler.postDelayed(() -> {
            if(!stopping && mayaAllowed() && ready && getSharedPreferences("settings",MODE_PRIVATE).getBoolean("auto_speak",true)){
                if(!listening && !ttsSpeaking){
                    String suggestion=MayaPredictiveActions.nextSuggestion(this);
                    if(suggestion==null||suggestion.trim().isEmpty()){scheduleProactiveCheckIn();return;}
                    SharedPreferences p=getSharedPreferences("maya_proactive",MODE_PRIVATE);
                    long now=System.currentTimeMillis();
                    long last=p.getLong("last_spoken_at",0L);
                    String lastText=p.getString("last_text","");
                    if(!suggestion.equals(lastText) || now-last>=6L*60L*60L*1000L){
                        speak(suggestion);
                        p.edit().putLong("last_spoken_at",now).putString("last_text",suggestion).apply();
                    }
                }
            }
            scheduleProactiveCheckIn();
        }, PROACTIVE_INTERVAL_MS);
    }

    private void startWakeWord(){
        if(stopping || !wakeWordEnabled || !mayaAllowed()){ if(!mayaAllowed()) stopSelf(); return; }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            speak("Microphone permission එක allow කරන්න.");
            return;
        }
        try{
            wakeWordAdapter=new OpenWakeWordAdapter(
                this,
                () -> { handler.post(this::onMayaWakeWord); return kotlin.Unit.INSTANCE; },
                error -> { handler.post(() -> {
                    realWakeWordActive=false;
                    fallbackListening=false;
                    // Do not fall back to always-on SpeechRecognizer when the wake-word
                    // engine fails. Retry the wake-word engine instead to avoid
                    // unintended continuous microphone listening.
                    if(!stopping && mayaAllowed()) handler.postDelayed(wakeWordRunnable,5000);
                }); return kotlin.Unit.INSTANCE; }
            );
            wakeWordAdapter.start();
        }catch(Exception e){
            realWakeWordActive=false;
            fallbackListening=false;
            if(!stopping && mayaAllowed()) handler.postDelayed(wakeWordRunnable,5000);
        }
    }

    private void onMayaWakeWord(){
        if(stopping || !mayaAllowed()){ stopSelf(); return; }
        // Ignore duplicate detections while Maya is already responding/listening.
        if (conversationMode || ttsSpeaking || listening) return;
        wakeWordDetected=true;
        realWakeWordActive=true;
        conversationMode=true;
        if(wakeWordAdapter!=null){ try{ wakeWordAdapter.stop(); }catch(Exception ignored){} }
        // Give the wake-word engine a moment to release AudioRecord before SpeechRecognizer starts.
        if(handler!=null) handler.removeCallbacks(listenRunnable);
        // The wake word can arrive before TextToSpeech finishes initializing.
        // Queue the response instead of silently dropping it.
        if(!ready){
            pendingWakeWordResponse=true;
            return;
        }
        respondToWakeWord();
    }

    private void respondToWakeWord(){
        if(stopping || !mayaAllowed() || !conversationMode) return;
        speak(modeReply("ඔව්, කියන්න.","Yoo 😄 කියන්න, Maya online!","ඔව්, කියන්න. 💛"));
    }

    private void listen(){
        if(stopping||!ready||listening)return;
        long wait=Math.max(0L,microphoneReleaseAt-System.currentTimeMillis());
        if(wait>0L){
            restart(wait);
            return;
        }
        if(stopping || !ready || !mayaAllowed() || !SpeechRecognizer.isRecognitionAvailable(this)){ if(!mayaAllowed()) stopSelf(); return; }
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){speak("Microphone permission එක allow කරන්න.");return;}
        if(listening) return;
        try{
        if(recognizer!=null){
            try{ recognizer.cancel(); }catch(Exception ignored){}
            try{ recognizer.destroy(); }catch(Exception ignored){}
            recognizer=null;
            microphoneReleaseAt=System.currentTimeMillis()+MIC_RELEASE_GUARD_MS;
        }
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        }catch(Exception e){
            listening=false;
            restart(1500);
            return;
        }
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){
                listening=true;
                speechErrorCount=0;
                if(conversationMode && handler!=null){
                    handler.removeCallbacks(conversationSilenceRunnable);
                    handler.postDelayed(conversationSilenceRunnable,CONVERSATION_SILENCE_MS);
                }
            }
            public void onBeginningOfSpeech(){
                if(conversationMode && handler!=null) handler.removeCallbacks(conversationSilenceRunnable);
            }
            public void onRmsChanged(float r){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){listening=false;}
            public void onPartialResults(Bundle b){}
            public void onEvent(int t,Bundle b){}
            public void onError(int e){
                listening=false;
                if(conversationMode && handler!=null){
                    handler.removeCallbacks(conversationSilenceRunnable);
                    handler.postDelayed(conversationSilenceRunnable,CONVERSATION_SILENCE_MS);
                }
                speechErrorCount++;
                // Android speech errors such as network timeout, temporary server
                // failures and recognizer-busy states are transient. Recreate the
                // recognizer with bounded backoff instead of leaving Maya stuck.
                long delay=Math.min(5000,700L*(1L<<Math.min(3,speechErrorCount-1)));
                if(e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY || e==SpeechRecognizer.ERROR_TOO_MANY_REQUESTS){
                    delay=Math.max(delay,2800L);
                    microphoneReleaseAt=System.currentTimeMillis()+2200L;
                }
                if(e==SpeechRecognizer.ERROR_AUDIO){
                    delay=Math.max(delay,1800L);
                    microphoneReleaseAt=System.currentTimeMillis()+1400L;
                }
                if(speechErrorCount>=4){
                    speechErrorCount=0;
                    speak("Voice connection එකට පොඩි issue එකක්. Maya ආයෙත් try කරනවා. 🎙️");
                    restart(2500);
                }else restart(delay);
            }
            public void onResults(Bundle b){
                listening=false; speechErrorCount=0;
                if(!mayaAllowed()){ stopSelf(); return; }
                ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String s=m==null||m.isEmpty()?"":m.get(0);
                handle(s);
                // In continuous voice mode, speak() starts the next listening turn
                // after TTS finishes. Avoid scheduling a second recognizer here.
                if(conversationMode){
                    if(!ttsSpeaking) restart(450);
                }else if(realWakeWordActive){
                    realWakeWordActive=false;
                    wakeWordDetected=false;
                    restart(900);
                }else{
                    restart(1300);
                }
                fallbackListening=false;
            }
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        SharedPreferences lp=getSharedPreferences("settings",MODE_PRIVATE);
        String selectedLanguage=lp.getString("maya_language","auto");
        String country=Locale.getDefault().getCountry();
        boolean sinhala="si".equals(selectedLanguage)
                || ("auto".equals(selectedLanguage) && "LK".equalsIgnoreCase(country));
        String recognitionLocale=sinhala ? "si-LK" : "en-LK";
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognitionLocale);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,recognitionLocale);
        try{
            handler.postDelayed(() -> {
                if(!stopping && conversationMode && !listening && recognizer!=null){
                    try{ recognizer.startListening(i); }
                    catch(Exception ex){ listening=false; microphoneReleaseAt=System.currentTimeMillis()+1600L; restart(1800); }
                }
            }, 350L);
        }catch(Exception e){
            listening=false;
            restart(1500);
        }
    }
    private final Runnable listenRunnable=new Runnable(){@Override public void run(){listen();}};
    private final Runnable wakeWordRunnable=new Runnable(){@Override public void run(){startWakeWord();}};
    private void restart(long d){
        if(handler!=null && mayaAllowed() && !stopping){
            handler.removeCallbacks(listenRunnable);
            handler.postDelayed(listenRunnable,d);
        }
    }

    /**
     * Detects a real user voice while Maya is speaking, then hands the single
     * microphone back to SpeechRecognizer. This gives natural interruption
     * without running two SpeechRecognizers at the same time.
     */
    private void startBargeInListening(){
        if(stopping || !conversationMode || !ttsSpeaking || bargeInListening) return;
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) return;
        stopBargeInListening();
        final int sampleRate=16000;
        final int min=AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0) return;
        final int bufferSize=Math.max(min*2,2048);
        try{
            bargeInAudio=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,
                AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSize);
            if(bargeInAudio.getState()!=AudioRecord.STATE_INITIALIZED){ stopBargeInListening(); return; }
            bargeInStopRequested=false;
            bargeInListening=true;
            bargeInThread=new Thread(() -> {
                short[] buffer=new short[bufferSize/2];
                long speechStart=0L;
                try{
                    bargeInAudio.startRecording();
                    while(!bargeInStopRequested && !stopping && conversationMode && ttsSpeaking){
                        int n=bargeInAudio.read(buffer,0,buffer.length);
                        if(n<=0) continue;
                        double sum=0.0;
                        for(int x=0;x<n;x++){ double v=buffer[x]/32768.0; sum+=v*v; }
                        double rms=Math.sqrt(sum/n);
                        long now=System.currentTimeMillis();
                        // Require sustained voice energy to avoid triggering from a tiny noise.
                        if(rms>0.055){
                            if(speechStart==0L) speechStart=now;
                            if(now-speechStart>=260L && now-lastBargeInAt>900L){
                                lastBargeInAt=now;
                                handler.post(this::interruptMayaSpeech);
                                break;
                            }
                        }else{
                            speechStart=0L;
                        }
                    }
                }catch(Exception ignored){}
                finally{
                    try{ if(bargeInAudio!=null) bargeInAudio.stop(); }catch(Exception ignored){}
                    try{ if(bargeInAudio!=null) bargeInAudio.release(); }catch(Exception ignored){}
                    bargeInAudio=null;
                    bargeInListening=false;
                }
            },"Maya-BargeIn-VAD");
            bargeInThread.start();
        }catch(Exception e){
            stopBargeInListening();
        }
    }

    private void interruptMayaSpeech(){
        if(!ttsSpeaking) return;
        try{ if(tts!=null) tts.stop(); }catch(Exception ignored){}
        ttsSpeaking=false;
        stopBargeInListening();
        // Give TTS/audio output a moment to release before the single SpeechRecognizer starts.
        if(conversationMode && !stopping && handler!=null){
            handler.removeCallbacks(listenRunnable);
            handler.postDelayed(() -> {
                if(conversationMode && !stopping && !listening) listen();
            },950L);
        }
    }

    private void stopBargeInListening(){
        bargeInStopRequested=true;
        bargeInListening=false;
        if(bargeInAudio!=null){
            try{bargeInAudio.stop();}catch(Exception ignored){}
            try{bargeInAudio.release();}catch(Exception ignored){}
            bargeInAudio=null;
        }
        bargeInThread=null;
    }

    private void endConversationMode(){
        boolean wasConversation=conversationMode;
        stopBargeInListening();
        if(handler!=null) handler.removeCallbacks(conversationSilenceRunnable);
        conversationMode=false;
        realWakeWordActive=false;
        wakeWordDetected=false;
        fallbackListening=false;
        if(recognizer!=null){
            try{recognizer.cancel();}catch(Exception ignored){}
            try{recognizer.destroy();}catch(Exception ignored){}
            recognizer=null;
            listening=false;
            microphoneReleaseAt=System.currentTimeMillis()+MIC_RELEASE_GUARD_MS;
        }else listening=false;
        if(handler!=null && !stopping && wasConversation) handler.postDelayed(wakeWordRunnable,1200);
    }

    private boolean isWaitCommand(String q){
        if(q==null)return false;
        String l=q.toLowerCase(Locale.ROOT).trim();
        return l.equals("wait") || l.equals("wait maya") || l.equals("waiting") ||
            l.equals("wait a second") || l.equals("wait a bit") ||
            l.equals("tikak inn") || l.equals("tikak inna") ||
            l.equals("poddak inn") || l.equals("poddak inna") ||
            l.equals("tikak wait karanna") || l.equals("poddak wait karanna") ||
            l.equals("wait karanna") || l.equals("ඉන්න") ||
            l.equals("ටිකක් ඉන්න") || l.equals("පොඩ්ඩක් ඉන්න") ||
            l.equals("ටිකක් wait කරන්න") || l.equals("පොඩ්ඩක් wait කරන්න") ||
            l.equals("wait කරන්න") || l.equals("බලාගෙන ඉන්න");
    }

    private boolean isConversationStopCommand(String q){
        if(q==null)return false;
        String l=q.toLowerCase(Locale.ROOT).trim();
        return l.equals("stop")||l.equals("stop talking")||l.equals("stop listening")||
            l.equals("end conversation")||l.equals("exit conversation")||
            l.equals("conversation off")||l.equals("voice off")||
            l.equals("නවත්වන්න")||l.equals("කතා කරන එක නවත්වන්න")||
            l.equals("කතාබහ නවත්වන්න")||l.equals("voice නවත්වන්න")||
            l.equals("hari")||l.equals("hari thanks")||l.equals("hari thank you")||
            l.equals("thanks")||l.equals("thank you")||l.equals("thankyou")||
            l.equals("thanks maya")||l.equals("thank you maya")||
            l.equals("ok")||l.equals("okay")||l.equals("ok bye")||
            l.equals("okay bye")||l.equals("bye")||l.equals("goodbye")||
            l.equals("ස්තුතියි")||l.equals("බොහොම ස්තුතියි")||l.equals("හරි ස්තුතියි");
    }

    private void handle(String raw){
        if(!mayaAllowed()){ stopSelf(); return; }
        String s=raw==null?"":raw.trim();
        String l=s.toLowerCase(Locale.ROOT);
        // In fallback SpeechRecognizer mode, listening itself is the trigger.
        // Only the real wake-word path requires "Maya" to be spoken first.
        if(!realWakeWordActive && !fallbackListening && !(l.contains("maya")||l.contains("මායා"))) return;
        if(realWakeWordActive || fallbackListening || !wakeWordEnabled) wakeWordDetected=true;
        String q=l.replace("maya","").replace("මායා","").replace("මයා","").trim();
        // Also accept the phrase "Hey Maya" when it reaches speech recognition.
        if(q.equals("hey")) q="";
        else if(q.startsWith("hey ")) q=q.substring(4).trim();
        q=normalizeMixedCommand(q);
        if(workoutLiveMode && (q.equals("maya stop") || q.equals("stop maya") || q.equals("live stop") || q.equals("workout live off") || q.equals("stop workout live"))){
            stopWorkoutLiveConversation();
            return;
        }

        if(q.contains("workout live") || q.contains("live conversation on") || q.contains("live mode on") ||
           q.contains("start workout conversation") || q.contains("workout කරනකොට maya") ||
           q.contains("workout ekedi maya") || q.contains("workout එකේදී maya")){
            startWorkoutLiveConversation();
            return;
        }

        if(isConversationStopCommand(q)){
            conversationMode=false;
            speak("හරි 😄 Voice conversation එක නවත්තනවා. ආයෙත් Maya කියලා කතා කළාම මං එන්නම්.");
            endConversationMode();
            return;
        }

        // "wait" / "waiting" keeps the live conversation open for 10 seconds.
        if(isWaitCommand(q)){
            if(handler!=null){
                handler.removeCallbacks(conversationSilenceRunnable);
                handler.postDelayed(conversationSilenceRunnable,10000L);
            }
            speak("හරි 😄 තත්පර 10ක් wait කරනවා.");
            return;
        }
        // Once Maya is invoked as the system assistant, keep the microphone turn-by-turn
        // active until the user explicitly ends the conversation.
        if(conversationMode) wakeWordDetected=true;
        q=resolveSmartIntent(q);
        String[] steps=splitMultiStepCommand(q);
        if(steps.length>1){
            StringBuilder combined=new StringBuilder();
            for(String step:steps){
                String intent=resolveSmartIntent(step);
                if(combined.length()>0) combined.append(" | ");
                combined.append(intent);
            }
            q=combined.toString();
        }
        if(q.isEmpty()) return;
        String followUp=followUpContext(q);
        if(followUp!=null){
            askAI(followUp);
            return;
        }
        lastUserQuery=q;

        if(isNoteCommand(q)){
            handleNoteCommand(q);
        }else if(isMemoryCommand(q)){
            handleMemory(q);
        }else if(q.contains("open ")||q.startsWith("open")||q.contains("launch ")||q.contains("start ")||q.contains("open app")||q.contains("ඇප් එක open")||q.contains("ඇප් එක අරින්න")){
            openApp(q);
        }else if(q.contains("call")||q.contains("කෝල්")){
            String target=q.replace("call","").replace("කෝල්","").trim();
            callContact(target);
        }else if((q.contains("do not disturb")||q.contains("dnd")||q.contains("disturb")) && (q.contains("on")||q.contains("දාන්න")||q.contains("enable"))){
            setDnd(true);
        }else if((q.contains("do not disturb")||q.contains("dnd")||q.contains("disturb")) && (q.contains("off")||q.contains("අයින්")||q.contains("disable"))){
            setDnd(false);
        }else if(q.contains("flash")||q.contains("torch")||q.contains("ටෝච්")){
            toggleFlash();
        }else if(q.contains("volume")||q.contains("ශබ්ද")||q.contains("sound")){
            volumeUp();
        }else if(q.contains("music")||q.contains("pause")||q.contains("play")||q.contains("සින්දු")){
            mediaKey(q.contains("pause")||q.contains("නවත්ත"));
        }else if(q.contains("notification")||q.contains("whatsapp")||q.contains("නොටිෆිකේෂන්")){
            readLatestNotification();
        }else if(q.contains("battery")||q.contains("බැටරි")||q.contains("charge")){
            batteryStatus();
        }else if(q.contains("wifi")||q.contains("wi-fi")||q.contains("වයිෆයි")){
            openSystemSettings(Settings.ACTION_WIFI_SETTINGS,"Wi-Fi settings");
        }else if(q.contains("bluetooth")||q.contains("බ්ලූටූත්")){
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS,"Bluetooth settings");
        }else if(q.contains("airplane")||q.contains("flight mode")||q.contains("airplane mode")){
            openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS,"Airplane mode settings");
        }else if(q.contains("location")||q.contains("gps")||q.contains("ලොකේෂන්")){
            openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS,"Location settings");
        }else if(q.contains("brightness")||q.contains("screen light")||q.contains("දීප්තිය")){
            setBrightness(q);
        }else if(q.contains("screen timeout")||q.contains("screen sleep")||q.contains("display timeout")){
            setScreenTimeout(q);
        }else if(q.contains("alarm")||q.contains("ඇලර්ම්")){
            openAlarm();
        }else if(q.contains("timer")||q.contains("ටයිමර්")){
            openTimer(q);
        }else if(q.contains("nfc")){
            openSystemSettings(Settings.ACTION_NFC_SETTINGS,"NFC settings");
        }else if(q.contains("data usage")||q.contains("mobile data")||q.contains("internet settings")){
            openSystemSettings(Settings.ACTION_DATA_USAGE_SETTINGS,"Data usage settings");
        }else if(q.contains("display settings")||q.contains("screen settings")){
            openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }else if(q.contains("app settings")||q.contains("application settings")){
            openSystemSettings(Settings.ACTION_APPLICATION_SETTINGS,"App settings");
        }else if(q.contains("notification settings")){
            openSystemSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS,"Notification settings");
        }else if(q.contains("calendar")||q.contains("schedule")||q.contains("කැලැන්ඩර්")||q.contains("event")){
            calendarEvent(q);
        }else if(q.contains("remind")||q.contains("reminder")||q.contains("මතක් කරන්න")){
            reminder(q);
        }else if(q.contains("device info")||q.contains("phone info")||q.contains("about phone")){
            deviceInfo();
        }else if(q.contains("plan")||q.contains("short plan")||q.contains("daily plan")||q.contains("මගේ plan")||q.contains("අද plan")||q.contains("අද වැඩ")){
            speak("📝 "+MayaOfflineNLP.answer(this,"plan"));
        }else if(q.contains("focus")||q.contains("focus goal")||q.contains("මගේ focus")){
            String live=MayaContextProvider.build(this);
            speak("🎯 "+extractContext(live,"focusGoal="));
        }else if(q.contains("mission")||q.contains("මගේ mission")||q.contains("mission එක")||q.contains("mission eka")||q.contains("ada mission")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"mission"));
        }else if(q.contains("xp")||q.contains("level")||q.contains("මගේ ලෙවල්")||q.contains("mage xp")||q.contains("xp kiyada")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"xp"));
        }else if(q.contains("streak")||q.contains("මගේ streak")||q.contains("streak eka")||q.contains("mage streak")){
            speak("ඔන්න 🔥 "+MayaContextProvider.quickStatus(this,"streak"));
        }else if(q.contains("day")||q.contains("දවස කීයද")||q.contains("කීවෙනි දවස")||q.contains("kaweni dawaseda")||q.contains("mage day")){
            speak("හරි 📅 "+MayaContextProvider.quickStatus(this,"day"));
        }else if(q.contains("progress")||q.contains("කොහොමද යන්නේ")||q.contains("mage progress")||q.contains("progress eka kohomada")){
            speak("හරි 😄 "+MayaContextProvider.quickStatus(this,"progress"));
        }else if(q.contains("hello")||q.contains("hi")||q.contains("හෙලෝ")){
            speak(modeReply("හෙලෝ! මං Maya. කියන්න. 😄","හෙලෝ 😄 Maya online! කියන්නකෝ ✨","හෙලෝ! මං මෙතන. හෙමින් කියන්න. 💛"));
        }else if(q.contains("motivat")||q.contains("වැඩ")||q.contains("බැහැ")){
            speak(modeReply("හරි, පොඩි task එකක් දැන්ම පටන් ගමු. 🔥","හරි hero 😂 excuses වලට අද නිවාඩු! පටන් ගමු. 🔥","හරි, අමාරු නම් පොඩියෙන් පටන් ගමු. ඔයාට පුළුවන්. 💛"));
        }else{
            askAI(s);
        }
    }


    private String normalizeMixedCommand(String q){
        if(q==null) return "";
        String s=q.toLowerCase(Locale.ROOT).trim();
        String[][] map={
            {"mage","මගේ"},{"eka","එක"},{"kiyada","කීයද"},{"kohomada","කොහොමද"},
            {"mona","මොන"},{"monawada","මොනවද"},{"ada","අද"},{"dan","දැන්"},
            {"karanna","කරන්න"},{"karamu","කරමු"},{"thiyenawada","තියෙනවද"},
            {"nathi","නැති"},{"on","දාන්න"},{"off","අයින් කරන්න"},
            {"balanna","බලන්න"},{"kiyanna","කියන්න"},{"hari","හරි"}
        };
        for(String[] pair:map) s=s.replaceAll("(?<![\\p{L}])"+java.util.regex.Pattern.quote(pair[0])+"(?![\\p{L}])",pair[1]);
        return s.replaceAll("\\s+"," ").trim();
    }

    private String resolveSmartIntent(String q){
        if(q==null) return "";
        String s=q.trim();
        String l=s.toLowerCase(Locale.ROOT);
        if(l.contains("streak")) return "streak";
        if(l.contains("xp") || l.contains("level")) return "xp";
        if(l.contains("focus")) return "focus";
        if(l.contains("plan") || l.contains("daily") || l.contains("short plan") || l.contains("අද වැඩ")) return "plan";
        if(l.contains("torch") || l.contains("flash") || l.contains("ටෝච්")){
            if(l.contains("off") || l.contains("අයින්")) return "torch off";
            return "torch on";
        }
        return s;
    }

    private String[] splitMultiStepCommand(String q){
        if(q==null || q.trim().isEmpty()) return new String[]{""};
        String[] parts=q.split("\\s+(?:and|then|සහ|ඊළඟට|ඊට පස්සේ)\\s+");
        return parts.length==0 ? new String[]{q.trim()} : parts;
    }

    private String followUpContext(String q){
        if(q==null || q.trim().isEmpty()) return null;
        String l=q.toLowerCase(Locale.ROOT).trim();
        boolean shortReply=l.equals("yes")||l.equals("yeah")||l.equals("yep")||l.equals("ok")||
            l.equals("okay")||l.equals("sure")||l.equals("no")||l.equals("nah")||
            l.equals("හරි")||l.equals("ඔව්")||l.equals("ඔව් කරමු")||l.equals("ඒක කරමු");
        boolean continuation=l.contains("and then")||l.contains("then what")||l.contains("what next")||
            l.contains("තව කියන්න")||l.contains("ඊළඟට")||l.contains("ඉතින්")||l.contains("ඒක මොකක්ද");
        if(!shortReply && !continuation) return null;
        if(lastUserQuery.isEmpty() && lastMayaReply.isEmpty()) return null;
        return "Continue the conversation naturally. Previous user: "+lastUserQuery+
            " | Previous Maya reply: "+lastMayaReply+" | Current user: "+q;
    }

    private String extractContext(String s,String key){
        int i=s.indexOf(key); if(i<0)return "අද focus goal එකක් set කරලා නැහැ.";
        int j=s.indexOf(';',i); if(j<0)j=s.length();
        String v=s.substring(i+key.length(),j).trim();
        if(v.startsWith("\"")) v=v.substring(1);
        if(v.endsWith("\"")) v=v.substring(0,v.length()-1);
        return v.isEmpty()?"අද focus goal එකක් set කරලා නැහැ.":v;
    }

    private void askAI(String userText){
        String predictive = MayaPredictiveActions.nextSuggestion(this);
        if (userText == null || userText.trim().isEmpty()) { speak(predictive); return; }
        String memoryText=memory==null?"":memory.relevant(userText);
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        String personality;
        if(p.getBoolean("mode_auto",false)){
            int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            personality=hour<12?"calm morning coach":hour>=21?"gentle night coach":"energetic discipline coach";
        }else if(p.getBoolean("mode_sweet",false)) personality="sweet/caring";
        else if(p.getBoolean("mode_cute",false)) personality="cute";
        else if(p.getBoolean("mode_funny",true)) personality="funny";
        else personality="normal";

        String live=MayaContextProvider.build(this);
        String context="Public creator profile: Maya was created by Lesandu Ransuka. Share only this creator name unless additional public profile information is explicitly provided in the current conversation. Never reveal private memory or private conversations. | Saved memory: "+memoryText+" | "+live+
                " Current app state is authoritative for discipline data. Use it naturally and don't invent values.";
        MayaAI.ask(this,userText,context,personality,reply->handler.post(()->{
            lastMayaReply=reply==null?"":reply;
            speak(reply);
        }));
    }

    
    private boolean isNoteCommand(String q){
        if(q==null)return false;
        String l=q.toLowerCase(Locale.ROOT);
        return l.contains("note")||l.contains("notes")||l.contains("නෝට්")||l.contains("මතක සටහන");
    }

    private void handleNoteCommand(String q){
        String l=q==null?"":q.toLowerCase(Locale.ROOT).trim();
        SharedPreferences p=getSharedPreferences("discipline",MODE_PRIVATE);
        int count;
        try{count=Math.max(0,Math.min(500,p.getInt("notes_count",0)));}catch(Exception e){count=0;}

        if(l.contains("delete")||l.contains("remove")||l.contains("මකන්න")||l.contains("අයින් කරන්න")){
            if(count<=0){speak("දැනට delete කරන්න note එකක් නැහැ. 📝");return;}
            String body=q.replaceAll("(?i)maya","").replaceAll("(?i)delete note","").replaceAll("(?i)remove note","")
                .replace("note","").replace("notes","").replace("මකන්න","").replace("අයින් කරන්න","").trim();
            if(body.isEmpty()||body.equals("එක")||body.equals("ඒක")){
                speak("Delete කරන්න note එකේ title එක කියන්න.");
                return;
            }
            int found=-1;
            for(int i=0;i<count;i++){
                String title=p.getString("note_"+i+"_title","");
                if(title!=null&&!title.isEmpty()&&title.toLowerCase(Locale.ROOT).contains(body.toLowerCase(Locale.ROOT))){found=i;break;}
            }
            if(found<0){speak("ඒ නමින් note එකක් හම්බවුනේ නැහැ. 📝");return;}
            deleteVoiceNote(p,found,count);
            speak("හරි, " + p.getString("note_"+found+"_title","note") + " note එක delete කළා. 🗑️");
            return;
        }

        if(l.contains("list")||l.contains("show")||l.contains("read")||l.contains("all notes")||
           l.contains("notes tika")||l.contains("notes ටික")||l.contains("නෝට් ටික")||l.contains("නෝට්ස්")){
            if(count<=0){speak("දැනට notes නැහැ. 📝");return;}
            StringBuilder out=new StringBuilder("ඔයාගේ notes මෙන්න. 📝 ");
            int limit=Math.min(count,10);
            for(int i=0;i<limit;i++){
                String title=p.getString("note_"+i+"_title","Untitled note");
                String body=p.getString("note_"+i+"_body","");
                out.append(i+1).append(". ").append(title);
                if(body!=null&&!body.trim().isEmpty())out.append(" — ").append(body.trim());
                if(i<limit-1)out.append(". ");
            }
            if(count>limit)out.append(" තව ").append(count-limit).append(" notes තියෙනවා.");
            speak(out.toString());
            return;
        }

        String text=q;
        text=text.replaceFirst("(?i)^save\\s+(a\\s+)?note\\s*", "");
        text=text.replaceFirst("(?i)^add\\s+(a\\s+)?note\\s*", "");
        text=text.replaceFirst("(?i)^create\\s+(a\\s+)?note\\s*", "");
        text=text.replaceFirst("(?i)^write\\s+(a\\s+)?note\\s*", "");
        text=text.replace("note එකක් දාන්න","").replace("note එකක් දා","").replace("නෝට් එකක් දාන්න","")
            .replace("නෝට් එකක් දා","").replace("මතක සටහනක් දාන්න","").trim();
        if(text.isEmpty()||text.equals("note")||text.equals("notes")){
            speak("මොනවාද note එකට save කරන්න ඕනේ? 📝");
            return;
        }
        if(count>=500){speak("Notes limit එක පිරී තියෙනවා. 📝");return;}
        String title=text.length()>40?text.substring(0,40).trim():text;
        String body=text;
        p.edit().putString("note_"+count+"_title",title).putString("note_"+count+"_body",body)
            .putLong("note_"+count+"_time",System.currentTimeMillis()).putBoolean("note_"+count+"_pinned",false)
            .putInt("notes_count",count+1).apply();
        speak("හරි 😄 note එක save කළා. 📝");
    }

    private void deleteVoiceNote(SharedPreferences p,int id,int n){
        SharedPreferences.Editor e=p.edit();
        for(int i=id;i<n-1;i++){
            e.putString("note_"+i+"_title",p.getString("note_"+(i+1)+"_title","Untitled note"));
            e.putString("note_"+i+"_body",p.getString("note_"+(i+1)+"_body",""));
            e.putLong("note_"+i+"_time",p.getLong("note_"+(i+1)+"_time",0L));
            e.putBoolean("note_"+i+"_pinned",p.getBoolean("note_"+(i+1)+"_pinned",false));
        }
        e.remove("note_"+(n-1)+"_title").remove("note_"+(n-1)+"_body").remove("note_"+(n-1)+"_time")
            .remove("note_"+(n-1)+"_pinned").putInt("notes_count",n-1).apply();
    }

    private boolean isMemoryCommand(String q){
        return q.contains("remember") || q.contains("mathaka") || q.contains("මතක") ||
               q.contains("memory") || q.contains("save this") || q.contains("save me") ||
               q.contains("forget") || q.contains("delete memory") || q.contains("clear memory");
    }

    private void handleMemory(String q){
        if(q.contains("what do you remember") || q.contains("what you remember") ||
           q.contains("මොනවා මතක") || q.contains("මතක තියෙන්නේ මොනවාද") || q.contains("memory list")){
            String all=memory.all();
            speak(all.isEmpty() ? "දැනට මගේ memory එක හිස්. 😄" : "මට මතක තියෙන්නේ මෙන්න:\n"+all);
            return;
        }
        if(q.contains("forget") || q.contains("delete memory") || q.contains("clear memory") ||
           q.contains("මතක අයින්") || q.contains("මතක මකන්න")){
            memory.clear();
            speak("හරි, මගේ saved memory එක clear කළා. 🧹");
            return;
        }

        String fact=q;
        String[] prefixes={
            "please remember","remember that","remember","save this","save me",
            "mathaka thiyaganna","mathaka thiyaganna meka","මතක තියාගන්න","මතක තියාගන්න මේක",
            "මතක තියාගන්න"
        };
        for(String prefix:prefixes){
            if(fact.startsWith(prefix)){
                fact=fact.substring(prefix.length()).trim();
                break;
            }
        }
        fact=fact.replaceFirst("^[,:;- ]+","");
        if(fact.isEmpty()){
            speak("මොකක්ද මතක තියාගන්න ඕනේ? 😄");
            return;
        }

        // Automatic memory only accepts clear, ordinary facts/preferences.
        // Avoid storing passwords, codes, payment details, or other sensitive secrets.
        String lower=fact.toLowerCase(Locale.ROOT);
        if(lower.contains("password")||lower.contains("passcode")||lower.contains("otp")||
           lower.contains("pin")||lower.contains("cvv")||lower.contains("credit card")||
           lower.contains("debit card")){
            speak("Passwords, PINs, OTPs වගේ sensitive details මං memory එකට save කරන්නේ නැහැ. 🔒");
            return;
        }

        memory.remember(fact);
        speak("හරි, ඒක මතක තියාගත්තා. 🧠✨");
    }

    private void openApp(String command){
        String q=command.toLowerCase(Locale.ROOT)
            .replace("open app","").replace("open","").replace("launch","").replace("start","")
            .replace("ඇප් එක open","").replace("ඇප් එක අරින්න","").trim();
        String pkg=null, name=q;
        if(q.contains("youtube")||q.contains("යූටියුබ්")){pkg="com.google.android.youtube";name="YouTube";}
        else if(q.contains("whatsapp")||q.contains("වට්ස්ඇප්")){pkg="com.whatsapp";name="WhatsApp";}
        else if(q.contains("chrome")||q.contains("ක්‍රෝම්")){pkg="com.android.chrome";name="Chrome";}
        else if(q.contains("instagram")||q.contains("ඉන්ස්ටග්‍රෑම්")){pkg="com.instagram.android";name="Instagram";}
        else if(q.contains("facebook")||q.contains("ෆේස්බුක්")){pkg="com.facebook.katana";name="Facebook";}
        else if(q.contains("tiktok")||q.contains("ටික්ටොක්")){pkg="com.zhiliaoapp.musically";name="TikTok";}
        else if(q.contains("spotify")||q.contains("ස්පොටිෆයි")){pkg="com.spotify.music";name="Spotify";}
        else if(q.contains("maps")||q.contains("map")||q.contains("මැප්")){pkg="com.google.android.apps.maps";name="Google Maps";}
        else if(q.contains("gmail")||q.contains("ජීමේල්")){pkg="com.google.android.gm";name="Gmail";}
        else if(q.contains("camera")||q.contains("කැමරා")){pkg="com.android.camera";name="Camera";}
        else if(q.contains("settings")||q.contains("සෙටින්")){pkg="com.android.settings";name="Settings";}
        if(pkg==null){
            speak("ඒ app එකේ නම මට හඳුනාගන්න බැරි වුණා. YouTube, WhatsApp, Chrome, Instagram, TikTok, Spotify, Maps වගේ app එකක් කියන්න.");
            return;
        }
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null){speak(name+" phone එකේ install කරලා නැහැ.");return;}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            speak(name+" open කළා. 📱");
        }catch(Exception e){speak(name+" open කරන්න බැරි වුණා.");}
    }

    private String modeReply(String normal,String funny,String sweet){
        SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
        String mode=p.getString("maya_mode","motivative");
        if("auto".equals(mode)){
            Calendar c=Calendar.getInstance();
            int h=c.get(Calendar.HOUR_OF_DAY);
            mode=(h>=21||h<7)?"caring":"motivative";
        }
        if("caring".equals(mode)) return sweet;
        if("romance".equals(mode)) return "හරි 💗 Maya මෙතන. කියන්න, අද discipline + fitness target එකට යමු.";
        if("angry".equals(mode)) return "ඔව්. 😤 කියන්න. දැන් excuses නැතුව target එකට යමු.";
        if("motivative".equals(mode)) return funny;
        return normal;
    }

    private void toggleFlash(){
        if(Build.VERSION.SDK_INT<23){speak("මේ phone එකේ flashlight control support නැහැ.");return;}
        try{
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);
            String id=cm.getCameraIdList()[0];
            SharedPreferences p=getSharedPreferences("maya_runtime",MODE_PRIVATE);
            boolean on=!p.getBoolean("flash",false);
            cm.setTorchMode(id,on);p.edit().putBoolean("flash",on).apply();
            speak(on?"Torch ON 🔦":"Torch OFF");
        }catch(Exception e){speak("Torch control කරන්න බැරි වුණා.");}
    }

    private void volumeUp(){
        try{
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
            speak("Volume ටිකක් වැඩි කළා. 🔊");
        }catch(Exception e){speak("Volume control කරන්න බැරි වුණා.");}
    }

    private void mediaKey(boolean pause){
        try{
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            long now=SystemClock.uptimeMillis();
            int key=pause?KeyEvent.KEYCODE_MEDIA_PAUSE:KeyEvent.KEYCODE_MEDIA_PLAY;
            KeyEvent down=new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,0);
            KeyEvent up=new KeyEvent(now,now,KeyEvent.ACTION_UP,key,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,0);
            am.dispatchMediaKeyEvent(down);
            am.dispatchMediaKeyEvent(up);
            speak(pause?"Music pause කළා. ⏸️":"Music play කළා. ▶️");
        }catch(Exception e){speak("Music control කරන්න බැරි වුණා.");}
    }

    private void callContact(String name){
        if(name==null||name.trim().isEmpty()){speak("කාට call කරන්නද කියන්න.");return;}
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
            speak("Contacts permission එක app එකේ Settings වලින් allow කරන්න.");
            return;
        }
        Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+name.trim()+"%"},
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
        String number=null,display=null;
        if(c!=null){if(c.moveToFirst()){number=c.getString(0);display=c.getString(1);}c.close();}
        if(number==null){speak(name+" කියන contact එක හම්බවුනේ නැහැ.");return;}
        try{
            Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(display+" ගේ call screen එක open කළා.");
        }catch(Exception e){speak("Call screen එක open කරන්න බැරි වුණා.");}
    }

    private void setDnd(boolean on){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=23 && !nm.isNotificationPolicyAccessGranted()){
            speak("DND permission එක දෙන්න. Settings වල Maya DND access enable කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        if(Build.VERSION.SDK_INT>=23){
            nm.setInterruptionFilter(on?NotificationManager.INTERRUPTION_FILTER_NONE:NotificationManager.INTERRUPTION_FILTER_ALL);
            speak(on?"Do Not Disturb ON කළා. 🔕":"Do Not Disturb OFF කළා. 🔔");
        }
    }

    private void readLatestNotification(){
        String text=getSharedPreferences("maya_notifications",MODE_PRIVATE).getString("latest","");
        if(text.isEmpty()) speak("අලුත් notification එකක් මට read කරන්න ලැබිලා නැහැ.");
        else speak(text);
    }

    private void openSystemSettings(String action,String label){
        try{
            Intent i=new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(label+" open කළා. ⚙️");
        }catch(Exception e){speak(label+" open කරන්න බැරි වුණා.");}
    }

    private void batteryStatus(){
        try{
            BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);
            int level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            IntentFilter f=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent b=registerReceiver(null,f);
            boolean charging=false;
            if(b!=null){int status=b.getIntExtra(BatteryManager.EXTRA_STATUS,-1); charging=status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL;}
            speak("Battery එක "+level+"%. "+(charging?"දැනට charge වෙනවා. 🔋":"දැනට charge වෙන්නේ නැහැ. 🔋"));
        }catch(Exception e){speak("Battery status එක ගන්න බැරි වුණා.");}
    }

    private void setBrightness(String q){
        if(!Settings.System.canWrite(this)){
            speak("Screen brightness control කරන්න WRITE SETTINGS permission එක allow කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d{1,3})").matcher(q);
            if(m.find()){
                int pct=Math.max(1,Math.min(100,Integer.parseInt(m.group(1))));
                int value=Math.round(255f*pct/100f);
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
                speak("Brightness "+pct+"% කළා. ☀️");
            }else openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }catch(Exception e){speak("Brightness change කරන්න බැරි වුණා.");}
    }

    private void setScreenTimeout(String q){
        if(!Settings.System.canWrite(this)){
            speak("Screen timeout change කරන්න WRITE SETTINGS permission එක allow කරන්න.");
            try{Intent i=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}
            return;
        }
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)").matcher(q);
            if(m.find()){
                int minutes=Math.max(1,Math.min(60,Integer.parseInt(m.group(1))));
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT,minutes*60*1000);
                speak("Screen timeout "+minutes+" minutes කළා. 💤");
            }else openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS,"Display settings");
        }catch(Exception e){speak("Screen timeout change කරන්න බැරි වුණා.");}
    }

    private void openAlarm(){
        try{
            Intent i=new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Alarm screen එක open කළා. ⏰");
        }catch(Exception e){speak("Alarm app එක open කරන්න බැරි වුණා.");}
    }

    private void openTimer(String q){
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\\\d+)").matcher(q);
            Intent i=new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
            if(m.find()) i.putExtra(android.provider.AlarmClock.EXTRA_LENGTH,Math.max(1,Math.min(86400,Integer.parseInt(m.group(1))*60)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(m.find()?"Timer screen එක open කළා. ⏱️":"Timer screen එක open කළා. ⏱️");
        }catch(Exception e){speak("Timer app එක open කරන්න බැරි වුණා.");}
    }

    private void calendarEvent(String q){
        try{
            Intent i=new Intent(Intent.ACTION_INSERT);
            i.setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE, extractEventTitle(q));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Calendar event එක add කරන්න screen එක open කළා. 📅");
        }catch(Exception e){speak("Calendar එක open කරන්න බැරි වුණා.");}
    }

    private String extractEventTitle(String q){
        String s=q.replace("add calendar event","").replace("calendar event","")
            .replace("add event","").replace("schedule","").replace("event","").trim();
        return s.isEmpty()?"Maya event":s;
    }

    private void reminder(String q){
        try{
            String text=q.replace("remind me","").replace("set reminder","")
                .replace("reminder","").replace("මතක් කරන්න","").trim();
            Intent i=new Intent(Intent.ACTION_INSERT);
            i.setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.Events.TITLE,text.isEmpty()?"Maya reminder":text);
            i.putExtra(CalendarContract.Events.DESCRIPTION,"Created by Maya");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Reminder එක save කරන්න calendar screen එක open කළා. 🔔");
        }catch(Exception e){speak("Reminder එක create කරන්න බැරි වුණා.");}
    }

    private void deviceInfo(){
        String model=Build.MANUFACTURER+" "+Build.MODEL;
        speak("Phone එක "+model+". Android "+Build.VERSION.RELEASE+". API "+Build.VERSION.SDK_INT+".");
    }

    private void speak(String s){
        try{
            if(s==null||s.trim().isEmpty()||tts==null||!ready)return;
            String normalized=s.trim().replaceAll("\\s+"," ");
            long now=System.currentTimeMillis();
            // Prevent the same response from being spoken twice within a short window.
            if(normalized.equals(lastSpokenText) && now-lastSpokenAt<4500L)return;
            lastSpokenText=normalized;
            lastSpokenAt=now;
            SharedPreferences p=getSharedPreferences("settings",MODE_PRIVATE);
            if(!p.getBoolean("auto_speak",true))return;
            if(recognizer!=null&&listening){try{recognizer.cancel();}catch(Exception ignored){}listening=false;}
            float speed=Math.max(0,Math.min(100,p.getInt("speech_speed",45)));
            // Slightly slower default speech and a neutral pitch make Sinhala words easier to understand.
            float rate=.68f+(speed/100f)*.62f;
            tts.setSpeechRate(rate);
            tts.setPitch(1.02f);
            ttsSpeaking=true;
            String id="maya_"+System.currentTimeMillis();
            if(Build.VERSION.SDK_INT>=15){
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
                    @Override public void onStart(String utteranceId){ttsSpeaking=true; startBargeInListening();}
                    @Override public void onDone(String utteranceId){
                        ttsSpeaking=false;
                        if(conversationMode && !stopping && mayaAllowed() && handler!=null){
                            handler.postDelayed(() -> {
                                if(conversationMode && !stopping && !listening) listen();
                            },180);
                        }
                    }
                    @Override public void onError(String utteranceId){
                        ttsSpeaking=false;
                        if(conversationMode && !stopping && mayaAllowed() && handler!=null){
                            handler.postDelayed(() -> {
                                if(conversationMode && !stopping && !listening) listen();
                            },180);
                        }
                    }
                });
            }
            tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,id);
        }catch(Exception ignored){ttsSpeaking=false;}
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel("maya_assistant","Maya Assistant",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Visible notification for Maya background microphone service");
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.createNotificationChannel(ch);
        }
    }
    private void handleAssistantInvocation(){
        if(stopping || !mayaAllowed()) return;
        wakeWordDetected=true;
        realWakeWordActive=true;
        fallbackListening=true;
        conversationMode=true;
        if(wakeWordAdapter!=null){
            try{wakeWordAdapter.stop();}catch(Exception ignored){}
        }
        listen();
    }

    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!mayaAllowed()){stopSelf();return START_NOT_STICKY;}
        if(i!=null && ACTION_PAUSE_LIVE_MIC.equals(i.getAction())){
            if(handler!=null) handler.post(this::pauseForLiveButton);
            return START_STICKY;
        }
        if(i!=null && ACTION_START_WORKOUT_LIVE.equals(i.getAction())){
            if(handler!=null) handler.post(this::startWorkoutLiveConversation);
            return START_STICKY;
        }
        if(i!=null && ACTION_STOP_WORKOUT_LIVE.equals(i.getAction())){
            if(handler!=null) handler.post(this::stopWorkoutLiveConversation);
            return START_STICKY;
        }
        if(i!=null && ACTION_RESUME_LIVE_MIC.equals(i.getAction())){
            if(handler!=null) handler.post(this::resumeAfterLiveButton);
            return START_STICKY;
        }
        if(i!=null && "com.discipline309.app.MAYA_ASSISTANT_INVOCATION".equals(i.getAction())){
            if(handler!=null){
                handler.post(() -> {
                    if(stopping) return;
                    if(!ready){
                        pendingAssistantInvocation=true;
                        return;
                    }
                    handleAssistantInvocation();
                });
            }
        }
        return START_STICKY;
    }
    @Override
    public android.os.IBinder onBind(android.content.Intent intent) {
        return null;
    }

}
