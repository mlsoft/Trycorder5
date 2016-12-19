package net.ddns.mlsoftlaberge.trycorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ddns.mlsoftlaberge.trycorder.settings.SettingsActivity;
import net.ddns.mlsoftlaberge.trycorder.utils.Fetcher;

import java.io.IOException;


/**
 * Created by mlsoft on 16-06-26.
 */
public class TryvisionFragment extends Fragment
        implements TextureView.SurfaceTextureListener {

    private int mVisionMode=1;

    public TryvisionFragment() {
    }

    public void setmode(int mode) {
        mVisionMode=mode;
    }
    // ======================================================================================
    public interface OnTryvisionInteractionListener {
        public void onTryvisionModeChange(int mode);
    }

    private OnTryvisionInteractionListener mOnTryvisionInteractionListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement.
            mOnTryvisionInteractionListener = (OnTryvisionInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTryvisionInteractionListener");
        }
    }

    // ======================================================================================

    private boolean mRunStatus = false;

    // the button to talk to computer
    private ImageButton mBacktopButton;

    // the button for sound activation
    private Button mBackButton;

    // the button for settings
    private Button mSettingsButton;

    // the one status line
    private TextView mTextstatus_top;

    // bottom part
    private TextView mTextstatus_bottom;

    // the button to talk to computer
    private ImageButton mBackbottomButton;

    // the button to stop it all
    private Button mSpeakButton;

    // the button for settings
    private Button mTalkButton;

    // the main contents layout in the center
    private LinearLayout mCenterLayout;

    private LinearLayout mSensor1Layout;

    // a texture to view vision
    private TextureView mViewerWindow;
    private Camera mCamera=null;

    // utility class to fetch system infos
    private Fetcher mFetcher;

    // the preferences holder
    private SharedPreferences sharedPref;

    // the preferences values
    private boolean autoListen;
    private boolean isChatty;
    private String speakLanguage;
    private String listenLanguage;
    private String displayLanguage;
    private String deviceName;
    private boolean replaySent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tryvision_fragment, container, false);

        // utility class to fetch infos from the system
        mFetcher = new Fetcher(getContext());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        autoListen = sharedPref.getBoolean("pref_key_auto_listen", false);
        isChatty = sharedPref.getBoolean("pref_key_ischatty", false);
        speakLanguage = sharedPref.getString("pref_key_speak_language", "");
        listenLanguage = sharedPref.getString("pref_key_listen_language", "");
        displayLanguage = sharedPref.getString("pref_key_display_language", "");
        deviceName = sharedPref.getString("pref_key_device_name", "");
        replaySent = sharedPref.getBoolean("pref_key_replay_sent", true);

        // ===================== top horizontal button grid ==========================
        // the start button
        mBacktopButton = (ImageButton) view.findViewById(R.id.backtop_button);
        mBacktopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryvisionmode(1);
            }
        });

        // the sound-effect button
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryvisionmode(1);
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
                switchtryvisionmode(1);
            }
        });

        // the stop button
        mSpeakButton = (Button) view.findViewById(R.id.speak_button);
        mSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listen();
            }
        });

        // the settings button
        mTalkButton = (Button) view.findViewById(R.id.talk_button);
        mTalkButton.setOnTouchListener(new View.OnTouchListener() {
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


        mTextstatus_bottom = (TextView) view.findViewById(R.id.textstatus_bottom);
        mTextstatus_bottom.setText("Ready");

        // =========================== center content area ============================

        // the center layout to show contents
        mCenterLayout = (LinearLayout) view.findViewById(R.id.center_layout);

        mSensor1Layout = (LinearLayout) view.findViewById(R.id.sensor1_layout);

        // ==============================================================================
        // create layout params for the created views
        final LinearLayout.LayoutParams tlayoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);

        // create and activate a textureview to contain vision display
        mViewerWindow = new TextureView(getContext());
        mViewerWindow.setSurfaceTextureListener(this);
        // add my sensorview to the layout 1
        mSensor1Layout.addView(mViewerWindow, tlayoutParams);
        mSensor1Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                rotatecam();
            }
        });


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
        mSpeakButton.setTypeface(face2);
        mTalkButton.setTypeface(face2);
        mTextstatus_bottom.setTypeface(face3);
    }

    // =====================================================================================

    private boolean mBound=false;
    private TrycorderService mTrycorderService;
    private TrycorderService.TryBinder mTryBinder;

    @Override
    public void onStart() {
        super.onStart();
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
            //mIpList = mTrycorderService.getiplist();
            //mNameList = mTrycorderService.getnamelist();
            //saylist();
        }
    }


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
        replaySent = sharedPref.getBoolean("pref_key_replay_sent", true);
        // dynamic status part
        mRunStatus = sharedPref.getBoolean("pref_key_run_status", false);
        // override the languages for french
        speakLanguage = "FR";
        listenLanguage = "FR";
        // start the sensors
        mRunStatus=true;
    }

    @Override
    public void onPause() {
        // save the current status
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pref_key_run_status", mRunStatus);
        editor.commit();
        // stop the sensors
        super.onPause();
    }

    // ask the activity to switch to another fragment
    private void switchtryvisionmode(int mode) {
        mOnTryvisionInteractionListener.onTryvisionModeChange(mode);
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
        Intent i = new Intent(getActivity(), SettingsActivity.class);
        startActivity(i);
    }

    // ========================================================================================
    // functions to listen to the surface texture view

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        switchcam(mVisionMode);
    }

    private void rotatecam() {
        mVisionMode++;
        if(mVisionMode>9) {
            mVisionMode=1;
        }
        switchcam(mVisionMode);
    }

    private void switchcam(int mode) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCamera.setPreviewTexture(mViewerWindow.getSurfaceTexture());
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.set("orientation", "portrait");
            parameters.set("scene-mode", "portrait");
            parameters.set("rotation", "90");
            switch(mode) {
                case 1:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
                    break;
                case 2:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                    break;
                case 3:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
                    break;
                case 4:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_AQUA);
                    break;
                case 5:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
                    break;
                case 6:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_POSTERIZE);
                    break;
                case 7:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                    break;
                case 8:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
                    break;
                case 9:
                    // night vision parameters
                    parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
                    break;
            }
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException ioe) {
            say("Something bad happened with vision");
        } catch (Exception sex) {
            say("vision permission refused");
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    // =========================================================================
    // tell something in the bottom status line

    private void say(String texte) {
        mTextstatus_bottom.setText(texte);
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
            return;
        }
        say("Understood: " + text);
    }

    // ==============================================================================
    // accents resource : éèêë áàâä íìîï óòôö úùûü çÇ
    // ==============================================================================
    private boolean matchvoice(String textein) {
        String texte = textein.toLowerCase();
        if (texte.contains("french") || texte.contains("français")) {
            listenLanguage = "FR";
            speakLanguage = "FR";
            speak("français", speakLanguage);
            return (true);
        }
        if (texte.contains("english") || texte.contains("anglais")) {
            listenLanguage = "EN";
            speakLanguage = "EN";
            speak("english", speakLanguage);
            return (true);
        }
        if (texte.contains("martin") || texte.contains("master")) {
            if (speakLanguage.equals("FR")) speak("Martin est mon maître.");
            else speak("Martin is my Master.");
            return (true);
        }
        if (texte.contains("computer") || texte.contains("ordinateur")) {
            if (speakLanguage.equals("FR")) speak("Faites votre requète");
            else speak("State your question");
            return (true);
        }
        if (texte.contains("fuck") || texte.contains("shit")) {
            if (speakLanguage.equals("FR")) speak("Ce n'est pas très poli");
            else speak("This is not very polite.");
            return (true);
        }

        return(false);
    }

}
