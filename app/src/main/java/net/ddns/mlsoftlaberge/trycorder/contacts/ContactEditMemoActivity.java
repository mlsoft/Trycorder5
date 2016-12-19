package net.ddns.mlsoftlaberge.trycorder.contacts;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;

import android.view.MenuItem;
import android.view.View;

import net.ddns.mlsoftlaberge.trycorder.BuildConfig;
import net.ddns.mlsoftlaberge.trycorder.R;

/**
 * Created by mlsoft on 28/02/16.
 */
public class ContactEditMemoActivity extends FragmentActivity {
    // Defines a tag for identifying the single fragment that this activity holds
    private static final String TAG = "ContactEditMemoActivity";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity expects to receive an intent that contains the uri of a contact
        if (getIntent() != null) {
            // Fetch the data Uri from the intent provided to this activity
            final Uri uri = getIntent().getData();
            final String name = getIntent().getStringExtra("NAME");
            final String memo = getIntent().getStringExtra("MEMO");
            // Checks to see if fragment has already been added, otherwise adds a new
            // ContactEditMemoFragment with the Uri provided in the intent
            if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(android.R.id.content, ContactEditMemoFragment.newInstance(uri, name, memo), TAG);
                ft.commit();
            }
        } else {
            // No intent provided, nothing to do so finish()
            finish();
        }
    }

    // permits this activity to hide status and action bars, and proceed full screen
    //@Override
    //public void onWindowFocusChanged(boolean hasFocus) {
    //    super.onWindowFocusChanged(hasFocus);
    //    if (hasFocus) {
    //        getWindow().getDecorView().setSystemUiVisibility(
    //                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    //                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    //                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    //                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    //                        | View.SYSTEM_UI_FLAG_FULLSCREEN
    //                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    //    }
    //}

}
