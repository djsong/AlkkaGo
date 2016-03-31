package com.djsong.alkkago;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by DJSong on 2016-03-19.
 * World drawable object base, such as board and stone.
 */
public class AKGActor {


    /** Physical dimensions */
    protected AKGVector2D mWorldCoord = new AKGVector2D(); // Origin at center
    protected float mBoundWidth = 10.0f;
    protected float mBoundHeight = 10.0f;

    public AKGActor(){

    }

    public void SetPosition(float CoordX, float CoordY)
    {
        mWorldCoord.X = CoordX;
        mWorldCoord.Y = CoordY;
    }
    public AKGVector2D WorldCoord() {return mWorldCoord;}
    public float WorldCoordX() {return mWorldCoord.X;}
    public float WorldCoordY() {return mWorldCoord.Y;}
    public float LeftX() {return mWorldCoord.X - mBoundWidth * 0.5f;}
    public float UpperY() {return mWorldCoord.Y - mBoundHeight * 0.5f;}
    public float RightX() {return mWorldCoord.X + mBoundWidth * 0.5f;}
    public float LowerY() {return mWorldCoord.Y + mBoundHeight * 0.5f;}

    public void SetSize(float InWidth, float InHeight) {
        mBoundWidth = InWidth;
        mBoundHeight = InHeight;
    }
    public float Width() { return mBoundWidth; }
    public float Height() { return mBoundHeight; }
    public float Radius() { return (mBoundWidth + mBoundHeight) * 0.5f; }




    //////////////////////////////////////////////////
    // Data and method for rendering.

    /** They are updated by draw call, based on this world coordinate and scale. */
    protected float mRenderCoordX = 0.0f; // It is calculated as upper left coordinate, not the center.
    public float RenderCoordX() {return mRenderCoordX;}
    protected float mRenderCoordY = 0.0f;
    public float RenderCoordY() {return mRenderCoordY;}
    protected float mRenderWidth = 10.0f;
    public float RenderWidth() {return mRenderWidth;}
    protected float mRenderHeight = 10.0f;
    public float RenderHeight() {return mRenderHeight;}

    public void RenderThread_Draw(Canvas InDrawCanvas, Paint InDrawPaint, float WorldRenderScale)
    {
        // Upper left based coordinates.
        float NonScaledRenderCoordX = mWorldCoord.X - mBoundWidth * 0.5f;
        float NonScaledRenderCoordY = mWorldCoord.Y - mBoundHeight * 0.5f;
        // Do some simple transformation. just scaling here.
        mRenderCoordX = NonScaledRenderCoordX * WorldRenderScale;
        mRenderCoordY = NonScaledRenderCoordY * WorldRenderScale;
        mRenderWidth = mBoundWidth * WorldRenderScale;
        mRenderHeight = mBoundHeight * WorldRenderScale;

        // Then, it's up to derived classes afterward.
        RenderThread_DrawImpl(InDrawCanvas, InDrawPaint);
    }

    /** To be overridden by extended classes. Called after render dimensions are set. */
    protected void RenderThread_DrawImpl(Canvas InDrawCanvas, Paint InDrawPaint){

    }

    //////////////////////////////////////////////////
}
