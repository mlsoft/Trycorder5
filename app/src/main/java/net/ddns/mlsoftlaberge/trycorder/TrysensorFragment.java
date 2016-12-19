package net.ddns.mlsoftlaberge.trycorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ddns.mlsoftlaberge.trycorder.settings.SettingsActivity;
import net.ddns.mlsoftlaberge.trycorder.trycorder.AudSensorView;
import net.ddns.mlsoftlaberge.trycorder.trycorder.GraSensorView;
import net.ddns.mlsoftlaberge.trycorder.trycorder.MagSensorView;
import net.ddns.mlsoftlaberge.trycorder.trycorder.OriSensorView;
import net.ddns.mlsoftlaberge.trycorder.trycorder.TemSensorView;
import net.ddns.mlsoftlaberge.trycorder.utils.Fetcher;

import java.io.IOException;


/**
 * Created by mlsoft on 16-06-26.
 */
public class TrysensorFragment extends Fragment
        implements TextureView.SurfaceTextureListener {



    public TrysensorFragment() {
    }

    // ======================================================================================
    public interface OnTrysensorInteractionListener {
        public void onTrysensorModeChange(int mode);
    }

    private OnTrysensorInteractionListener mOnTrysensorInteractionListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement.
            mOnTrysensorInteractionListener = (OnTrysensorInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTrysensorInteractionListener");
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
    private Button mSendButton;

    // the main contents layout in the center
    private LinearLayout mCenterLayout;

    private LinearLayout mSensor1Layout;
    private LinearLayout mSensor2Layout;
    private LinearLayout mSensor3Layout;
    private LinearLayout mSensor4Layout;
    private LinearLayout mSensor5Layout;
    private LinearLayout mSensor6Layout;


    // handle for the gps
    private LocationManager mLocationManager = null;

    // the handle to the sensors
    private SensorManager mSensorManager = null;

    // the new scope class
    private MagSensorView mMagSensorView;

    // the new scope class
    private OriSensorView mOriSensorView;

    // the new scope class
    private GraSensorView mGraSensorView;

    // the new scope class
    private TemSensorView mTemSensorView;

    // the new scope class
    private AudSensorView mAudSensorView;

    // a texture to view camera
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
        View view = inflater.inflate(R.layout.trysensor_fragment, container, false);

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
                switchtryviewermode(1);
            }
        });

        // the sound-effect button
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryviewermode(1);
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
                switchtryviewermode(1);
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
        mSendButton = (Button) view.findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
            }
        });

        mTextstatus_bottom = (TextView) view.findViewById(R.id.textstatus_bottom);
        mTextstatus_bottom.setText("Ready");

        // =========================== center content area ============================

        // the center layout to show contents
        mCenterLayout = (LinearLayout) view.findViewById(R.id.center_layout);

        mSensor1Layout = (LinearLayout) view.findViewById(R.id.sensor1_layout);
        mSensor2Layout = (LinearLayout) view.findViewById(R.id.sensor2_layout);
        mSensor3Layout = (LinearLayout) view.findViewById(R.id.sensor3_layout);
        mSensor4Layout = (LinearLayout) view.findViewById(R.id.sensor4_layout);
        mSensor5Layout = (LinearLayout) view.findViewById(R.id.sensor5_layout);
        mSensor6Layout = (LinearLayout) view.findViewById(R.id.sensor6_layout);

        // ==============================================================================
        // create layout params for the created views
        final LinearLayout.LayoutParams tlayoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);

        // ==============================================================================
        // a sensor manager to obtain sensors data
        mSensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);

        // a gps manager to obtain gps data
        mLocationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);

        // ============== create a sensor display and incorporate in layout ==============

        // my sensorview that display the sensors data
        mMagSensorView = new MagSensorView(getContext(), mSensorManager);
        // add my sensorview to the layout 1
        mSensor1Layout.addView(mMagSensorView, tlayoutParams);

        // my sensorview that display the sensors data
        mOriSensorView = new OriSensorView(getContext(), mSensorManager, mLocationManager);
        mOriSensorView.setClickable(true);
        mOriSensorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googlemapactivity();
                buttonsound();
            }
        });
        // add my sensorview to the layout 2
        mSensor2Layout.addView(mOriSensorView, tlayoutParams);

        // my sensorview that display the sensors data
        mGraSensorView = new GraSensorView(getContext(), mSensorManager);
        // add my sensorview to the layout 1
        mSensor3Layout.addView(mGraSensorView, tlayoutParams);

        // my sensorview that display the sensors data
        mTemSensorView = new TemSensorView(getContext(), mSensorManager);
        // add my sensorview to the layout 1
        mSensor4Layout.addView(mTemSensorView, tlayoutParams);

        // my sensorview that display the sensors data
        mAudSensorView = new AudSensorView(getContext());
        // add my sensorview to the layout 1
        mSensor5Layout.addView(mAudSensorView, tlayoutParams);

        // create and activate a textureview to contain camera display
        mViewerWindow = new TextureView(getContext());
        mViewerWindow.setSurfaceTextureListener(this);
        // add my sensorview to the layout 1
        mSensor6Layout.addView(mViewerWindow, tlayoutParams);


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
        mSendButton.setTypeface(face2);
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
        mMagSensorView.start();
        mGraSensorView.start();
        mAudSensorView.start();
        mOriSensorView.start();
        mTemSensorView.start();
    }

    @Override
    public void onPause() {
        // save the current status
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pref_key_run_status", mRunStatus);
        editor.commit();
        // stop the sensors
        mMagSensorView.stop();
        mGraSensorView.stop();
        mAudSensorView.stop();
        mOriSensorView.stop();
        mTemSensorView.stop();
        super.onPause();
    }

    // ask the activity to switch to another fragment
    private void switchtryviewermode(int mode) {
        mOnTrysensorInteractionListener.onTrysensorModeChange(mode);
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

    // =========================================================================================
    // map activity to see where we are on the map of this planet

    public void googlemapactivity() {
        float longitude = mOriSensorView.getLongitude();
        float latitude = mOriSensorView.getLatitude();

        //final Intent viewIntent = new Intent(Intent.ACTION_VIEW, constructGeoUri(view.getContentDescription().toString()));
        String geopath = "geo:" + String.valueOf(latitude) + "," + String.valueOf(longitude);
        Uri geouri = Uri.parse(geopath);
        say("Open planetary mapping");
        say(geopath);
        final Intent viewIntent = new Intent(Intent.ACTION_VIEW, geouri);
        // A PackageManager instance is needed to verify that there's a default app
        // that handles ACTION_VIEW and a geo Uri.
        final PackageManager packageManager = getActivity().getPackageManager();
        // Checks for an activity that can handle this intent. Preferred in this
        // case over Intent.createChooser() as it will still let the user choose
        // a default (or use a previously set default) for geo Uris.
        if (packageManager.resolveActivity(
                viewIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            // Toast.makeText(getActivity(),
            //        R.string.yes_intent_found, Toast.LENGTH_SHORT).show();
            startActivity(viewIntent);
        } else {
            // If no default is found, displays a message that no activity can handle
            // the view button.
            Toast.makeText(getActivity(), "No application for mapping.", Toast.LENGTH_SHORT).show();
        }
    }

    // ========================================================================================
    // functions to listen to the surface texture view

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            mCamera.setPreviewTexture(mViewerWindow.getSurfaceTexture());
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.set("orientation", "portrait");
            parameters.set("scene-mode", "portrait");
            parameters.set("rotation", "270");
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException ioe) {
            say("Something bad happened with camera");
        } catch (Exception sex) {
            say("camera permission refused");
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
