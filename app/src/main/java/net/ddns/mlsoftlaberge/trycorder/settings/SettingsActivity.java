package net.ddns.mlsoftlaberge.trycorder.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

import net.ddns.mlsoftlaberge.trycorder.R;


/**
 * Created by mlsoft on 15/04/16.
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}