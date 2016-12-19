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
public class TrbSensorView extends TextView {
    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Canvas mCanvas = new Canvas();

    private int mWidth;
    private int mHeight;

    private int mode;   // 1=in 2=out
    private int freq;   // slow or fast in freq/100
    private int force=0;    // force of the beam
    private boolean rotate;  // true if we need to rotate frequency

    private int position=0;

    // initialize the 3 colors, and setup painter
    public TrbSensorView(Context context) {
        super(context);
        // text paint
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(24);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        // line paint
        mPaint2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setStrokeWidth(2);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.MAGENTA);
    }

    public void setmode(int no) {
        mode=no;
        if(mode==0) stop();
    }

    public void setfreq(int no) {
        freq = no;
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
        position=0;
        invalidate();
    }

    public void start() {
        // start the timer to eat this stuff and display it
        position=1;
        timer = new Timer("tractor");
        myTimer = new MyTimer();
        timer.schedule(myTimer, 10L, 10L);
    }

    private class MyTimer extends TimerTask {
        public void run() {
            position+=(1+freq);
            postInvalidate();
            if(position>=100) {
                position=1;
                if(rotate) freq++;
                if(freq>10) freq=0;
                postInvalidate();
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
                // draw the shield effect
                if(mode==1) mPaint2.setColor(Color.MAGENTA);
                else mPaint2.setColor(Color.BLUE);
                mPaint2.setStrokeWidth(1+force);
                if(position!=0) {
                    // compute positions
                    float px=mWidth/2;
                    float dx=position;
                    mCanvas.drawLine(px,mHeight,px,0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px-dx,0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px+dx,0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px-(dx/2),0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px+(dx/2),0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px-(dx/4),0,mPaint2);
                    mCanvas.drawLine(px,mHeight,px+(dx/4),0,mPaint2);
                }
                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

}

