package com.djsong.alkkago;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.Toast;

/**
 * Created by DJSong on 2016-03-19.
 * The black and white stone on the board
 * It has physical capabilities too.
 */
public class AKGStone extends AKGActor {

    /** WorldCoord in previous frame.
     * It is properly set only when the movement is done by physics update. */
    private AKGVector2D mPreviousPos = new AKGVector2D();
    public AKGVector2D GetPreviousPos() {return mPreviousPos;}

    private float mRadius = 3.0f;
    public float Radius() { return mRadius; }

    /** Black if true, white if false */
    private boolean mbIsBlackStone = true;
    public boolean IsBlackStone() { return mbIsBlackStone; }

    /** False means it is out of board area. */
    private boolean mbIsAlive = true;
    public void SetIsAlive(boolean bIn) { mbIsAlive = bIn; }
    public boolean IsAlive() { return mbIsAlive; }

    public AKGStone(boolean InBlack, float InRadius)
    {
        super();
        mbIsBlackStone = InBlack;
        SetSize(InRadius, InRadius);
    }

    public void SetPosition(float CoordX, float CoordY)
    {
        super.SetPosition(CoordX, CoordY);
        // Just put the same value for explicit SetPosition call. PreviousPos is updated as expected only for physics update.
        mPreviousPos.X = mWorldCoord.X;
        mPreviousPos.Y = mWorldCoord.Y;
    }

    public void SetSize(float InWidth, float InHeight) {
        super.SetSize(InWidth, InHeight);

        // BoundW/H affect radius
        mBoundWidth = Math.min(InWidth, InHeight);
        mBoundHeight = mBoundWidth;
        mRadius = mBoundWidth * 0.5f;
    }

    public boolean IsPointOverlap(float InX, float InY) {
        AKGVector2D DeltaV = new AKGVector2D(mWorldCoord.X - InX, mWorldCoord.Y - InY);
        return (DeltaV.GetLength() < mRadius);
    }
    /** It will make easier (less strict) check for SlackScale bigger than 1.0 */
    public boolean IsPointOverlapWithSlack(float InX, float InY, float SlackScale) {
        AKGVector2D DeltaV = new AKGVector2D(mWorldCoord.X - InX, mWorldCoord.Y - InY);
        return (DeltaV.GetLength() < mRadius * Math.max(1.0f, SlackScale));
    }

    //////////////////////////////////////////////////
    // For physics

    private final float mMinMovingSpeed = 0.1f; // Will be considered as stopped below this speed.
    public boolean IsMoving() {return (mVelocity.GetLength() >= mMinMovingSpeed); }

    private AKGVector2D mVelocity = new AKGVector2D();
    public AKGVector2D GetVelocity() {return mVelocity;}
    public float GetVelocityX() {return mVelocity.X;}
    public float GetVelocityY() {return mVelocity.Y;}
    private float mMass = 1.0f;
    public float GetMass() {return mMass;}
    private float mFriction = 5.0f;
    public float GetFriction() {return mFriction;}
    private float mElasticity = 0.8f; // Adjust this to compensate sloppy collision handling.
    public float GetElasticity() {return mElasticity;}

    /** Prevent something ugly.. */
    public static final float mStoneSpeedLimit = 250;

    public void PhysicsThread_UpdateMovement(float DeltaSeconds){
        mPreviousPos.X = mWorldCoord.X;
        mPreviousPos.Y = mWorldCoord.Y;
        synchronized (mVelocity) {
            mVelocity.ClampSize(mStoneSpeedLimit);
        }
        synchronized (mWorldCoord) {
            mWorldCoord.X += mVelocity.X * DeltaSeconds;
            mWorldCoord.Y += mVelocity.Y * DeltaSeconds;
        }

        synchronized (mVelocity) {
            // Damping by friction
            float DampedVelocityX = mVelocity.X - (mVelocity.X * mFriction * DeltaSeconds);
            if (DampedVelocityX * mVelocity.X > 0.0f) {
                mVelocity.X = DampedVelocityX;
            } else { // Set it to zero if damping is too much
                mVelocity.X = 0.0f;
            }

            float DampedVelocityY = mVelocity.Y - (mVelocity.Y * mFriction * DeltaSeconds);
            if (DampedVelocityY * mVelocity.Y > 0.0f) {
                mVelocity.Y = DampedVelocityY;
            } else { // Set it to zero if damping is too much
                mVelocity.Y = 0.0f;
            }
        }

        if (IsMoving() == false) {
            ForceStop();
        }

        // Collision detection and handling won't be done here.
    }

