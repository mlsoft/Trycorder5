package net.ddns.mlsoftlaberge.trycorder.tryclient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ddns.mlsoftlaberge.trycorder.R;
import net.ddns.mlsoftlaberge.trycorder.TrycorderService;
import net.ddns.mlsoftlaberge.trycorder.settings.SettingsActivity;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by mlsoft on 16-06-26.
 */
public class TryclientFragment extends Fragment {

    public TryclientFragment() {
    }

    // ======================================================================================
    public interface OnTryclientInteractionListener {
        public void onTryclientModeChange(int mode);
    }

    private OnTryclientInteractionListener mOnTryclientInteractionListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement.
            mOnTryclientInteractionListener = (OnTryclientInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTryclientInteractionListener");
        }
    }

    // ======================================================================================

    // the button to talk to computer
    private ImageButton mBacktopButton;

    // the button for sound activation
    private Button mBackButton;

    private boolean mRunStatus = false;

    // the button for settings
    private Button mSettingsButton;

    // the one status line
    private TextView mTextstatus_top;
    private TextView mTextstatus_bottom;

    // the button to talk to computer
    private ImageButton mBackbottomButton;

    // the button to stop it all
    private Button mViewButton;

    // the button for settings
    private Button mSendButton;

    // in the bottom layout
    private TextView mLogsConsole;

    // utility class to fetch system infos
    private Fetcher mFetcher;

    // the walkie layout on sensor screen
    private LinearLayout mWalkieLayout;
    private Button mWalkieSpeakButton;
    private Button mWalkieTalkButton;
    private Button mWalkieScanButton;
    private Button mWalkieServeronButton;
    private Button mWalkieServeroffButton;
    private TextView mWalkieIpList;

    // the preferences holder
    private SharedPreferences sharedPref;

    // the preferences values
    private boolean autoListen;
    private boolean isChatty;
    private String speakLanguage;
    private String listenLanguage;
    private String displayLanguage;
    private String deviceName;
    private boolean isMaster;
    private boolean replaySent;
    private boolean autoBoot;
    private boolean autoStop;

    // list obtained from the trycorder server
    private List<String> mIpList = new ArrayList<String>();
    private List<String> mNameList = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tryclient_fragment, container, false);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        autoListen = sharedPref.getBoolean("pref_key_auto_listen", false);
        isChatty = sharedPref.getBoolean("pref_key_ischatty", false);
        speakLanguage = sharedPref.getString("pref_key_speak_language", "");
        listenLanguage = sharedPref.getString("pref_key_listen_language", "");
        displayLanguage = sharedPref.getString("pref_key_display_language", "");
        deviceName = sharedPref.getString("pref_key_device_name", "");
        isMaster = sharedPref.getBoolean("pref_key_ismaster", true);
        replaySent = sharedPref.getBoolean("pref_key_replay_sent", false);
        autoBoot = sharedPref.getBoolean("pref_key_auto_boot", true);
        autoStop = sharedPref.getBoolean("pref_key_auto_stop", false);

        // ===================== top horizontal button grid ==========================
        // the start button
        mBacktopButton = (ImageButton) view.findViewById(R.id.backtop_button);
        mBacktopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryclientmode(1);
            }
        });

        // the sound-effect button
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryclientmode(1);
            }
        });

        // the settings button
        mSettingsButton = (Button) view.findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                settingsactivity();
            }
        });

        mTextstatus_top = (TextView) view.findViewById(R.id.textstatus_top);

        // ===================== bottom horizontal button grid ==========================
        // the ask button
        mBackbottomButton = (ImageButton) view.findViewById(R.id.backbottom_button);
        mBackbottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryclientmode(1);
            }
        });

        // the stop button
        mViewButton = (Button) view.findViewById(R.id.view_button);
        mViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
            }
        });

        // the settings button
        mSendButton = (Button) view.findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
            }
        });

        mTextstatus_bottom = (TextView) view.findViewById(R.id.textstatus_bottom);
        mTextstatus_bottom.setText("Ready");

        // in the bottom layout
        mLogsConsole = (TextView) view.findViewById(R.id.logs_console);

        // utility class to fetch infos from the system
        mFetcher = new Fetcher(getContext());

        // ==================== top main action buttons ========================

        // position 12 of sensor layout
        mWalkieLayout = (LinearLayout) view.findViewById(R.id.walkie_layout);

        mWalkieSpeakButton = (Button) view.findViewById(R.id.walkie_speak_button);
        mWalkieSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listen();
            }
        });

        mWalkieTalkButton = (Button) view.findViewById(R.id.walkie_talk_button);
        mWalkieTalkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startstreamingaudio();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopstreamingaudio();
                        break;
                }
                return false;
            }
        });

        mWalkieScanButton = (Button) view.findViewById(R.id.walkie_scan_button);
        mWalkieScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                askscanlist();
            }
        });

        mWalkieServeronButton = (Button) view.findViewById(R.id.walkie_serveron_button);
        mWalkieServeronButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                startTrycorderService();
                bindTrycorderService();
            }
        });

        mWalkieServeroffButton = (Button) view.findViewById(R.id.walkie_serveroff_button);
        mWalkieServeroffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                unbindTrycorderService();
                stopTrycorderService();
            }
        });

        // the list of ip - machine that we discover
        mWalkieIpList = (TextView) view.findViewById(R.id.walkie_iplist);
        mWalkieIpList.setHorizontallyScrolling(true);
        mWalkieIpList.setMovementMethod(new ScrollingMovementMethod());

        // fill the list with at least our private IP until some events fill it more
        mWalkieIpList.setText(mFetcher.fetch_ip_address());

        return view;

    }

    // setup the fonts on every text-containing widgets
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Typeface face = Typeface.createFromAsset(getActivity().getAssets(), "sonysketchef.ttf");
        Typeface face2 = Typeface.createFromAsset(getActivity().getAssets(), "finalold.ttf");
        Typeface face3 = Typeface.createFromAsset(getActivity().getAssets(), "finalnew.ttf");
        // top buttons
        mBackButton.setTypeface(face2);
        mSettingsButton.setTypeface(face2);
        mTextstatus_top.setTypeface(face);
        // bottom buttons
        mViewButton.setTypeface(face2);
        mSendButton.setTypeface(face2);
        mTextstatus_bottom.setTypeface(face3);
        // top layout
        mWalkieSpeakButton.setTypeface(face2);
        mWalkieTalkButton.setTypeface(face2);
        mWalkieScanButton.setTypeface(face2);
        mWalkieServeronButton.setTypeface(face2);
        mWalkieServeroffButton.setTypeface(face2);
        mWalkieIpList.setTypeface(face3);
        // bottom layout
        mLogsConsole.setTypeface(face2);
    }
    // =====================================================================================

    private boolean mBound=false;
    private TrycorderService mTrycorderService;
    private TrycorderService.TryBinder mTryBinder;

    @Override
    public void onStart() {
        super.onStart();
        //if (!autoBoot) startTrycorderService();
        bindTrycorderService();
    }

    public void bindTrycorderService() {
        // Bind to Service
        Intent intent = new Intent(getActivity(), TrycorderService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindTrycorderService();
        //if(autoStop) stopTrycorderService();
    }

    public void unbindTrycorderService() {
        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mTryBinder = (TrycorderService.TryBinder) service;
            mTrycorderService = mTryBinder.getService();
            mBound = true;
            askscanlist();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    // ask the service to give us the list of ip/names

    public void askscanlist() {
        if(mBound) {
            mIpList = mTrycorderService.getiplist();
            mNameList = mTrycorderService.getnamelist();
            saylist();
        }
    }

    public void saylist() {
        StringBuffer str = new StringBuffer("");
        for (int i = 0; i < mIpList.size(); ++i) {
            str.append(mIpList.get(i) + " - " + mNameList.get(i) + "\n");
        }
        mWalkieIpList.setText(str.toString());
    }

    // ===================================================================================

    private void startTrycorderService() {
        say("Start Trycorder Service");
        try {
            getActivity().startService(new Intent(getContext(), TrycorderService.class));
        } catch (Exception e) {
            say("Cant start trycorder service");
        }
    }

    private void stopTrycorderService() {
        say("Stop Trycorder Service");
        try {
            getActivity().stopService(new Intent(getContext(), TrycorderService.class));
        } catch (Exception e) {
            say("Cant stop trycorder service");
        }
    }

    // =====================================================================================

    @Override
    public void onResume() {
        super.onResume();
        // settings part of the preferences
        autoListen = sharedPref.getBoolean("pref_key_auto_listen", false);
        isChatty = sharedPref.getBoolean("pref_key_ischatty", false);
        speakLanguage = sharedPref.getString("pref_key_speak_language", "");
        listenLanguage = sharedPref.getString("pref_key_listen_language", "");
        displayLanguage = sharedPref.getString("pref_key_display_language", "");
        deviceName = sharedPref.getString("pref_key_device_name", "");
        isMaster = sharedPref.getBoolean("pref_key_ismaster", true);
        replaySent = sharedPref.getBoolean("pref_key_replay_sent", false);
        autoBoot = sharedPref.getBoolean("pref_key_auto_boot", true);
        autoStop = sharedPref.getBoolean("pref_key_auto_stop", false);
        // dynamic status part
        mRunStatus = sharedPref.getBoolean("pref_key_run_status", false);
        // start the speak server
        //initspeak();
        // start the network listener server
        //initserver();
        // start the service if not started
        //if(!autoBoot) startTrycorderService();
    }

    @Override
    public void onPause() {
        // save the current status
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pref_key_run_status", mRunStatus);
        editor.commit();
        // stop the listener server
        //stopserver();
        // stop the service when needed
        //if(autoStop) stopTrycorderService();
        super.onPause();
    }

    // ask the activity to switch to another fragment
    private void switchtryclientmode(int mode) {
        mOnTryclientInteractionListener.onTryclientModeChange(mode);
    }

    // beep
    private void buttonsound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity().getBaseContext(), R.raw.keyok2);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    // booooop
    private void buttonbad() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity().getBaseContext(), R.raw.denybeep1);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    // settings activity incorporation in the display
    public void settingsactivity() {
        say("Settings");
        if (isChatty) speak("Settings");
        Intent i = new Intent(getActivity(), SettingsActivity.class);
        startActivity(i);
    }

    // =========================================================================
    // tell something in the bottom status line
    private StringBuffer logbuffer = new StringBuffer(500);

    public void say(String texte) {
        mTextstatus_bottom.setText(texte);
        logbuffer.insert(0, texte + "\n");
        mLogsConsole.setText(logbuffer);
    }

    // =====================================================================================
    // voice capture and send on udp

    private void startstreamingaudio() {
        say("start streaming audio");
        mTrycorderService.startstreamingaudio();
    }

    private void stopstreamingaudio() {
        say("stop streaming audio");
        mTrycorderService.stopstreamingaudio();
    }

    // =========================================================================
    // usage of text-to-speech to speak a sentence
    // by the trycorderservice

    public void setspeaklang(String lng) {
        mTrycorderService.setspeaklang(lng);
    }

    public void speak(String texte) {
        mTrycorderService.speak(texte);
    }

    public void speak(String texte, String lng) {
        mTrycorderService.speak(texte,lng);
    }


    // ========================================================================================
    // functions to control the speech to text

    // ask the service to listen to the mic and return what is understood
    private void listen() {
        mTextstatus_top.setText("");
        mTrycorderService.listen();
    }

    // receiver of the broadcast from service with understood text from mic
    public void understood(String text) {
        mTextstatus_top.setText(text);
        if(matchvoice(text)) {
            say("Said: " + text);
            sendtext(text);
            if(autoListen) listen();
            return;
        }
        say("Understood: " + text);
        sendtext(text);
        if(autoListen) listen();
    }

    private boolean matchvoice(String textein) {
        String texte = textein.toLowerCase();
        if (texte.contains("french") || texte.contains("français")) {
            listenLanguage = "FR";
            speakLanguage = "FR";
            speak("français",speakLanguage);
            return (true);
        }
        if (texte.contains("english") || texte.contains("anglais")) {
            listenLanguage = "EN";
            speakLanguage = "EN";
            speak("english",speakLanguage);
            return (true);
        }
        return (false);
    }


    public void displaytext(String msg) {
        mTextstatus_top.setText(msg);
        say("Received: " + msg);
        if(matchvoice(msg)==false) {
            speak(msg);
        }
    }


    // ====================================================================================
    // client part

    // send a message to the other
    private void sendtext(String text) {
        // start the client thread
        say("Send: " + text);
        mTrycorderService.sendtext(text);
    }


    // =====================================================================================



}
