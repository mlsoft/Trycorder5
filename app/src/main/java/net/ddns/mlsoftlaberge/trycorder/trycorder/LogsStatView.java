package net.ddns.mlsoftlaberge.trycorder.trycorder;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import net.ddns.mlsoftlaberge.trycorder.utils.Fetcher;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mlsoft on 16-06-25.
 */
// ============================================================================
// class defining the sensor display widget
public class LogsStatView extends TextView {

    private Context context;
    private ActivityManager activityManager;

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    private Paint mPaint3 = new Paint();
    private Canvas mCanvas = new Canvas();

    private int mWidth;
    private int mHeight;

    private int mode;   // 1=in 2=out

    private int position=0;

    // initialize the 3 colors, and setup painter
    public LogsStatView(Context ctx) {
        super(ctx);
        context=ctx;
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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

    // ======= timer section =======
    private Timer timer=null;
    private MyTimer myTimer=null;

    public void stop() {
        if(timer!=null) {
            timer.cancel();
            timer=null;
        }
        position=0;
    }

    public void start() {
        position=0;
        getvalues();
        // start the timer to eat this stuff and display it
        if(timer==null) {
            timer = new Timer("shield");
            myTimer = new MyTimer();
            timer.schedule(myTimer, 100L, 100L);
        }
    }

    private class MyTimer extends TimerTask {
        public void run() {
            position+=1;
            if(position>=10) {
                position=0;
            }
            if(position==1) getvalues();
            postInvalidate();
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
                // draw the grid
                for(int i=0;i<10;++i) {
                    mCanvas.drawLine(0,mHeight/10*i,mWidth,mHeight/10*i,mPaint3);
                    //mCanvas.drawLine(mWidth/10*i,0,mWidth/10*i,mHeight,mPaint3);
                }

                String text;
                text = String.format("Total Mem: %d",memtotal);
                mCanvas.drawText(text, 0, mHeight/10-8, mPaint);

                text = String.format("Free Mem: %d",memfree);
                mCanvas.drawText(text, 0, mHeight/10*2-8, mPaint);

                // transfer the bitmap to the view
                viewcanvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        super.onDraw(viewcanvas);
    }

    // values to update every second on screen
    private long memtotal;
    private long memfree;

    private void getvalues() {
        ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(outInfo);
        memtotal=outInfo.totalMem;
        memfree=outInfo.availMem;
    }

}

