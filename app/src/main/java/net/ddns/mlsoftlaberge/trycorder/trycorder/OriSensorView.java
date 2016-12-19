package net.ddns.mlsoftlaberge.trycorder.trycorder;

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
        // initialize the gps service
        boolean enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        //if (!enabled) {
        //    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //    startActivity(intent);
        //}
        // Define the criteria how to select the location provider -> use
        // default
        Location location = null;
        Criteria criteria = new Criteria();
        locationProvider = mLocationManager.getBestProvider(criteria, false);
        //locationProvider = "gps";
        try {
            location = mLocationManager.getLastKnownLocation(locationProvider);
        } catch (SecurityException e) {
            //say("No GPS available");
        }
        // Initialize the location fields
        if (location != null) {
            //say("Provider " + locationProvider + " has been selected.");
            setLocation(location);
        } else {
            //say("No location available. " + locationProvider);
        }

        // link a sensor to the sensorview
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        // start gps location updates
        try {
            mLocationManager.requestLocationUpdates(locationProvider, 400, 1, this);
        } catch (SecurityException e) {
            //say("No GPS avalaible.");
        }

    }

    public void stop() {
        mSensorManager.unregisterListener(this);
        try {
            if(mLocationManager!=null) mLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            //say("Error closing GPS");
        }
    }

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

    public void updateData(float position) {
        this.position = position;
        invalidate();
    }

    public void setLocation(Location loc) {
        this.location = loc;
    }

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

    // ============ callbacks for the location listener ===============

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //say("Location Status changed. " + String.valueOf(status));
    }

    @Override
    public void onProviderEnabled(String provider) {
        //say("Enabled new provider " + provider);

    }

    @Override
    public void onProviderDisabled(String provider) {
        //say("Disabled new provider " + provider);
    }


}
