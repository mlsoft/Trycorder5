package net.ddns.mlsoftlaberge.trycorder;

/**
 * Created by mlsoft on 29/07/16.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import net.ddns.mlsoftlaberge.trycorder.tryclient.TryclientActivity;
import net.ddns.mlsoftlaberge.trycorder.utils.Fetcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrycorderService extends Service implements RecognitionListener {

    private int NOTIFICATION_ID = 1701;
    private int SERVERPORT = 1701;
    public final static String BROADCAST_ACTION="net.ddns.mlsoftlaberge.trycorder.updateservice";

    private Intent intentForActivity;

    // the preferences holder
    private SharedPreferences sharedPref;

    private Fetcher mFetcher;

    private Handler mHandler;

    private IBinder mBinder=new TryBinder();

    public class TryBinder extends Binder {

        public TrycorderService getService() {
            return(TrycorderService.this);
        }

    }

    public List<String> getiplist() {
        return(mIpList);
    }

    public List<String> getnamelist() {
        return(mNameList);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    private String deviceName="";
    private boolean autoListen;
    private String speakLanguage;
    private String listenLanguage;
    private boolean isMaster;
    private boolean sendLocal;
    private boolean sendRemote;
    private boolean autoBoot;
    private boolean autoStop;
    private boolean debugMode;
    private String debugAddr;

    @Override
    public void onCreate() {
        super.onCreate();
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Trycorder Service Created ...", Toast.LENGTH_LONG).show();

        mFetcher=new Fetcher(getApplicationContext());

        mHandler=new Handler();

        intentForActivity = new Intent(BROADCAST_ACTION);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        autoListen = sharedPref.getBoolean("pref_key_auto_listen", false);
        speakLanguage = sharedPref.getString("pref_key_speak_language", "");
        listenLanguage = sharedPref.getString("pref_key_listen_language", "");
        deviceName = sharedPref.getString("pref_key_device_name", "Trycorder");
        isMaster = sharedPref.getBoolean("pref_key_ismaster", true);
        sendLocal = sharedPref.getBoolean("pref_key_send_local", true);
        sendRemote = sharedPref.getBoolean("pref_key_send_remote", true);
        autoBoot = sharedPref.getBoolean("pref_key_auto_boot", true);
        autoStop = sharedPref.getBoolean("pref_key_auto_stop", false);
        debugMode = sharedPref.getBoolean("pref_key_debug_mode", false);
        debugAddr = sharedPref.getString("pref_key_debug_addr", "192.168.0.184");

        if(deviceName.equals("Trycorder")) {
            deviceName=mFetcher.fetch_device_name();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("pref_key_device_name", deviceName);
            editor.commit();
        }

        initserver();

        initstarship();

        inittalkserver();

        registerService();

        startdiscoverService();

    }

    private Notification mNotification=null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mNotification==null) {
            mNotification = getNotification("Starting...");
            startForeground(NOTIFICATION_ID, mNotification);
            Toast.makeText(this, "Trycorder Service Started.", Toast.LENGTH_LONG).show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopdiscoverService();
        unregisterService();
        stoptalkserver();
        stopstarship();
        stopserver();
        stopForeground(true);
        super.onDestroy();
        Toast.makeText(this, "Trycorder Service Destroyed", Toast.LENGTH_LONG).show();
    }

    // ============================================================================

    // prepare a notification with the text
    private Notification getNotification(String text){
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), TryclientActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("Trycorder " + deviceName)
                .setContentText("Talk Server running on "+mFetcher.fetch_ip_address()+":"+SERVERPORT)
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.trycorder_icon)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .build();

        return(notification);
    }

    /**
     * This is the method that can be called to update the Notification
     */
    private void updateNotification(String text) {

        Notification notification = getNotification(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    // =========================================================================
    // usage of text-to-speech to speak a sensence
    // =========================================================================
    // usage of text-to-speech to speak a sensence
    private TextToSpeech tts=null;

    private void initspeak() {
        if(tts==null) {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        setspeaklang(speakLanguage);
                    }
                }
            });
        }
    }

    public void setspeaklang(String lng) {
        if (lng.equals("FR")) {
            tts.setLanguage(Locale.FRENCH);
        } else if (lng.equals("EN")) {
            tts.setLanguage(Locale.US);
        } else {
            // default prechoosen language
        }
    }

    public void speak(String texte) {
        initspeak();
        tts.speak(texte, TextToSpeech.QUEUE_ADD, null);
        say("Speaked: "+texte);
    }

    public void speak(String texte,String lng) {
        initspeak();
        setspeaklang(lng);
        tts.speak(texte, TextToSpeech.QUEUE_ADD, null);
        say("Speaked: "+texte);
    }

    // ========================================================================================
    // functions to control the speech process

    // handles for the conversation functions
    private SpeechRecognizer mSpeechRecognizer = null;
    private Intent mSpeechRecognizerIntent = null;

    public void listen() {
        if (mSpeechRecognizer == null) {
            // ============== initialize the audio listener and talker ==============

            //AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(this);
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "net.ddns.mlsoftlaberge.trycorder");
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            //mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 50);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20);

            // produce a FC on android 4.0.3
            //mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
        }

        if (listenLanguage.equals("FR")) {
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
        } else if (listenLanguage.equals("EN")) {
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        } else {
            // automatic
        }
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        //mTextstatus_top.setText("");
        say("Speak");
    }

    // =================================================================================
    // listener for the speech recognition service
    // ========================================================================================
    // functions to listen to the voice recognition callbacks

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> dutexte = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (dutexte != null && dutexte.size() > 0) {
            for (int i = 0; i < dutexte.size(); ++i) {
                String mSentence = dutexte.get(i);
                if (matchvoice(mSentence)) {
                    informActivity("listen",mSentence);
                    return;
                }
            }
            informActivity("listen",dutexte.get(0));
        }
    }

    private boolean matchvoice(String textein) {
        String texte = textein.toLowerCase();
        if (texte.contains("server ok")) return(true);
        //if (texte.contains("red alert")) return(true);
        //if (texte.contains("yellow alert")) return(true);
        if (texte.contains("french") || texte.contains("français")) return(true);
        if (texte.contains("english") || texte.contains("anglais")) return(true);
        //if (texte.contains("martin") || texte.contains("master")) return(true);
        //if (texte.contains("computer") || texte.contains("ordinateur")) return(true);
        if (texte.contains("fuck") || texte.contains("shit")) return(true);
        if (texte.contains("sensor off")) return(true);
        if (texte.contains("sensor") || texte.contains("magnetic")) return(true);
        if (texte.contains("orientation") || texte.contains("direction")) return(true);
        if (texte.contains("gravity") || texte.contains("vibration")) return(true);
        if (texte.contains("temperature") || texte.contains("pressure") || texte.contains("light")) return(true);
        if (texte.contains("hailing") && texte.contains("close")) return(true);
        if (texte.contains("hailing") || texte.contains("frequency")) return(true);
        if (texte.contains("intercom")) return(true);
        if (texte.contains("chatcomm")) return(true);
        if (texte.contains("shield") && texte.contains("down")) return(true);
        if (texte.contains("shield") || texte.contains("raise")) return(true);
        if (texte.contains("phaser")) return(true);
        if (texte.contains("fire") || texte.contains("torpedo")) return(true);
        if (texte.contains("beam me up") || texte.contains("scotty") || texte.contains("transporteur")) return(true);
        if (texte.contains("beam me down")) return(true);
        if (texte.contains("tractor push")) return(true);
        if (texte.contains("tractor off")) return(true);
        if (texte.contains("tractor pull")) return(true);
        if (texte.contains("impulse power")) return(true);
        if (texte.contains("stay here")) return(true);
        if (texte.contains("warp drive")) return(true);
        if (texte.contains("viewer on")) return(true);
        if (texte.contains("local viewer")) return(true);
        if (texte.contains("viewer off")) return(true);
        if (texte.contains("logs console")) return(true);
        if (texte.contains("logs info")) return(true);
        if (texte.contains("system plans")) return(true);
        if (texte.contains("system info")) return(true);
        if (texte.contains("system stat")) return(true);
        if (texte.contains("speak list")) return(true);
        return(false);
    }

    private MediaPlayer soundmedia=null;

    private void playsound(int resourceid) {
        if(soundmedia!=null) {
            soundmedia.release();
        }
        soundmedia = MediaPlayer.create(getBaseContext(), resourceid);
        soundmedia.start(); // no need to call prepare(); create() does that for you
    }


    // ===================================================================================
    // send a message to the other machines

    public void sendtext(String text) {
        // start the client thread
        //say("Send: " + text);
        Thread clientThread = new Thread(new ClientThread(text));
        clientThread.start();
    }

    class ClientThread implements Runnable {

        private Socket clientSocket = null;

        private String mesg;

        public ClientThread(String str) {
            mesg = str;
        }

        @Override
        public void run() {
            if(sendLocal) {
                // send to all other trycorders
                if (mIpList.size() >= 2) {
                    for (int i = 1; i < mIpList.size(); ++i) {
                        clientsend(mIpList.get(i));
                    }
                }
            }
            if(sendRemote) {
                // send to the tryserver machine
                if (debugMode) serversend(debugAddr);
                else serversend("mlsoftlaberge.ddns.net");
            }
        }

        private void serversend(String destip) {
            // try to connect to a socket
            try {
                InetAddress serverAddr = InetAddress.getByName(destip);
                clientSocket = new Socket(serverAddr, SERVERPORT);
                Log.d("clientthread", "server connected " + destip);
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
            }
            // try to send the message
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream())), true);
                out.println(mesg);
                Log.d("clientthread", "data sent: " + mesg);
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
            }
            // try to receive the answer
            try {
                BufferedReader bufinput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String read = bufinput.readLine();
                if (read != null) {
                    mHandler.post(new updateUIThread(read));
                    Log.d("clientthread", "answer received: " + read);
                }
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
            }
            // try to close the socket of the client
            try {
                clientSocket.close();
                Log.d("clientthread", "server closed " + destip);
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
            }
        }

        private void clientsend(String destip) {
            // try to connect to a socket
            try {
                Log.d("clientthread", "try to connect to a server " + destip);
                InetAddress serverAddr = InetAddress.getByName(destip);
                clientSocket = new Socket(serverAddr, SERVERPORT);
                Log.d("clientthread", "server connected " + destip);
            } catch (UnknownHostException e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            }
            // try to send the message
            try {
                Log.d("clientthread", "sending data");
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream())), true);
                out.println(mesg);
                Log.d("clientthread", "data sent: " + mesg);
            } catch (UnknownHostException e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            }
            // try to close the socket of the client
            try {
                Log.d("clientthread", "closing socket");
                clientSocket.close();
                Log.d("clientthread", "socket closed");
            } catch (Exception e) {
                Log.d("clientthread", e.toString());
                e.printStackTrace();
            }
        }

    }

    // ===================================================================================
    // connect with the starship server, announce myself, and wait for orders

    private Thread starshipThread=null;
    private Socket starshipSocket=null;

    public void initstarship() {
        say("Initialize the starship network");
        // start the starship thread
        Thread starshipThread = new Thread(new StarshipThread());
        starshipThread.start();
    }

    public void stopstarship() {
        say("Stop the starship network");
        // stop the server thread
        try {
            starshipThread.interrupt();
        } catch (Exception e) {
            Log.d("stopstarshipthread", e.toString());
        }
        // close the socket of the server
        try {
            starshipSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class StarshipThread implements Runnable {

        String identification="trycorder";

        public StarshipThread() {
            identification="trycorder:"+mFetcher.fetch_device_name();
        }

        @Override
        public void run() {
            // send to the tryserver machine
            if(debugMode) starshipsend(debugAddr);
            else starshipsend("mlsoftlaberge.ddns.net");
        }

        private void starshipsend(String destip) {
            while(!Thread.currentThread().isInterrupted()) {
                // try to connect to a socket
                try {
                    InetAddress serverAddr = InetAddress.getByName(destip);
                    starshipSocket = new Socket(serverAddr, SERVERPORT);
                    Log.d("starshipthread", "server connected " + destip);
                } catch (Exception e) {
                    Log.d("starshipthread", e.toString());
                }
                // try to send the identification
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(starshipSocket.getOutputStream())), true);
                    out.println(identification);
                    Log.d("starshipthread", "data sent: " + identification);
                } catch (Exception e) {
                    Log.d("starshipthread", e.toString());
                }
                // prepare the input stream
                BufferedReader bufinput = null;
                try {
                    bufinput = new BufferedReader(new InputStreamReader(starshipSocket.getInputStream()));
                } catch (Exception e) {
                    Log.d("starshipthread", e.toString());
                }
                // continue to receive all answers from the starship server until it dies
                while (!Thread.currentThread().isInterrupted()) {
                    // try to receive the answer
                    try {
                        String read = bufinput.readLine();
                        if (read != null) {
                            mHandler.post(new updateUIThread(read));
                            Log.d("starshipthread", "answer received: " + read);
                        }
                    } catch (Exception e) {
                        Log.d("starshipthread", e.toString());
                        break;
                    }
                }
                // try to close the socket of the client
                try {
                    starshipSocket.close();
                    Log.d("starshipthread", "server closed " + destip);
                } catch (Exception e) {
                    Log.d("starshipthread", e.toString());
                }

            }
        }

    }

    // =====================================================================================
    // network operations.   ===   Hi Elvis!
    // =====================================================================================

    private ServerSocket serverSocket = null;

    private Thread serverThread = null;

    // initialize the servers
    private void initserver() {
        say("Initialize the network server");
        // create the handler to receive events from communication thread
        //mHandler = new Handler();
        // start the server thread
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    // stop the servers
    private void stopserver() {
        say("Stop the network");
        // stop the server thread
        serverThread.interrupt();
        // close the socket of the server
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ====================================================================================
    // server part

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    Log.d("serverthread", "accepting server socket");
                    saypost("Accepting server socket");
                    socket = serverSocket.accept();
                    Log.d("serverthread", "accepted server socket");
                    saypost("Accepted socket");

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    Log.d("serverthread", "exception " + e.toString());
                    saypost("Exception: "+e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket commSocket;

        private BufferedReader bufinput;

        public CommunicationThread(Socket socket) {

            commSocket = socket;

            try {

                bufinput = new BufferedReader(new InputStreamReader(commSocket.getInputStream()));

            } catch (IOException e) {
                Log.d("commthreadinit", "exception " + e.toString());
                e.printStackTrace();
            }
        }

        public void run() {
            saypost("Receiving text");
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = bufinput.readLine();
                    if (read == null) break;
                    Log.d("commthreadrun", "update conversation");
                    mHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    Log.d("commthreadrun", "exception " + e.toString());
                    e.printStackTrace();
                }
            }
        }

    }

    // ===== thread used to update the ui of the running application with received text =====
    class updateUIThread implements Runnable {
        private String msg = null;

        public updateUIThread(String str) {
            msg = str;
            Log.d("uithread", str);
        }

        @Override
        public void run() {
            if (msg != null) {
                displaytext(msg);
            }
        }
    }

    public void displaytext(String msg) {
        if(msg.contains("trycorders:")) {
            playsound(R.raw.computerbeep_29);
            return;
        }
        if(msg.contains("server ok")) {
            playsound(R.raw.computerbeep_39);
            return;
        }
        if(msg.contains("red alert")) {
            playsound(R.raw.tng_red_alert1);
            speak(msg);
            return;
        }
        if(msg.contains("yellow alert")) {
            playsound(R.raw.alert15);
            speak(msg);
            return;
        }
        if(matchvoice(msg)==false) {
            speak(msg);
        }
        informActivity("text", msg);
    }


    // =====================================================================================
    // voice capture and send on udp

    private int RECORDING_RATE = 44100;
    private int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);

    private AudioRecord recorder;

    private boolean currentlySendingAudio = false;

    public void startstreamingaudio() {
        currentlySendingAudio = true;
        startStreaming();
    }

    public void stopstreamingaudio() {
        currentlySendingAudio = false;
        try {
            recorder.release();
        } catch (Exception e) {
            Log.d("stop streaming", "stop streaming error");
        }
    }

    private void startStreaming() {

        Log.i("startstreaming", "Starting the background thread to stream the audio data");

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    Log.d("streamaudio", "Obtaining server address");
                    String SERVER;
                    int PORT = SERVERPORT;

                    Log.d("streamaudio", "Creating the datagram socket");
                    DatagramSocket socket = new DatagramSocket();

                    Log.d("streamaudio", "Creating the buffer of size " + BUFFER_SIZE);
                    byte[] buffer = new byte[BUFFER_SIZE];

                    List<InetAddress> mServerAddress = new ArrayList<>();
                    mServerAddress.clear();
                    for (int i = 0; i < mIpList.size(); ++i) {
                        SERVER = mIpList.get(i);
                        Log.d("streamaudio", "Connecting to " + SERVER + ":" + PORT);
                        mServerAddress.add(InetAddress.getByName(SERVER));
                        Log.d("streamaudio", "Connected to " + SERVER + ":" + PORT);
                    }

                    Log.d("streamaudio", "Creating the reuseable DatagramPacket");
                    DatagramPacket packet;

                    Log.d("streamaudio", "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);

                    Log.d("streamaudio", "AudioRecord recording...");
                    recorder.startRecording();

                    while (currentlySendingAudio == true) {

                        Log.d("streamloop", "Reading data from recorder");
                        // read the data into the buffer
                        int read = recorder.read(buffer, 0, buffer.length);

                        // repeat to myself if i am alone on the net only
                        int j;
                        //if (mIpList.size() < 2) j = 0;
                        //else j = 1;
                        j=1;
                        // repeat to each other address from list
                        if(mIpList.size()>1) for (int i = j; i < mIpList.size(); ++i) {
                            // place contents of buffer into the packet
                            packet = new DatagramPacket(buffer, read, mServerAddress.get(i), PORT);

                            Log.d("streamloop", "Sending packet : " + read + " to " + mIpList.get(i));
                            // send the packet
                            socket.send(packet);
                        }
                    }

                    Log.d("streamaudio", "AudioRecord finished recording");

                } catch (Exception e) {
                    Log.e("streamaudio", "Exception: " + e);
                }
            }
        });

        // start the thread
        streamThread.start();
    }


    // =====================================================================================
    // voice receive on udp and playback
    private Thread talkServerThread = null;

    private void inittalkserver() {
        say("Start talk server thread");
        if(talkServerThread==null) {
            talkServerThread = new Thread(new TalkServerThread());
            talkServerThread.start();
        }
    }

    private void stoptalkserver() {
        say("Stop the talk server");
        // stop the server thread
        try {
            talkServerThread.interrupt();
        } catch (Exception e) {
            say("cant stop talk server thread");
        }
        talkServerThread=null;
    }

    class TalkServerThread implements Runnable {

        private int RECORDING_RATE = 44100;
        private int CHANNEL = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        private int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

        private int PORT = 1701;

        private int bufferSize = 10000;

        public void run() {

            DatagramSocket talkServerSocket;
            try {
                // create the server socket
                talkServerSocket = new DatagramSocket(null);
                talkServerSocket.setReuseAddress(true);
                talkServerSocket.bind(new InetSocketAddress(PORT));
                Log.d("talkserver", "socket created");
            } catch (Exception e) {
                Log.d("talkserver","talkserverthread Cant create socket");
                return;
            }

            byte[] receiveData = new byte[bufferSize];

            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                    RECORDING_RATE, CHANNEL, FORMAT, bufferSize,
                    AudioTrack.MODE_STREAM);
            track.play();

            long systime;
            long lasttime;
            lasttime=0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Log.d("talkloop", "ready to receive " + bufferSize);
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    talkServerSocket.receive(receivePacket);
                    byte[] buffer = receivePacket.getData();
                    int offset = receivePacket.getOffset();
                    int len = receivePacket.getLength();
                    systime=System.currentTimeMillis();
                    if((systime-lasttime)>1000) saypost("Speaking ...");
                    lasttime=systime;
                    Log.d("talkloop", "received bytes : " + offset + " - " + len);
                    track.write(buffer, offset, len);
                } catch (Exception e) {
                    Log.d("talkloop", "exception: " + e);
                }

            }

            track.flush();
            track.release();
            talkServerSocket.close();

        }

    }

    // =====================================================================================
    // register my service with NSD

    private String mServiceName=null;
    private NsdManager mNsdManager=null;
    private NsdManager.RegistrationListener mRegistrationListener=null;
    private String SERVICE_TYPE = "_http._tcp.";
    private String SERVICE_NAME = "Trycorder";

    public void registerService() {
        if (deviceName.isEmpty()) deviceName = SERVICE_NAME;

        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(deviceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(SERVERPORT);

        say("Register service");
        initializeRegistrationListener();

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void unregisterService() {
        try {
            mNsdManager.unregisterService(mRegistrationListener);
        } catch (Exception e) {
            Log.d("unregisterservice","Error "+e);
        }
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
                Log.d("registration", "Registration done: " + mServiceName);
                saypost("Registration done: " + mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.d("registration", "Registration failed: " + mServiceName);
                saypost("Registration failed: " + mServiceName);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d("registration", "Unregistration done");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                Log.d("registration", "Unregistration failed");
            }
        };
    }



    // =======================================================================================
    // discovery section

    private NsdManager.DiscoveryListener mDiscoveryListener=null;
    private NsdManager.ResolveListener mResolveListener=null;

    private List<String> mIpList = new ArrayList<String>();
    private List<String> mNameList = new ArrayList<String>();

    public void startdiscoverService() {
        if (deviceName.isEmpty()) deviceName = SERVICE_NAME;

        mIpList.clear();
        mIpList.add(mFetcher.fetch_ip_address());
        mNameList.clear();
        mNameList.add(deviceName);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        say("Discover services");
        initializeResolveListener();

        initializeDiscoveryListener();

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

    }

    public void stopdiscoverService() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                //Log.e("discovery", "Resolve failed: " + errorCode);
                //saypost("Resolve failed: " + serviceInfo.getServiceName() + " Err:" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                //Log.e("discovery", "Resolve Succeeded: " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    //Log.d("discovery", "Same IP.");
                    //saypost("Local machine " + mServiceName);
                    //return;
                }
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
                //Log.d("discovery", "Host: " + host.toString() + " Port: " + port);
                //saypost("Resolved " + serviceInfo.getServiceName() +
                //        " Host: " + host.toString() + " Port: " + port);
                StringBuffer str = new StringBuffer(host.toString());
                addiplist(str.substring(1), serviceInfo.getServiceName());
            }
        };
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                //Log.d("discovery", "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                //Log.d("discovery", "Service discovery success: " + service);
                //saypost("Service discovered: " + service.getServiceName());
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    //Log.d("discovery", "Unknown Service Type: " + service.getServiceType());
                } else {
                    if (service.getServiceName().equals(mServiceName)) {
                        // The name of the service tells the user what they'd be
                        // connecting to. It could be "Bob's Chat App".
                        //Log.d("discovery", "Same machine: " + mServiceName);
                        return;
                    }
                    //Log.d("discovery", "Resolved service: " + service.getServiceName());
                    //Log.d("discovery", "Resolved service: " + service.getHost());  // empty
                    //Log.d("discovery", "Resolved service: " + service.getPort());  // empty
                    try {
                        mNsdManager.resolveService(service, mResolveListener);
                    } catch (Exception e) {
                        //Log.d("discovery", "resolve error: " + e.toString());
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                //Log.e("discovery", "service lost: " + service);
                //saypost("Lost: " + service.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                //Log.i("discovery", "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                //Log.e("discovery", "Start Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                //Log.e("discovery", "Stop Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void addiplist(String ip, String name) {
        for (int i = 0; i < mIpList.size(); ++i) {
            if (ip.equals(mIpList.get(i))) {
                listpost();
                return;
            }
        }
        // replace the \032 on android 4.4.4 by a blank space
        String newname = name.replaceFirst("032"," ").replace('\\',' ');
        mIpList.add(ip);
        mNameList.add(newname);
        listpost();
        saypost("Added "+ip+" - "+newname);
        informActivity("iplist",ip);
        Toast.makeText(this, "New IP found: "+ip, Toast.LENGTH_LONG).show();
    }

    public void listpost() {
        mHandler.post(new listThread());
    }

    // tread to update the ui
    class listThread implements Runnable {

        public listThread() {
        }

        @Override
        public void run() {
            saylist();
        }
    }

    public void saylist() {
        StringBuffer str = new StringBuffer("");
        for (int i = 0; i < mIpList.size(); ++i) {
            str.append(mIpList.get(i) + " - " + mNameList.get(i) + "\n");
        }
        updateNotification(str.toString());
    }

    // post something to say on the main thread (from a secondary thread)
    public void saypost(String str) {
        mHandler.post(new sayThread(str));
    }

    // tread to update the ui
    class sayThread implements Runnable {
        private String msg;

        public sayThread(String str) {
            msg = str;
            //Log.d("saythread", str);
        }

        @Override
        public void run() {
            if (msg != null) {
                say(msg);
            }
        }
    }

    // pass a message to the user in the appropriate way depending on  ...
    private void say(String msg) {
        informActivity("say",msg);
        //Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    private void informActivity(String cmd, String text) {
        intentForActivity.putExtra("TRYSERVERCMD", cmd);
        intentForActivity.putExtra("TRYSERVERTEXT", text);
        sendBroadcast(intentForActivity);
    }

}
