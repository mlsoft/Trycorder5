package net.ddns.mlsoftlaberge.trycorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

/*
*  By Martin Laberge (mlsoft), From March 2016 to november 2016.
*  Licence: Can be shared with anyone, for non profit, provided my name stays in the comments.
*  This is a conglomerate of examples codes found in differents public forums on internet.
*  I just used the public knowledge to fit a special way to use an android phone functions.
*/

public class TrycorderActivity extends FragmentActivity implements
        TrycorderFragment.OnTrycorderInteractionListener,
        TrysensorFragment.OnTrysensorInteractionListener,
        TryvisionFragment.OnTryvisionInteractionListener,
        TryviscamFragment.OnTryviscamInteractionListener,
        TryviewerFragment.OnTryviewerInteractionListener,
        TrydesktopFragment.OnTrydesktopInteractionListener,
        TrygalleryFragment.OnTrygalleryInteractionListener {

    private static String TAG = "Trycorder";

    private TrycorderFragment mTrycorderFragment=null;
    private TrygalleryFragment mTrygalleryFragment=null;
    private TryviewerFragment mTryviewerFragment=null;
    private TrysensorFragment mTrysensorFragment=null;
    private TryvisionFragment mTryvisionFragment=null;
    private TryviscamFragment mTryviscamFragment=null;
    private TrydesktopFragment mTrydesktopFragment=null;

    private int currentMode=0;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ask the permissions
        askpermissions();
        // create the 1 initial fragment
        mTrycorderFragment=new TrycorderFragment();
        // start the fragment full screen
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(android.R.id.content, mTrycorderFragment, TAG);
        ft.commit();
        currentMode=1;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int lastmode = sharedPref.getInt("pref_key_last_mode", 0);
        if(lastmode>1) {
            switchfragment(lastmode);
        }
    }

    // the function who will receive broadcasts from the service
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String theEvent = intent.getStringExtra("TRYSERVERCMD");
            String theText = intent.getStringExtra("TRYSERVERTEXT");
            if (theEvent.equals("iplist")) {
                // refresh the ip list
                if(currentMode==1) mTrycorderFragment.askscanlist();
                if(currentMode==7) mTrydesktopFragment.askscanlist();
            } else if (theEvent.equals("text")) {
                // text received
                if(currentMode==1) mTrycorderFragment.displaytext(theText);
                if(currentMode==7) mTrydesktopFragment.displaytext(theText);
            } else if (theEvent.equals("say")) {
                // text received
                if(currentMode==1) mTrycorderFragment.say(theText);
                if(currentMode==7) mTrydesktopFragment.say(theText);
            } else if (theEvent.equals("listen")) {
                // text received
                if(currentMode==1) mTrycorderFragment.understood(theText);
                if(currentMode==4) mTrysensorFragment.understood(theText);
                if(currentMode==7) mTrydesktopFragment.understood(theText);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(TrycorderService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("pref_key_last_mode", currentMode);
        editor.commit();
        super.onStop();
    }

    // permits this activity to hide status and action bars, and proceed full screen
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onTrycorderModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTrygalleryModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTryviewerModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTrysensorModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTryvisionModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTryviscamModeChange(int mode) {
        switchfragment(mode);
    }

    @Override
    public void onTrydesktopModeChange(int mode) {
        switchfragment(mode);
    }

    private void switchfragment(int mode) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        switch(mode) {
            case 1:
                if(mTrycorderFragment==null) mTrycorderFragment=new TrycorderFragment();
                ft.replace(android.R.id.content, mTrycorderFragment, TAG);
                ft.commit();
                break;
            case 2:
                if(mTrygalleryFragment==null) mTrygalleryFragment=new TrygalleryFragment();
                ft.replace(android.R.id.content, mTrygalleryFragment, TAG);
                ft.commit();
                break;
            case 3:
                if(mTryviewerFragment==null) mTryviewerFragment=new TryviewerFragment();
                ft.replace(android.R.id.content, mTryviewerFragment, TAG);
                ft.commit();
                break;
            case 4:
                if(mTrysensorFragment==null) mTrysensorFragment=new TrysensorFragment();
                ft.replace(android.R.id.content, mTrysensorFragment, TAG);
                ft.commit();
                break;
            case 5:
                if(mTryvisionFragment==null) mTryvisionFragment=new TryvisionFragment();
                mTryvisionFragment.setmode(1);
                ft.replace(android.R.id.content, mTryvisionFragment, TAG);
                ft.commit();
                break;
            case 6:
                if(Build.VERSION.SDK_INT>=21) {
                    if (mTryviscamFragment == null) mTryviscamFragment = new TryviscamFragment();
                    mTryviscamFragment.seteffect(1);
                    mTryviscamFragment.setscene(1);
                    ft.replace(android.R.id.content, mTryviscamFragment, TAG);
                    ft.commit();
                }
                break;
            case 7:
                if(mTrydesktopFragment==null) mTrydesktopFragment=new TrydesktopFragment();
                ft.replace(android.R.id.content, mTrydesktopFragment, TAG);
                ft.commit();
                break;
        }
        currentMode=mode;
    }



    // ==========================================================================

    private void askpermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_TASKS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.GET_TASKS,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS,
                            Manifest.permission.RECEIVE_BOOT_COMPLETED
                    }, 1);
        }
    }

}
