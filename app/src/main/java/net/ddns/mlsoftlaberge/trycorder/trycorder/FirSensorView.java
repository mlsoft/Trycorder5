package net.ddns.mlsoftlaberge.trycorder.trycorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mlsoft on 16-06-25.
 */
// ============================================================================
// class defining the sensor display widget
public class FirSensorView extends TextView {
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Paint mPaint3 = new Paint();
    private Canvas mCanvas = new Canvas();

    private int mWidth;
    private int mHeight;

    private float position = 0.0f;
    private int firMode = 0;

    private int mDirection=50;
    private int mForce=50;

    private Timer timer;
    private MyTimer myTimer;

    // initialize the 3 colors, and setup painter
    public FirSensorView(Context context) {
        super(context);
        // text paint
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        // line paint
        mPaint2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setStrokeWidth(16);
        mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint2.setColor(Color.RED);
        // target paint
        mPaint3.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint3.setStrokeWidth(3);
        mPaint3.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint3.setColor(Color.BLUE);

    }

    public void setmode(int no) {
        firMode = no;
    }

    public void setdirection(int no) {
        mDirection = no;
        invalidate();
    }

    public void setforce(int no) {
        mForce = no;
    }

    public void startfire() {
        position = 0.0f;
        timer = new Timer("fire");
        myTimer = new MyTimer();
        timer.schedule(myTimer, 10L, 10L);
    }

    private class MyTimer extends TimerTask {
        public void run() {
            if(firMode==1) position += 10;
            else position += 5;
            postInvalidate();
            if (position > mHeight) {
                cancel();
                position = 0;
                postInvalidate();
            }
        }
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
                // compute the target position
                float posx=(mWidth/2.0f)+((mDirection*4)*(position/mHeight));
                float posy=mHeight-position;
                // draw the grid
                mCanvas.drawLine(mWidth/3,0,mWidth/3,mHeight,mPaint);
                mCanvas.drawLine(mWidth/3.0f*2.0f,0,mWidth/3.0f*2.0f,mHeight,mPaint);
                mCanvas.drawLine(0,mHeight/3,mWidth,mHeight/3,mPaint);
                mCanvas.drawLine(0,mHeight/3.0f*2.0f,mWidth,mHeight/3.0f*2.0f,mPaint);
                // draw the target
                mCanvas.drawCircle( (mWidth/2.0f)+(mDirection*4), 16,16, mPaint3);


                // draw the shooting line
                if (position != 0.0f) {
                    switch (firMode) {
                        case 1:
                            //mCanvas.drawLine(mWidth / 2.0f, mHeight - position + 32, mWidth / 2.0f, mHeight - position, mPaint2);
                            //mCanvas.drawLine(mWidth / 2.0f, mHeight - position + 32, posx, posy, mPaint2);
                            mPaint2.setStrokeWidth(2);
                            mCanvas.drawCircle( posx, posy,mForce, mPaint2);
                            break;
                        case 2:
                            //mCanvas.drawLine(mWidth / 3.0f, mHeight, mWidth / 2.0f, mHeight - position, mPaint2);
                            //mCanvas.drawLine(mWidth / 3.0f * 2.0f, mHeight, mWidth / 2.0f, mHeight - position, mPaint2);
                            mPaint2.setStrokeWidth(32.0f * (mForce/100.0f));
                            mCanvas.drawLine(mWidth / 3.0f, mHeight, posx, posy, mPaint2);
                            mCanvas.drawLine(mWidth / 3.0f * 2.0f, mHeight, posx, posy, mPaint2);
                            break;
                    }
                }
                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

}