    /** Give some physical impulse by other's collision. */
    void OnPhysicsThread_CollideOtherStone(AKGStone other){
        AKGVector2D CollisionNormalA = new AKGVector2D(mWorldCoord.X - other.WorldCoordX(), mWorldCoord.Y - other.WorldCoordY());
        CollisionNormalA.NormalizeSelf();
        AKGVector2D CollisionNormalB = new AKGVector2D(CollisionNormalA); // Just the other way.
        CollisionNormalB.multiply(-1.0f);

        AKGVector2D OtherDir = new AKGVector2D(other.GetVelocity());
        float OtherSpeed = OtherDir.GetLength();
        OtherDir.NormalizeSelf();
        AKGVector2D MyDir = new AKGVector2D(mVelocity);
        float MySpeed = MyDir.GetLength();
        MyDir.NormalizeSelf();

        float CollisionDotA = (OtherSpeed > AKGUtil.KINDA_SMALL_NUMBER) ? Math.max(OtherDir.Dot(CollisionNormalA), 0.0f ) : 0.0f;
        float CollisionDotB = (MySpeed > AKGUtil.KINDA_SMALL_NUMBER) ? Math.max(MyDir.Dot(CollisionNormalB), 0.0f ) : 0.0f;
        if(CollisionDotA <= AKGUtil.KINDA_SMALL_NUMBER && CollisionDotB <= AKGUtil.KINDA_SMALL_NUMBER) {
            return; // In this case, two stones are going away
        }

        // I forgot to how to apply momentum correctly. Just going simple now..
        float CollisionImpulseSize = (CollisionDotA * OtherSpeed + CollisionDotB * MySpeed) *
                ( other.GetMass() / (other.GetMass() + mMass) ) *
                mElasticity * other.GetElasticity();

        AddImpulse( CollisionNormalA.X * CollisionImpulseSize, CollisionNormalA.Y * CollisionImpulseSize );
    }

    /** Give impulse force to accelerate at certain moment. */
    public void AddImpulse(float ForceX, float ForceY){
        float AccX = ForceX / mMass; // F= ma
        float AccY = ForceY / mMass;
        synchronized (mVelocity) {
            mVelocity.X += AccX; // When it is not continuous force like gravity..
            mVelocity.Y += AccY;
        }
    }
    public void ForceStop()
    {
        synchronized (mVelocity) {
            mVelocity.X = 0.0f;
            mVelocity.Y = 0.0f;
        }
    }

    //////////////////////////////////////////////////
    // Data and method for rendering.

    private float mRenderRaius = 10.0f;

    public void RenderThread_Draw(Canvas InDrawCanvas, Paint InDrawPaint, float WorldRenderScale){
        mRenderRaius = WorldRenderScale * mRadius;
        super.RenderThread_Draw(InDrawCanvas, InDrawPaint, WorldRenderScale);
    }

    public void RenderThread_DrawImpl(Canvas InDrawCanvas, Paint InDrawPaint) {
        InDrawPaint.setStrokeWidth(1);
        if(mbIsBlackStone) {
            InDrawPaint.setARGB(255, 0,0,0);
        }
        else{
            InDrawPaint.setARGB(255, 255,255,255);
        }
        // mRenderCoordX/Y are calculated as upper left coordinate, we need center coordinate here.
        InDrawCanvas.drawCircle(mRenderCoordX + mRenderRaius, mRenderCoordY + mRenderRaius,
                mRenderRaius, InDrawPaint);

        // Draw dead stone mark
        if(!IsAlive()) {
            if(mbIsBlackStone) {
                InDrawPaint.setARGB(255, 255,255,255); // In opposite color
            }
            else{
                InDrawPaint.setARGB(255, 0,0,0);
            }
            InDrawPaint.setTextSize(mRenderRaius * 2.0f);
            InDrawCanvas.drawText("X", mRenderCoordX + mRenderRaius * 0.4f,
                    mRenderCoordY + mRenderRaius * 1.6f, InDrawPaint);
        }
    }

}
