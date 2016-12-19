package net.ddns.mlsoftlaberge.trycorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import net.ddns.mlsoftlaberge.trycorder.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by mlsoft on 16-06-20.
 */
public class TryviewerFragment extends Fragment {

    public TryviewerFragment() {
    }

    // ======================================================================================
    public interface OnTryviewerInteractionListener {
        public void onTryviewerModeChange(int mode);
    }

    private OnTryviewerInteractionListener mOnTryviewerInteractionListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement.
            mOnTryviewerInteractionListener = (OnTryviewerInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTryviewerInteractionListener");
        }
    }

    // ======================================================================================

    // the button to talk to computer
    private ImageButton mBacktopButton;

    // the button for sound activation
    private Button mBackButton;

    // the button to start it all
    private Button mStartButton;

    // the button to stop it all
    private Button mStopButton;
    private boolean mRunStatus = false;

    // the button for settings
    private Button mRestartButton;

    // the one status line
    private TextView mTextstatus_top;
    private TextView mTextstatus_bottom;

    // the button to talk to computer
    private ImageButton mBackbottomButton;

    // the button to stop it all
    private Button mRecordButton;

    // the button for settings
    private Button mGalleryButton;

    // the main contents layout in the center
    private LinearLayout mCenterLayout;

    // the list of videos to view
    private ListView mListView;
    private ListUriAdapter mListUriAdapter;

    // the video view widget to show contents
    private VideoView mVideoView;

    // the preferences holder
    private SharedPreferences sharedPref;

    // the preferences values
    private boolean autoListen;
    private boolean isChatty;
    private String speakLanguage;
    private String listenLanguage;
    private String displayLanguage;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tryviewer_fragment, container, false);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        autoListen = sharedPref.getBoolean("pref_key_auto_listen", false);
        isChatty = sharedPref.getBoolean("pref_key_ischatty", false);
        speakLanguage = sharedPref.getString("pref_key_speak_language", "");
        listenLanguage = sharedPref.getString("pref_key_listen_language", "");
        displayLanguage = sharedPref.getString("pref_key_display_language", "");

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

        // the start button
        mStartButton = (Button) view.findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                startvideo();
            }
        });

        // the stop button
        mStopButton = (Button) view.findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                stopvideo();
            }
        });

        // the settings button
        mRestartButton = (Button) view.findViewById(R.id.restart_button);
        mRestartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                restartvideo();
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
        mRecordButton = (Button) view.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                recordvideo();
            }
        });

        // the settings button
        mGalleryButton = (Button) view.findViewById(R.id.gallery_button);
        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtryviewermode(2);
            }
        });

        mTextstatus_bottom = (TextView) view.findViewById(R.id.textstatus_bottom);
        mTextstatus_bottom.setText("Ready");

        // =========================== center content area ============================

        // the center layout to show contents
        mCenterLayout = (LinearLayout) view.findViewById(R.id.center_layout);

        // start the filling of the list with uris of videos
        filllistview();

        // the list containing the names of files to display
        mListView = (ListView) view.findViewById(R.id.list_view);
        mListUriAdapter = new ListUriAdapter(getActivity(),mImageUris);
        mListView.setAdapter(mListUriAdapter);
        mListView.setClickable(true);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                buttonsound();
                view.setSelected(true);
                listuriclicked(position);
            }
        });

        mVideoView = (VideoView) view.findViewById(R.id.video_view);

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
        mStartButton.setTypeface(face2);
        mStopButton.setTypeface(face2);
        mBackButton.setTypeface(face2);
        mRestartButton.setTypeface(face2);
        mTextstatus_top.setTypeface(face);
        // bottom buttons
        mRecordButton.setTypeface(face2);
        mGalleryButton.setTypeface(face2);
        mTextstatus_bottom.setTypeface(face3);
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
        // dynamic status part
        mRunStatus = sharedPref.getBoolean("pref_key_run_status", false);
        // resurect the application to last settings
        //switchbuttonlayout(mButtonsmode);
    }

    @Override
    public void onPause() {
        // save the current status
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pref_key_run_status", mRunStatus);
        editor.commit();
        //stopsensors();
        super.onPause();
    }

    // ask the activity to switch to another fragment
    private void switchtryviewermode(int mode) {
        mOnTryviewerInteractionListener.onTryviewerModeChange(mode);
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

    // tell something in the bottom status line
    private void say(String texte) {
        mTextstatus_bottom.setText(texte);
    }

    // =======================================================================================

    //private Uri staticUri =  Uri.parse("android.resource://"
    //        + getActivity().getPackageName()
    //        + "/" + R.raw.powers_of_ten);
    //private Uri staticUri =  Uri.parse("android.resource://net.ddns.mlsoftlaberge.trycorder"
    //        + "/" + R.raw.powers_of_ten);

    private Uri dynamicUri = null;

    private MediaController mMediaController=null;

    private void startvideo() {
        say("Start Video");
        if(mMediaController==null) {
            mMediaController = new MediaController(getActivity());
            mMediaController.setAnchorView(mVideoView);
            mVideoView.setMediaController(mMediaController);
        }
        if(dynamicUri!=null) {
            mVideoView.setVideoURI(dynamicUri);
        }
        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(false);
                mVideoView.start();
                say("Video is started");
            }
        });
        //mVideoView.start();
    }

    private void stopvideo() {
        mVideoView.pause();
        say("Video is paused");
    }

    private void restartvideo() {
        mVideoView.start();
        say("Video is restarted");
    }

    // ======================== Image list loader ==========================================

    private ArrayList<Uri> mImageUris = new ArrayList<Uri>();
    private int currenturi=0;

    private void filllistview() {
        loadimageuris();
        if (mImageUris.size()>0) {
            currenturi=0;
            dynamicUri=mImageUris.get(currenturi);
        }
    }

    private void loadimageuris() {
        mImageUris.clear();
        say("Loading videos Uris");
        String path = Environment.getExternalStorageDirectory().toString()+"/DCIM/Camera";
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        Log.d("Files", "Size: "+ file.length);
        for (int i=0; i < file.length; i++)
        {
            String name = file[i].getName();
            if(name.contains(".mp4")) {
                Log.d("Files", "FileName:" + name);
                Uri uri = Uri.parse(path + "/" + name);
                mImageUris.add(uri);
            }
        }
        // sort the name list in reverse order
        Comparator comparator = Collections.reverseOrder();
        Collections.sort(mImageUris,comparator);
        say("Videos Uris loaded");
    }

    public class ListUriAdapter extends ArrayAdapter<Uri> {
        public ListUriAdapter(Context context, ArrayList<Uri> uris) {
            super(context, 0, uris);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Uri uri = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.tryviewer_row, parent, false);
            }
            // Lookup view for data population
            TextView label = (TextView) convertView.findViewById(R.id.label);
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            // Populate the data into the template view using the data object
            label.setText(uri.getLastPathSegment());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    private void listuriclicked(int position) {
        dynamicUri=mImageUris.get(position);
        startvideo();
    }

    // ===================================================================================
    // common functions to obtain a media uri

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Trycorder", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    // ==========================================================================================
    // call camera and gallery application

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private Uri fileUri;

    private void takephoto() {
        say("Open Photo application");
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);  // create a file to save the picture
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private void recordvideo() {
        say("Open Video application");
        //create new Intent
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);  // create a file to save the video
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        // start the Video Capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == getActivity().RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                say("Saved to " + fileUri.toString());
                // inform the media manager to scan our new file
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(fileUri);
                getActivity().sendBroadcast(intent);
            } else if (resultCode == getActivity().RESULT_CANCELED) {
                // User cancelled the image capture
                say("Cancelled Photo");
            } else {
                // Image capture failed, advise user
                say("Failed Saving Photo");
            }
        }

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == getActivity().RESULT_OK) {
                say("Saved to " + fileUri.toString());
                // inform the media manager to scan our new file
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(fileUri);
                getActivity().sendBroadcast(intent);
                filllistview();
            } else if (resultCode == getActivity().RESULT_CANCELED) {
                // User cancelled the video capture
                say("Cancelled Video");
            } else {
                // Video capture failed, advise user
                say("Failed Saving Video");
            }
        }
    }


}