package net.ddns.mlsoftlaberge.trycorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/*
*  By Martin Laberge (mlsoftlaberge@gmail.com), From March 2016 to november 2016.
*  Licence: Can be shared with anyone, for non profit, provided my name stays in the comments.
*  This is a conglomerate of examples codes found in differents public forums on internet.
*  I just used the public knowledge to fit a special way to use an android phone functions.
*/

/* Copyright 2016 Martin Laberge
*
*        Licensed under the Apache License, Version 2.0 (the "License");
*        you may not use this file except in compliance with the License.
*        You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*        Unless required by applicable law or agreed to in writing, software
*        distributed under the License is distributed on an "AS IS" BASIS,
*        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*        See the License for the specific language governing permissions and
*        limitations under the License.
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

