package net.ddns.mlsoftlaberge.trycorder.trycorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

/**
 * Created by mlsoft on 16-06-25.
 */
// ============================================================================
// class defining the sensor display widget
public class TemSensorView extends TextView implements SensorEventListener {

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Canvas mCanvas = new Canvas();

    private int mWidth;
    private int mHeight;

    private float lastPresValue = 0.0f;
    private float lastAtempValue = 0.0f;
    private float lastLightValue = 0.0f;

    private Context mContext;
    private SensorManager mSensorManager;

    // initialize the 3 colors, and setup painter
    public TemSensorView(Context context, SensorManager manager) {
        super(context);
        mContext=context;
        mSensorManager=manager;
        // text paint
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        // line paint
        mPaint2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setStrokeWidth(2);
        mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint2.setColor(Color.RED);

    }

    public void resetcount() {
        lastPresValue = 0.0f;
        lastAtempValue = 0.0f;
        lastLightValue = 0.0f;
    }

    public void start() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    // initialize the bitmap to the size of the view, fill it white
    // init the view state variables to initial values
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.BLACK);
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    // draw
    @Override
    public void onDraw(Canvas viewcanvas) {
        synchronized (this) {
            if (mBitmap != null) {
                // clear the surface
                mCanvas.drawColor(Color.BLACK);
                // draw the square
                mPaint.setColor(Color.MAGENTA);
                mPaint.setStrokeWidth(2.0f);
                mCanvas.drawLine(0,0,mWidth,0,mPaint);
                mCanvas.drawLine(0,0,0,mHeight,mPaint);
                mCanvas.drawLine(mWidth-1,mHeight-1,mWidth-1,0,mPaint);
                mCanvas.drawLine(mWidth-1,mHeight-1,0,mHeight-1,mPaint);
                // draw the ambient temperature
                drawSensor("Press(Kpa)", lastPresValue, 0.0f, 1200.0f, 0, 0, mWidth / 3.0f, mHeight);

                mCanvas.drawLine(mWidth / 3.0f, 0, mWidth / 3.0f, mHeight, mPaint);

                // draw the ambient temperature
                drawSensor("ATemp(°C)", lastAtempValue, -20.0f, 100.0f, mWidth / 3.0f, 0, mWidth / 3.0f, mHeight);

                mCanvas.drawLine(mWidth / 3.0f * 2.0f, 0, mWidth / 3.0f * 2.0f, mHeight, mPaint);

                // draw the ambient temperature
                drawSensor("Light(Lux)", lastLightValue, 0.0f, 200.0f, mWidth / 3.0f * 2.0f, 0, mWidth / 3.0f, mHeight);


                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

    private void drawSensor(String label, float value, float minvalue, float maxvalue,
                            float px, float py, float nx, float ny) {
        mPaint.setColor(Color.WHITE);
        // draw a proportionnal large red line for the value
        float linelen = (ny - 64.0f) * ((value - minvalue) / (maxvalue - minvalue));
        mCanvas.drawRect(px + (nx / 3.0f), py + ny - 32 - linelen, px + (nx / 3.0f * 2.0f), py + ny - 32, mPaint2);
        // draw the label on top
        mCanvas.drawText(label, px + 32, py + 24, mPaint);
        // draw the value on bottom
        mCanvas.drawText(String.format("%.2f", value), px + (nx / 3.0f), py + ny - 4, mPaint);
        // draw a center white line showing range
        mCanvas.drawLine(px + (nx / 2.0f), py + 32, px + (nx / 2.0f), py + ny - 32, mPaint);
        // draw scale lines
        mCanvas.drawLine(px + (nx / 2) - 32, py + 32, px + (nx / 2) + 32, py + 32, mPaint); // top white line
        mCanvas.drawLine(px + (nx / 2) - 32, py + ny - 32, px + (nx / 2) + 32, py + ny - 32, mPaint); // bottom white line
        float zerolen = (ny - 64.0f) * ((0.0f - minvalue) / (maxvalue - minvalue));
        if (zerolen > 0.0f)
            mCanvas.drawLine(px + (nx / 2) - 32, py + ny - 32 - zerolen, px + (nx / 2) + 32, py + ny - 32 - zerolen, mPaint); // zero white line
        // draw scale texts
        mCanvas.drawText(String.format("%.0f", maxvalue), px + 4, py + 56, mPaint);  // max value indicator
        mCanvas.drawText(String.format("%.0f", minvalue), px + 4, py + ny - 32, mPaint);  // min value indicator
        if (zerolen > 0.0f)
            mCanvas.drawText("0.0", px + 4, py + ny - 32 - zerolen, mPaint);  // zero indicator
    }

    // extract sensor data and plot them on view
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (mBitmap != null) {
                if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    lastAtempValue = event.values[0];
                    invalidate();
                }
                if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                    lastPresValue = event.values[0];
                    invalidate();
                }
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                    lastLightValue = event.values[0];
                    invalidate();
                }
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do
    }

}

