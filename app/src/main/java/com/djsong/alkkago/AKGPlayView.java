package com.djsong.alkkago;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Created by DJSong on 2016-03-17.
 * The main view of Alkkagi game.
 */
public class AKGPlayView  extends SurfaceView implements SurfaceHolder.Callback {

    public AKGPlayWorld mPlayWorld = null;

    AlkkagiActivity mAlkkagiActivity = null;
    Context mCachedContext = null; // Is this necessary?

    //////////////////////////////////////////////////////////////////////
    // Some variables and objects for drawing..

    // Internal buffer bitmap for the internal buffer canvas
    private Bitmap mInternalBufferBitmap = null;
    /** Some object's dimension might depend on this. */
    private static final int mInternalBufferWidth = 1024;
    private static final int mInternalBufferHeight = 1200;
    /** Could give some scale later.. */
    public static int GetInternalBufferWidth() { return mInternalBufferWidth; }
    public static int GetInternalBufferHeight() { return mInternalBufferHeight; }
    // Internal buffer canvas to enable final scaling
    private Canvas mInternalBufferCanvas = null;

    private Paint mPaintObject = null;

    /** Real-time asynchronous rendering interface.. */
    private ImageRenderingThread mRenderingThread = null;

    /** Cached screen size from device. */
    private int mCachedScreenSizeX = 720;
    private int mCachedScreenSizeY = 1280;

    /** For present internal buffer to main canvas. */
    private int mPresentCoordX = 0;
    private int mPresentCoordY = 0;
    private float mPresentScaleX = 1.0f;
    private float mPresentScaleY = 1.0f;
    public int GetPresentCoordX() {return mPresentCoordX;}
    public int GetPresentCoordY() {return mPresentCoordY;}
    public float GetPresentScaleX() {return mPresentScaleX;}
    public float GetPresentScaleY() {return mPresentScaleY;}

    public AKGPlayView(Context context, AlkkagiActivity InActivity) {
        super(context);

        // Probably those two are just the same..
        mAlkkagiActivity = InActivity;
        mCachedContext = context;

        mPlayWorld = new AKGPlayWorld(this, mAlkkagiActivity);
        mPaintObject = new Paint();

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

    }

    /** It starts only if not started yet. */
    public void StartRenderingThread(){
        if(mRenderingThread == null) {
            SurfaceHolder holder = getHolder();
            holder.addCallback(this);
            mRenderingThread = new ImageRenderingThread(holder);
            mRenderingThread.start();
        }
    }
    public void StopRenderingThread(){
        if(mRenderingThread != null) {
            try{
                mRenderingThread.SendThreadStopSignal();
                mRenderingThread.join();
            } catch (InterruptedException ex) { }
            mRenderingThread = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // Any problem if this thread is started right after the creation?
        StartRenderingThread();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        mCachedScreenSizeX = width;
        mCachedScreenSizeY = height;


        // Set present scale to fit the internal buffer to screen
        float LocalScaleX = (float)mCachedScreenSizeX / (float)GetInternalBufferWidth();
        float LocalScaleY = (float)mCachedScreenSizeY / (float)GetInternalBufferHeight();
        // Get the least one for now..
        mPresentScaleX = Math.min(LocalScaleX, LocalScaleY);
        mPresentScaleY = Math.min(LocalScaleX, LocalScaleY);

        CreateInternalBuffer(width, height);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        StopRenderingThread();
    }

    private void CreateInternalBuffer(int ScreenSizeX, int ScreenSizeY)
    {
        // Well, just create with fixed size for now..
        mInternalBufferBitmap = Bitmap.createBitmap(GetInternalBufferWidth(), GetInternalBufferHeight(), Bitmap.Config.ARGB_8888);
        mInternalBufferCanvas = new Canvas();
        mInternalBufferCanvas.setBitmap(mInternalBufferBitmap);
    }

    public void RenderThread_UpdateInternalBuffer()
    {
        // The actual drawing process, but not presenting to the display
        // Call this for some event, or in real-time.. at least PresentToCanvas will be called in real time

        if(mInternalBufferBitmap != null && mInternalBufferCanvas != null)
        {
            mInternalBufferCanvas.drawColor(Color.WHITE); // Clearing
            mPlayWorld.RenderThread_Draw(mInternalBufferCanvas, mPaintObject);
        }
    }

    protected float GetPresentWidth() { return mInternalBufferBitmap.getWidth() * mPresentScaleX; }
    protected float GetPresentHeight() { return mInternalBufferBitmap.getHeight() * mPresentScaleY; }
    protected RectF GetPresentDestRect()
    {
        RectF ReturnRect = new RectF(mPresentCoordX, mPresentCoordY,
                GetPresentWidth() + mPresentCoordX, GetPresentHeight() + mPresentCoordY);
        return ReturnRect;
    }

    protected void RenderThread_PresentToCanvas(Canvas canvas)
    {
        // Here, we present the updated buffer (mInternalBufferBitmap) to the main canvas.
        // The scale and draw coordinate will be adjusted..

        if(mInternalBufferBitmap != null && canvas != null)
        {
            RectF DestRect = GetPresentDestRect();
            Rect SrcRect = new Rect(0, 0, mInternalBufferBitmap.getWidth(), mInternalBufferBitmap.getHeight());
            canvas.drawBitmap(mInternalBufferBitmap, SrcRect, DestRect, null);

            //Log.d("MapView", "DestRect " + DestRect.toString());
            //Log.d("MapView", "Center " + GetInternalBufferCoordOfScreenCenter().toString());
        }
    }


    ///// Stuff kick the stone by user interaction.
    private class TimedTouchListElemInfo
    {
        public AKGVector2D TouchPoint = new AKGVector2D();
        public long TimeStamp = 0;

        public TimedTouchListElemInfo(long InTime, float X, float Y)
        {
            TouchPoint.X = X;
            TouchPoint.Y = Y;
            TimeStamp = InTime;
        }
    }
    private ArrayList<TimedTouchListElemInfo> mTimedTouchList = new ArrayList<TimedTouchListElemInfo>();
    /**
     * The duration (in millisec) of mTimedTouchList element.
     * */
    private long mTouchListCacheDuration = 200;
    private float mStoneKickForceScale = 0.25f;
    private float mStoneKickSlack = 3.0f; // Bigger value will make kicking easier, by effectively consider stone's radius bigger.

    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();

        int TouchPointNum = event.getPointerCount();

        float X = event.getX();
        float Y = event.getY();

        UpdateTimedTouchList(X, Y);

        switch (action) {
            case MotionEvent.ACTION_UP:

                break;
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:
                TryKickAStoneByTouchList();
                break;
        }

        return true;
    }

