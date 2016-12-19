package net.ddns.mlsoftlaberge.trycorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by mlsoft on 04/08/16.
 */
public class TrycorderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Trycorder Boot Detected.", Toast.LENGTH_LONG).show();
        Log.d("receiver", "Trycorder Boot Detected");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoBoot = sharedPref.getBoolean("pref_key_auto_boot", true);

        if(autoBoot) {
            Log.d("receiver", "Start Trycorder Service");
            try {
                context.startService(new Intent(context, TrycorderService.class));
            } catch (Exception e) {
                Log.d("receiver", "Cant start trycorder service");
            }
        }
    }

}

