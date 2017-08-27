package net.ddns.mlsoftlaberge.trycorder.trycorder;

/*
*  By Martin Laberge (mlsoft), From March 2016 to november 2016.
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

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by mlsoft on 16-06-25.
 */
// ==========================================================================================

public class OriSensorView extends TextView implements SensorEventListener, LocationListener {

    private float longitude=0.0f;
    private float latitude=0.0f;

    private Paint mPaint;
    private float position = 0;

    private Location location = null;

    private Context mContext;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private String locationProvider;

    public OriSensorView(Context context, SensorManager smanager, LocationManager lmanager) {
        super(context);
        mContext=context;
        mSensorManager=smanager;
        mLocationManager=lmanager;
        init();
    }

    private void init() {
        // initialize the mPaint object
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
    }

    public float getLongitude() {
        return(longitude);
    }

    public float getLatitude() {
        return(latitude);
    }

    public void start() {
        // link a sensor to the sensorview
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);

        // initialize the gps service
        boolean enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // check if enabled and if not send user to the GPS settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mContext.startActivity(intent);
        }
        // Define the criteria how to select the location provider -> use
        // default
        Location loc = null;
        Criteria criteria = new Criteria();
        locationProvider = mLocationManager.getBestProvider(criteria, false);
        //locationProvider = "gps";
        Log.d("Orisensorview","Provider:"+locationProvider);
        try {
            Log.d("Orisensorview","Get location from "+locationProvider);
            loc = mLocationManager.getLastKnownLocation(locationProvider);
            if(loc==null) {
                Log.d("Orisensorview","Get location from network");
                loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            setLocation(loc);
        } catch (SecurityException e) {
            Log.d("Orisensorview","No GPS available");
        }
        // start gps location updates
        try {
            //mLocationManager.requestLocationUpdates(locationProvider, 400, 1, this);
            mLocationManager.requestLocationUpdates(locationProvider, 0, 0, this);
        } catch (SecurityException e) {
            Log.d("Orisensorview","No GPS available");
        }

    }

    public void stop() {
        // stop sensor orientation
        mSensorManager.unregisterListener(this);
        // stop location updates
        try {
            if(mLocationManager!=null) mLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.d("Orisensorview","Error closing GPS");
        }
    }

    public void setLocation(Location loc) {
        location = loc;
        if(location==null) Log.d("Orisensorview","Location NULL");
        else Log.d("Orisensorview","Location:"+String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude()));
    }

    // ================= callbacks for the orientation sensor ===========

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // angle between the magnetic north direction
        // 0=North, 90=East, 180=South, 270=West
        float azimuth = event.values[0];
        updateData(azimuth);
    }

    public void updateData(float position) {
        this.position = position;
        invalidate();
    }

    // ============ callbacks for the location listener ===============

    @Override
    public void onLocationChanged(Location loc) {
        Log.d("Orisensorview","Location changed.");
        setLocation(loc);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Orisensorview","Location Status changed. " + String.valueOf(status));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Orisensorview","Enabled new provider " + provider);

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Orisensorview","Disabled new provider " + provider);
    }

    // =========================================================================
    // handle the drawing of values
    @Override
    protected void onDraw(Canvas mCanvas) {
        int mWidth = getMeasuredWidth();
        int mHeight = getMeasuredHeight();
        // draw the square
        mPaint.setColor(Color.MAGENTA);
        mPaint.setStrokeWidth(2.0f);
        mCanvas.drawLine(0,0,mWidth,0,mPaint);
        mCanvas.drawLine(0,0,0,mHeight,mPaint);
        mCanvas.drawLine(mWidth-1,mHeight-1,mWidth-1,0,mPaint);
        mCanvas.drawLine(mWidth-1,mHeight-1,0,mHeight-1,mPaint);

        mPaint.setColor(Color.WHITE);
        int xPoint = getMeasuredWidth() / 2;
        int yPoint = getMeasuredHeight() / 2;
        if (yPoint > xPoint) {
            yPoint = xPoint;
        } else {
            xPoint = yPoint;
        }

        float radius = (float) (Math.min(xPoint, yPoint) * 0.9);
        mCanvas.drawCircle(xPoint, yPoint, radius, mPaint);
        // mCanvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);

        // 3.1416 is a good approximation for the circle
        mCanvas.drawLine(xPoint,
                yPoint,
                (float) (xPoint + radius
                        * Math.sin((double) (-position) / 180 * 3.1416)),
                (float) (yPoint - radius
                        * Math.cos((double) (-position) / 180 * 3.1416)), mPaint);

        mCanvas.drawText("ORI: " + String.valueOf(position), xPoint * 2.0f, yPoint * 2.0f - 32.0f, mPaint);

        // draw the longitude and latitude
        if (location != null) {
            float lat = (float) (location.getLatitude());
            float lng = (float) (location.getLongitude());
            // save it to public accesssible values
            latitude=lat;
            longitude=lng;
            mCanvas.drawText("LAT: " + String.valueOf(lat), xPoint * 2.0f, 32.0f, mPaint);
            mCanvas.drawText("LON: " + String.valueOf(lng), xPoint * 2.0f, 64.0f, mPaint);
        } else {
            mCanvas.drawText("LAT: " + "Not avalaible", xPoint * 2.0f, 32.0f, mPaint);
            mCanvas.drawText("LON: " + "Not avalaible", xPoint * 2.0f, 64.0f, mPaint);
        }
    }


}
