package com.djsong.alkkago;

import android.graphics.Canvas;
import android.graphics.Paint;


/**
 * Created by DJSong on 2016-03-18.
 * The board that stones on.
 */
public class AKGBoard extends AKGActor {

    public AKGBoard(){
        super();
    }

    public boolean IsStoneInside(AKGStone InStone) {
        if( InStone.WorldCoordX() >= LeftX() && InStone.WorldCoordX() <= RightX() &&
                InStone.WorldCoordY() >= UpperY() && InStone.WorldCoordY() <= LowerY()){
            return true;
        }
        return false;
    }
    public boolean IsPointInside(AKGVector2D InPoint) {
        if( InPoint.X >= LeftX() && InPoint.X <= RightX() &&
                InPoint.Y >= UpperY() && InPoint.Y <= LowerY()){
            return true;
        }
        return false;
    }

    /** Get the distance from designated point to any closest board edge in specified direction. */
    public float GetMinDistToEdgeInDirection(AKGVector2D InPoint, AKGVector2D InDirection){
        if(!IsPointInside(InPoint)){
            return 0.0f;
        }

        AKGVector2D CopiedDir = new AKGVector2D(InDirection);
        if(CopiedDir.GetLength() < AKGUtil.KINDA_SMALL_NUMBER){
            return 0.0f;
        }
        CopiedDir.NormalizeSelf();

        float DistInDirX = (CopiedDir.X == 0.0f) ? 0.0f :
                ((CopiedDir.X > 0.0f) ? (RightX() - InPoint.X) : InPoint.X - LeftX());
        float DistInDirY = (CopiedDir.Y == 0.0f) ? 0.0f :
                ((CopiedDir.Y > 0.0f) ? (LowerY() - InPoint.Y) : InPoint.Y - UpperY());

        if(DistInDirX == 0.0f){
            return DistInDirY;
        } else if(DistInDirY == 0.0f){
            return DistInDirX;
        }

        // Special case handlings are done.

        // Extend the direction along each x edge and y edge, then get the shortest.
        // The direction of ExtendToX and ExtendToY can be different from CopiedDir, they are just to see the length.
        AKGVector2D ExtendToX = new AKGVector2D(DistInDirX, (DistInDirX / CopiedDir.X) * CopiedDir.Y);
        AKGVector2D ExtendToY = new AKGVector2D((DistInDirY / CopiedDir.Y) * CopiedDir.X, DistInDirY);

        return Math.min( ExtendToX.GetLength(), ExtendToY.GetLength() );
    }

    //////////////////////////////////////////////////
    // Data and method for rendering.

    /** Approximate board color */
    private int DrawColorR = 245;
    private int DrawColorG = 200;
    private int DrawColorB = 100;

    public void RenderThread_DrawImpl(Canvas InDrawCanvas, Paint InDrawPaint)
    {
        InDrawPaint.setStrokeWidth(1);
        InDrawPaint.setARGB(255, DrawColorR, DrawColorG, DrawColorB);
        InDrawCanvas.drawRect(mRenderCoordX, mRenderCoordY, mRenderWidth + mRenderCoordX, mRenderHeight + mRenderCoordY, InDrawPaint);

        // Then, draw the 19 x 19 lines
        InDrawPaint.setARGB(255,0,0,0);

        float HLineSpace = (float)mRenderHeight / 20.0f;
        float VLineSpace = (float)mRenderWidth / 20.0f;

        float[] PointXCoords = new float[3];
        float[] PointYCoords = new float[3];
        int PointCoordCacheIndex = 0;

        InDrawPaint.setStrokeWidth(3);
        for(int LI = 1; LI <= 19; ++LI) {
            // Horizontal line
            float HLineYCoord = HLineSpace * LI + mRenderCoordY;
            InDrawCanvas.drawLine( VLineSpace + mRenderCoordX, HLineYCoord,
                    mRenderWidth - VLineSpace + mRenderCoordX, HLineYCoord, InDrawPaint );

            // Vertical line
            float VLineXCoord = VLineSpace * LI + mRenderCoordX;
            InDrawCanvas.drawLine( VLineXCoord, HLineSpace + mRenderCoordY,
                    VLineXCoord, mRenderHeight - HLineSpace + mRenderCoordY, InDrawPaint );

            // Cache x and y coordinates to draw 9 points
            if(LI == 4 || LI == 10 || LI == 16){
                PointXCoords[PointCoordCacheIndex] = VLineXCoord;
                PointYCoords[PointCoordCacheIndex] = HLineYCoord;
                ++PointCoordCacheIndex;
            }
        }

        // Draw the points using cached coordinates
        float PointDrawRadius = HLineSpace * 0.13f;
        for(int IX = 0; IX < PointXCoords.length; ++IX){
            for(int IY = 0; IY < PointYCoords.length; ++IY) {
                InDrawCanvas.drawCircle(PointXCoords[IX], PointYCoords[IY], PointDrawRadius, InDrawPaint);
            }
        }
    }

    //////////////////////////////////////////////////
}