    /**
     * Update the elements of mTimedTouchList
     * */
    private void UpdateTimedTouchList(float NewX, float NewY)
    {
        long CurrTime = System.currentTimeMillis();
        mTimedTouchList.add(new TimedTouchListElemInfo(CurrTime, NewX, NewY));

        // Remove old one
        for(int EI = 0; EI < mTimedTouchList.size(); ++EI)
        {
            TimedTouchListElemInfo CurrInfo = mTimedTouchList.get(EI);
            if( Math.abs(CurrInfo.TimeStamp - CurrTime) > mTouchListCacheDuration )
            {
                mTimedTouchList.remove(EI);
                --EI; // Removed current index element, so make it to get the object at the same index for next iteration.
            }
        }
    }

    void TryKickAStoneByTouchList(){
        if(mTimedTouchList.size() < 2){
            return;
        }

        // Well, looks like we just use the first and last element..
        TimedTouchListElemInfo MostRecentTouch = mTimedTouchList.get(mTimedTouchList.size() - 1);
        TimedTouchListElemInfo MostOldTouch = mTimedTouchList.get(0);

        AKGVector2D KickDirVector = new AKGVector2D(MostRecentTouch.TouchPoint.X - MostOldTouch.TouchPoint.X,
                MostRecentTouch.TouchPoint.Y - MostOldTouch.TouchPoint.Y);

        float MoveLength = KickDirVector.GetLength();
        KickDirVector.NormalizeSelf();
        float MoveTimeSec = (float)(MostRecentTouch.TimeStamp - MostOldTouch.TimeStamp) * 0.001f;

        // Give information to play world. It will decide if any one stone is to be kicked.
        // It will do some only when it is valid human player chance.
        mPlayWorld.TryKickAStoneByTouchInfo(MostRecentTouch.TouchPoint, KickDirVector, MoveLength, MoveTimeSec, mStoneKickForceScale, mStoneKickSlack);
    }


    //////////////////////////////////////////////////////////////////////
    // ImageRenderingThread.. for real time rendering

    class ImageRenderingThread extends Thread
    {
        SurfaceHolder mHolder;

        /** Thread will be running while this is true. */
        private boolean bContinueThread = true;
        /** It just send stop signal, you need to check IsThreadRunning afterward. */
        public void SendThreadStopSignal(){
            bContinueThread = false;
        }

        /**
         * The minimum time between rendering frame. Inverse of FPS, but in millisecond unit
         * */
        private long mMinFrameTime = 30;

        public ImageRenderingThread(SurfaceHolder InHolder)
        {
            super();

            mHolder = InHolder;
        }

        public void run()
        {
            while (bContinueThread)
            {
                long StartTickTime = System.currentTimeMillis();

                Canvas LockedCanvas = mHolder.lockCanvas(null);
                synchronized (mHolder) {
                    RenderThread_UpdateInternalBuffer(); // full real time
                    RenderThread_PresentToCanvas(LockedCanvas);
                }

                if (LockedCanvas != null) {
                    mHolder.unlockCanvasAndPost(LockedCanvas);
                }

                // Frame rate controlling

                long EndTickTime = System.currentTimeMillis();
                // Use the abs value because I guess the currentTimeMillis might return reset value at some time..?
                long FrameDelta = Math.abs(EndTickTime - StartTickTime);
                if(FrameDelta < mMinFrameTime && bContinueThread)
                {
                    try {
                        sleep(mMinFrameTime - FrameDelta);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    //////////////////////////////////////////////////////////////////////

}
