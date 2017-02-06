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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mlsoft on 16-06-25.
 */
// =====================================================================================
// animated gif viewer class
public class GIFView extends View {
    public Movie mMovie=null;
    public long movieStart=0;
    private int gifId;
    private int position=0;

    public GIFView(Context context, int res) {
        super(context);
        setGIFResource(res);
    }

    public GIFView(Context context, AttributeSet attrs, int res) {
        super(context, attrs);
        setGIFResource(res);
    }

    public GIFView(Context context, AttributeSet attrs, int defStyle, int res) {
        super(context, attrs, defStyle);
        setGIFResource(res);
    }

    public void setGIFResource(int resId) {
        this.gifId = resId;
        initializeView();
    }

    public int getGIFResource() {
        return this.gifId;
    }

    private void initializeView() {
        // disable hardware acceleration
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        // decode the movie
        InputStream is = getContext().getResources().openRawResource(gifId);
        mMovie = Movie.decodeStream(is);
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
        timer = new Timer("warp");
        myTimer = new MyTimer();
        timer.schedule(myTimer, 100L, 100L);
    }

    private class MyTimer extends TimerTask {
        public void run() {
            position++;
            postInvalidate();
            if(position>=100) {
                position=1;
                postInvalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);
        // compute time elapsed
        long now = android.os.SystemClock.uptimeMillis();
        if (movieStart == 0) {
            movieStart = now;
        }
        if (mMovie != null) {
            int relTime = (int) ((now - movieStart) % mMovie.duration());
            mMovie.setTime(relTime);
            //mMovie.draw(canvas, 0,0);
            mMovie.draw(canvas, (getWidth() - mMovie.width())/2, (getHeight() - mMovie.height())/2);
            this.invalidate();
        }
    }

}
