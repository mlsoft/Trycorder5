package net.ddns.mlsoftlaberge.trycorder.trycorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import net.ddns.mlsoftlaberge.trycorder.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mlsoft on 16-06-25.
 */
// ============================================================================
// class defining the sensor display widget
public class MotSensorView extends TextView {
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Canvas mCanvas = new Canvas();
    private Bitmap motorBitmap;
    private int mWidth;
    private int mHeight;

    private int mode;   // 1=in 2=out

    private int position=0;

    // initialize the 3 colors, and setup painter
    public MotSensorView(Context context) {
        super(context);
        // text paint
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        // line paint
        mPaint2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setStrokeWidth(3);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.MAGENTA);
    }

    public void setmode(int no) {
        mode=no;
        if(mode==0) stop();
    }

    // ======= timer section =======
    private Timer timer=null;
    private MyTimer myTimer;

    public void stop() {
        if(timer!=null) {
            timer.cancel();
            timer=null;
        }
        position=0;
        invalidate();
    }

    public void start() {
        // start the timer to eat this stuff and display it
        position=1;
        timer = new Timer("motor");
        myTimer = new MyTimer();
        if(mode==1) {
            timer.schedule(myTimer, 50L, 50L);
        } else {
            timer.schedule(myTimer, 10L, 10L);
        }
    }

    private class MyTimer extends TimerTask {
        public void run() {
            position+=3;
            postInvalidate();
            if(position>=100) {
                position=1;
                postInvalidate();
            }
        }
    }

    // =========== textview callbacks =================
    // initialize the bitmap to the size of the view, fill it white
    // init the view state variables to initial values
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // create the canvas bitmap to draw the animation
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.BLACK);
        mWidth = w;
        mHeight = h;
        // create a motor background
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.warpcore);
        motorBitmap = Bitmap.createScaledBitmap(bm,w,h,true);
        // call the super class
        super.onSizeChanged(w, h, oldw, oldh);
    }

    // draw
    @Override
    public void onDraw(Canvas viewcanvas) {
        synchronized (this) {
            if (mBitmap != null) {
                // clear the surface
                mCanvas.drawColor(Color.BLACK);
                // draw the background motor bitmap
                mCanvas.drawBitmap(motorBitmap,18,0,null);
                // draw the shield effect
                if(mode==1) mPaint2.setColor(Color.MAGENTA);
                else mPaint2.setColor(Color.BLUE);
                if(position!=0) {
                    // compute positions
                    float px=mWidth/2;
                    float py=(mHeight/6)*4;
                    float dy=(((float)mHeight)/180)*position;
                    mCanvas.drawCircle(px,py-dy,16,mPaint2);
                    mCanvas.drawCircle(px,py-dy,32,mPaint2);
                }
                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

}

