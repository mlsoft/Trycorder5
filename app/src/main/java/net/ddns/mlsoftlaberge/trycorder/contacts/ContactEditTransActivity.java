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

import net.ddns.mlsoftlaberge.trycorder.BuildConfig;
import net.ddns.mlsoftlaberge.trycorder.R;

/**
 * Created by mlsoft on 28/02/16.
 */
public class ContactEditTransActivity extends FragmentActivity {
    // Defines a tag for identifying the single fragment that this activity holds
    private static final String TAG = "ContactEditTransActivity";

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            // Enable strict mode checks when in debug modes
            Utils.enableStrictMode();
        }
        super.onCreate(savedInstanceState);

        // This activity expects to receive an intent that contains the uri of a contact
        if (getIntent() != null) {

            // For OS versions honeycomb and higher use action bar
            //if (Utils.hasHoneycomb()) {
                // Enables action bar "up" navigation
            //    getActionBar().setDisplayHomeAsUpEnabled(true);
            //}

            // Fetch the data Uri from the intent provided to this activity
            final Uri uri = getIntent().getData();
            final String name = getIntent().getStringExtra("NAME");
            final String descrip = getIntent().getStringExtra("DESCRIP");
            final String amount = getIntent().getStringExtra("AMOUNT");
            final String date = getIntent().getStringExtra("DATE");
            // Checks to see if fragment has already been added, otherwise adds a new
            // ContactEditMemoFragment with the Uri provided in the intent
            if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                // Adds a newly created ContactEditMemoFragment that is instantiated with the
                // data Uri
                ft.add(android.R.id.content, ContactEditTransFragment.newInstance(uri, name, descrip, amount, date), TAG);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Tapping on top left ActionBar icon navigates "up" to hierarchical parent screen.
                // The parent is defined in the AndroidManifest entry for this activity via the
                // parentActivityName attribute (and via meta-data tag for OS versions before API
                // Level 16). See the "Tasks and Back Stack" guide for more information:
                // http://developer.android.com/guide/components/tasks-and-back-stack.html
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        // Otherwise, pass the item to the super implementation for handling, as described in the
        // documentation.
        return super.onOptionsItemSelected(item);
    }


}
