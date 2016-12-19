package net.ddns.mlsoftlaberge.trycorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import net.ddns.mlsoftlaberge.trycorder.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by mlsoft on 16-06-23.
 */
public class TrygalleryFragment extends Fragment implements
        AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory {

    public TrygalleryFragment() {
    }

    // ======================================================================================
    public interface OnTrygalleryInteractionListener {
        public void onTrygalleryModeChange(int mode);
    }

    private OnTrygalleryInteractionListener mOnTrygalleryInteractionListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Assign callback listener which the holding activity must implement.
            mOnTrygalleryInteractionListener = (OnTrygalleryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTrygalleryInteractionListener");
        }
    }

    // ======================================================================================


    private ImageButton mBacktopButton;
    private Button mBackButton;
    private Button mGalleryappButton;
    private Button mSendButton;

    // the button to talk to computer
    private ImageButton mBackbottomButton;

    // the button to start it all
    private Button mPhotoButton;

    // the button for settings
    private Button mVideogalButton;

    // the one status line
    private TextView mTextstatus_top;
    private TextView mTextstatus_bottom;


    private ImageSwitcher mImageSwitcher;
    private Gallery mImageGallery;

    private List<Uri> mImageUris = new ArrayList<Uri>();
    private int currenturi=0;

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
        View view = inflater.inflate(R.layout.trygallery_fragment, container, false);

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
                switchtrygallerymode(1);
            }
        });

        // the sound-effect button
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtrygallerymode(1);
            }
        });

        // the search button
        mGalleryappButton = (Button) view.findViewById(R.id.gallery_app_button);
        mGalleryappButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                buttongallery();
            }
        });
        // the search button
        mSendButton = (Button) view.findViewById(R.id.photo_send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                buttonsend();
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
                switchtrygallerymode(1);
            }
        });

        // the start button
        mPhotoButton = (Button) view.findViewById(R.id.photo_button);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                takephoto();
            }
        });

        // the settings button
        mVideogalButton = (Button) view.findViewById(R.id.videogal_button);
        mVideogalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                switchtrygallerymode(3);
            }
        });

        mTextstatus_bottom = (TextView) view.findViewById(R.id.textstatus_bottom);
        mTextstatus_bottom.setText("Ready");

        // load the list of uris from camera directory
        loadimageuris();

        // obtain and program the switcher and gallery
        mImageSwitcher = (ImageSwitcher) view.findViewById(R.id.image_switcher);
        mImageGallery = (Gallery) view.findViewById(R.id.image_gallery);

        mImageSwitcher.setFactory(this);
        mImageSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        mImageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        mImageGallery.setAdapter(new ImageAdapter(getContext()));
        mImageGallery.setOnItemSelectedListener(this);

        return(view);
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
        mGalleryappButton.setTypeface(face2);
        mSendButton.setTypeface(face2);
        mTextstatus_top.setTypeface(face);
        // bottom buttons
        mPhotoButton.setTypeface(face2);
        mVideogalButton.setTypeface(face2);
        mTextstatus_bottom.setTypeface(face3);
    }


    // ==================================================================================

    private void buttonsound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity().getBaseContext(), R.raw.keyok2);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    // tell something in the bottom status line
    private void say(String texte) {
        mTextstatus_bottom.setText(texte);
    }

    // ask the activity to switch to another fragment
    private void switchtrygallerymode(int mode) {
        mOnTrygalleryInteractionListener.onTrygalleryModeChange(mode);
    }

    // start the external gallery application
    private void buttongallery() {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // send the current content to someone
    private void buttonsend() {
        if(mImageUris==null) return;
        Uri uri = mImageUris.get(currenturi);
        String filepath = uri.toString();
        // send this image to someone
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        //shareIntent.setData(uri);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Image");
        shareIntent.putExtra(Intent.EXTRA_TEXT,"From my Trycorder\n"+uri.toString());
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+filepath));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(Intent.createChooser(shareIntent, "Send image to ..."));
    }

    // =====================================================================================
    // ======================== Image list loader ==========================================

    private void loadimageuris() {
        mImageUris.clear();
        String path = Environment.getExternalStorageDirectory().toString()+"/DCIM/Camera";
        Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        Log.d("Files", "Size: "+ file.length);
        for (int i=0; i < file.length; i++)
        {
            String name = file[i].getName();
            if(name.contains(".jpg")) {
                Log.d("Files", "FileName:" + name);
                Uri uri = Uri.parse(path + "/" + name);
                mImageUris.add(uri);
            }
        }
        // sort the name list in reverse order
        Comparator comparator = Collections.reverseOrder();
        Collections.sort(mImageUris,comparator);
    }

    // ===================================================================================
    // ====================== Image Gallery callbacks ===================================

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        currenturi=position;
        if(mImageUris!=null) {
            //mImageSwitcher.setImageURI(mImageUris.get(position));
            File f = new File(mImageUris.get(position).getPath());
            Bitmap b = decodeFile(f,800);
            BitmapDrawable pic = new BitmapDrawable(b);
            mImageSwitcher.setImageDrawable(pic);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    public View makeView() {
        ImageView i = new ImageView(getContext());
        i.setBackgroundColor(0xFF000000);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(Gallery.LayoutParams.MATCH_PARENT,
                Gallery.LayoutParams.MATCH_PARENT));
        return i;
    }

    // ===================================================================================
    // ====================== Image Gallery Adapter ======================================

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            if(mImageUris!=null) {
                return (mImageUris.size());
            } else {
                return(0);
            }
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);
            if(mImageUris!=null) {
                File f = new File(mImageUris.get(position).getPath());
                Bitmap b = decodeFile(f,100);
                BitmapDrawable pic = new BitmapDrawable(b);
                i.setImageDrawable(pic);
            }
            i.setAdjustViewBounds(true);
            i.setLayoutParams(new Gallery.LayoutParams(
                    Gallery.LayoutParams.WRAP_CONTENT, Gallery.LayoutParams.WRAP_CONTENT));
            i.setBackgroundResource(R.drawable.picture_frame);
            return i;
        }

    }

    // ==================================================================================
    //decodes image and scales it to reduce memory consumption in gallery

    private Bitmap decodeFile(File f, int size){
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //Find the correct scale value. It should be the power of 2.
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;

            while(true){
                if(width_tmp/2<size || height_tmp/2<size)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;

            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        }

        catch (FileNotFoundException e) {}

        return null;
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
                loadimageuris();
                mImageGallery.setAdapter(new ImageAdapter(getContext()));
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
