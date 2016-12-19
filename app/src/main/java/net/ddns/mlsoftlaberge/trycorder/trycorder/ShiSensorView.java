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
public class ShiSensorView extends TextView {
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Paint mPaint3 = new Paint();
    private Canvas mCanvas = new Canvas();

    private int mWidth;
    private int mHeight;

    private int mode=1;   // 1=in 2=out
    private int freq=0;   // slow or fast in freq/100
    private int force=0;    // force of the beam
    private boolean rotate=false;  // true if we need to rotate frequency

    private int position=0;

    // initialize the 3 colors, and setup painter
    public ShiSensorView(Context context) {
        super(context);
        // text paint
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        // circle paint
        mPaint2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setStrokeWidth(2);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.GREEN);
        // line paint
        mPaint3.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint3.setStrokeWidth(2);
        mPaint3.setStyle(Paint.Style.STROKE);
        mPaint3.setColor(Color.LTGRAY);
    }

    public void setmode(int no) {
        mode=no;
    }

    public void setfreq(int no) {
        freq = no;
        invalidate();
    }

    public void setforce(int no) {
        force = no;
        invalidate();
    }

    public void setrotate(boolean rot) {
        rotate=rot;
    }


    // ======= timer section =======
    private Timer timer=null;
    private MyTimer myTimer;

    public void stop() {
        if(timer!=null) {
            timer.cancel();
            timer=null;
        }
    }

    public void start() {
        position=1;
        // start the timer to eat this stuff and display it
        timer = new Timer("shield");
        myTimer = new MyTimer();
        timer.schedule(myTimer, 10L, 10L);
    }

    private class MyTimer extends TimerTask {
        public void run() {
            position+=3;
            postInvalidate();
            if(position>=250) {
                if(rotate && mode==1) {
                    position=200;
                    freq++;
                    if(freq>10) freq=0;
                    postInvalidate();
                } else {
                    cancel();
                    position = 0;
                    postInvalidate();
                }
            }
        }
    }

    // =========== textview callbacks =================
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
                // ajust alpha of grid
                if(position==0) {
                    if(mode==1) {
                        mPaint3.setAlpha(255);
                    } else {
                        mPaint3.setAlpha(0);
                    }
                } else {
                    if (mode == 1) {
                        mPaint3.setAlpha(position);
                    } else {
                        mPaint3.setAlpha(255 - position);
                    }
                }
                mPaint3.setStrokeWidth(1+force);

                // draw the grid
                int grid=10+freq;
                for(int i=0;i<=grid;++i) {
                    mCanvas.drawLine(0,mHeight/grid*i,mWidth,mHeight/grid*i,mPaint3);
                    mCanvas.drawLine(mWidth/grid*i,0,mWidth/grid*i,mHeight,mPaint3);
                }
                // draw the circle effect
                //if(position!=0) {
                //    if (mode == 1) {
                //        mCanvas.drawCircle(mWidth / 2, mHeight / 2, position, mPaint2);
                //    } else {
                //        mCanvas.drawCircle(mWidth / 2, mHeight / 2, 250 - position, mPaint2);
                //    }
                //}
                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

}

