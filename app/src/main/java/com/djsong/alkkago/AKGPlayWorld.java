package com.djsong.alkkago;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by DJSong on 2016-03-18.
 * Probably the place that you put stuff in whenever in doubt.
 */
public class AKGPlayWorld {

    /**
     * Refer to each other
     */
    AKGPlayView mPlayView;

    AlkkagiActivity mAlkkagiActivity = null;

    /**
     * Manages gameplay with certain game rule.. To be created for each match.
     */
    SingleAlkkagiMatch mCurrentMatch = null;

    AlkkagiAI mAlkkagiAI = null;
    private boolean mbAITrainingMode = false;
    /**
     * We will run physics thread at max speed with fixed frame time, to speed up the training
     */
    private long mAITrainingModePhysicsFrameTime = 10;
    private long mAITrainingStartedTime = 0;
    private float mAITrainingLengthInSec = 0.0f; // Selected by user.

    public void SetAITrainingLengthInSec(float InValue) {
        mAITrainingLengthInSec = InValue;
    }

    private int mAITrainingSessionTimes = 0; // How many match gone through for training?

    /** Being set after human-AI match completed. */
    private boolean mbHumanJustWon = false;
    private boolean mbAIJustWon = false;
    private boolean mbJustTied = false;

    Bitmap mAIWinImage = null;
    Bitmap mHumanWinImage = null;

    /**
     * Scale from Actor's physical dimension to screen pixel.
     */
    private float WorldRenderingScale = 16.0f;

    public float GetWorldRenderingScale() {
        return WorldRenderingScale;
    }

    private AKGBoard mAlkkagiBoard = new AKGBoard();
    private ArrayList<AKGStone> mBlackStones = new ArrayList<AKGStone>();
    private ArrayList<AKGStone> mWhiteStones = new ArrayList<AKGStone>();
    private ArrayList<AKGStone> mAllStones = new ArrayList<AKGStone>(); // Simply sum of mBlackStones and mWhiteStones
    private final float AlkkagiBoardSize = 60.0f;
    private final float AlkkagiStoneRadius = 3.0f;
    /** How much initial placement deviation will be applied for AI training? */
    public final float AITrainingModeRandomPlacement = 3.0f;

    private PhysicsUpdateThread mPhysicsThread = null;

    private AIUpdateThread mAIThread = null;
    /** True runs AI update in AI thread, false runs in physics thread.
     * Running AI in separate thread can speed up the AI training, but not sure about thread safety.. Looks like sometimes getting in trouble..? */
    public final boolean mbUseSeparateAIThread = false;

    public AKGPlayWorld(AKGPlayView inPlayView, AlkkagiActivity InActivity) {
        mPlayView = inPlayView;
        mAlkkagiActivity = InActivity;

        // Must set objects size and coordinates.

        // Place the board at the center.
        mAlkkagiBoard.SetPosition(mPlayView.GetInternalBufferWidth() * 0.5f / WorldRenderingScale,
                mPlayView.GetInternalBufferHeight() * 0.5f / WorldRenderingScale);
        mAlkkagiBoard.SetSize(AlkkagiBoardSize, AlkkagiBoardSize);

        mAlkkagiAI = new AlkkagiAI(this, mAlkkagiBoard, mAlkkagiActivity);

        // Create stones
        int StoneNum = 5;
        for (int SI = 0; SI < StoneNum; ++SI) {
            AKGStone BlackStone = new AKGStone(true, AlkkagiStoneRadius);
            mBlackStones.add(BlackStone);
            mAllStones.add(BlackStone);

            AKGStone WhiteStone = new AKGStone(false, AlkkagiStoneRadius);
            mWhiteStones.add(WhiteStone);
            mAllStones.add(WhiteStone);
        }

        mAIWinImage = BitmapFactory.decodeResource(mPlayView.getResources(), R.drawable.alkkago_is_skynet);
        mHumanWinImage = BitmapFactory.decodeResource(mPlayView.getResources(), R.drawable.i_will_be_back);

        SetStonesAtStartingPosition(0.0f); // No need random placement for the first time

        StartPhysicsThread();
        StartAIThread();
    }

    /**
     * Start a match with choosing both player type, human or AI
     */
    public void StartSingleMatch(int BlackPlayerType, int WhitePlayerType, boolean bIsAITrainingMatch) {
        synchronized (this) {
            mCurrentMatch = new SingleAlkkagiMatch(this, mAlkkagiActivity, BlackPlayerType, WhitePlayerType, mBlackStones, mWhiteStones);
            mCurrentMatch.StartMatch(bIsAITrainingMatch);

            // Reset result variables
            mbHumanJustWon = false;
            mbAIJustWon = false;
            mbJustTied = false;
        }
    }

    public void StartHumanAIMatch() {
        StartSingleMatch(SingleAlkkagiMatch.ALKKAGI_PLAYER_HUMAN, SingleAlkkagiMatch.ALKKAGI_PLAYER_AI, false);
    }

    /** For non AI training mode, match complete notification comes from physics thread. */
    private void OnPhysicsThread_NonTrainingMatchComplete(){
        if(mCurrentMatch != null) { // mCurrentMatch must be valid yet
            if(mCurrentMatch.GetBlackStonePlayerType() != mCurrentMatch.GetWhiteStonePlayerType()) {
                // Human - AI match

                if (mCurrentMatch.IsTied()) {
                    // Some handling for tie?
                    mbJustTied = true;
                } else if ((mCurrentMatch.GetBlackStonePlayerType() == SingleAlkkagiMatch.ALKKAGI_PLAYER_HUMAN && mCurrentMatch.IsBlackWon())
                        || (mCurrentMatch.GetWhiteStonePlayerType() == SingleAlkkagiMatch.ALKKAGI_PLAYER_HUMAN && mCurrentMatch.IsWhiteWon())){
                    // Human win
                    mbHumanJustWon = true;
                }else {
                    // AI win
                    mbAIJustWon = true;
                }

                // Cache the time to show up some dialog..
                CachedMatchCompletedTime = System.currentTimeMillis();
                bWaitingForMatchCompleteDialog = true;
            }
        }
    }

    private boolean bWaitingForMatchCompleteDialog = false;
    private long CachedMatchCompletedTime = 0;
    private long MatchCompleteDialogShowUpTime = 5000; // Dialog will show up after this time.

    private AlertDialog CreateMatchCompleteDialogBox(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mAlkkagiActivity);

        builder.setTitle("Notification");
        builder.setMessage("다시 시도하시겠습니까? No 선택시 메인메뉴로 돌아갑니다.\nTry again? No to return to Main Menu.");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                StartHumanAIMatch();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mAlkkagiActivity.ReturnToMainMenu();
            }
        });

        AlertDialog dialog = builder.create();
        return dialog;
    }

    /** Initialize stone positions for play */
    public synchronized void SetStonesAtStartingPosition(float RandomPlaceDeviation)
    {
        for(int SI = 0; SI < mBlackStones.size(); ++SI)
        {
            AKGStone CurrStone = mBlackStones.get(SI);

            float XCoord = mAlkkagiBoard.LeftX() + ( (mAlkkagiBoard.Width() / (float)(mBlackStones.size() + 1)) * (float)(SI + 1) );
            float YCoord = mAlkkagiBoard.UpperY() + (mAlkkagiBoard.Height() / 4.0f) * 3.0f;
            if(RandomPlaceDeviation > 0.0f){
                XCoord += AKGUtil.RandRangeF(-1.0f * RandomPlaceDeviation, RandomPlaceDeviation);
                YCoord += AKGUtil.RandRangeF(-1.0f * RandomPlaceDeviation, RandomPlaceDeviation);
            }
            CurrStone.SetIsAlive(true);
            CurrStone.ForceStop();
            CurrStone.SetPosition(XCoord, YCoord);
        }
        for(int SI = 0; SI < mWhiteStones.size(); ++SI)
        {
            AKGStone CurrStone = mWhiteStones.get(SI);

            float XCoord = mAlkkagiBoard.LeftX() + ( (mAlkkagiBoard.Width() / (float)(mWhiteStones.size() + 1)) * (float)(SI + 1) );
            float YCoord = mAlkkagiBoard.UpperY() + (mAlkkagiBoard.Height() / 4.0f);
            if(RandomPlaceDeviation > 0.0f){
                XCoord += AKGUtil.RandRangeF(-1.0f * RandomPlaceDeviation, RandomPlaceDeviation);
                YCoord += AKGUtil.RandRangeF(-1.0f * RandomPlaceDeviation, RandomPlaceDeviation);
            }
            CurrStone.SetIsAlive(true);
            CurrStone.ForceStop();
            CurrStone.SetPosition(XCoord, YCoord);
        }
    }


    public AKGVector2D ScreenToWorldCoord(AKGVector2D InScreenCoord){
        AKGVector2D RetV = new AKGVector2D((InScreenCoord.X - mPlayView.GetPresentCoordX()) / (WorldRenderingScale * mPlayView.GetPresentScaleX()),
                (InScreenCoord.Y - mPlayView.GetPresentCoordY()) / (WorldRenderingScale * mPlayView.GetPresentScaleY()));
        return RetV;
    }
    public AKGVector2D WorldToScreenCoord(AKGVector2D InWorldCoord){
        AKGVector2D RetV = new AKGVector2D(InWorldCoord.X * WorldRenderingScale * mPlayView.GetPresentScaleX() + mPlayView.GetPresentCoordX(),
                InWorldCoord.Y * WorldRenderingScale * mPlayView.GetPresentScaleY() + mPlayView.GetPresentCoordY());
        return RetV;
    }

    /** Called from View with some refined touch information. Check if it is worth to kick a stone.
     * This is for human control, not for AI. (See TryKickAStone for AI stone kicking) */
    public void TryKickAStoneByTouchInfo(AKGVector2D LastTouchPoint, AKGVector2D TouchDir, float TouchedLength, float TouchedTimeSec, float KickForceScale, float TouchSlack) {
        if(TouchedTimeSec < AKGUtil.KINDA_SMALL_NUMBER) {
            return;
        }

        AKGVector2D LastTouchPointWorld = ScreenToWorldCoord(LastTouchPoint); // LastTouchPoint is in screen coordinate.

        // Get human controlled stone, if it is a valid chance for human player.
        ArrayList<AKGStone> HumanStones = (mCurrentMatch != null && mCurrentMatch.IsMatchStarted() && mCurrentMatch.IsWaitingForKickingThisTurn()) ?
                mCurrentMatch.GetHumanStonesIfCurrentHumanTurn() : null;
        if(HumanStones == null) { // Could be null if not current turn or if both are AI player.
            return;
        }

        // See if LastTouchPoint overlap any one human controlled stone.
        int OverlapStoneIndex = -1;
        for(int SI = 0; SI < HumanStones.size(); ++SI) {

            if(HumanStones.get(SI).IsAlive() && // Only for live stones
                    HumanStones.get(SI).IsPointOverlapWithSlack(LastTouchPointWorld.X, LastTouchPointWorld.Y, TouchSlack)){
                OverlapStoneIndex = SI;
                break;
            }
        }
        if(OverlapStoneIndex < 0){
            return;
        }

        float TouchForceSize = (TouchedLength / TouchedTimeSec) * KickForceScale; // Basically, scaled velocity
        // Final kicking.
        HumanStones.get(OverlapStoneIndex).AddImpulse(TouchDir.X * TouchForceSize, TouchDir.Y * TouchForceSize);

        if(mCurrentMatch != null){
            mCurrentMatch.NotifyKickedAStone(HumanStones.get(OverlapStoneIndex), null); // Human kicking don't have target
        }
    }

    /** Notified from AlkkagiMatch class at the end of turn, right before starting new turn */
    public void NotifyTurnEnd(SingleAlkkagiTurn JustEndedTurn){
        mAlkkagiAI.NotifyTurnEnd(JustEndedTurn);

    }

    private boolean mbIsCurrentlyBlackTurn = true; // Cached for each turn begin notification
    public void NotifyTurnBegin(SingleAlkkagiTurn JustBegunTurn){
        if(JustBegunTurn != null) {
            mbIsCurrentlyBlackTurn = JustBegunTurn.IsBlackTurn();
        }
    }

    //////////////////////////////////////////////////
    // Data and method for rendering. Just going simple..

    public void RenderThread_Draw(Canvas InDrawCanvas, Paint InDrawPaint)
    {
        // Do for sub objects..

        mAlkkagiBoard.RenderThread_Draw(InDrawCanvas, InDrawPaint, WorldRenderingScale);

        for(int SI = 0; SI < mAllStones.size(); ++SI) {
            mAllStones.get(SI).RenderThread_Draw(InDrawCanvas, InDrawPaint, WorldRenderingScale);
        }

        // Draw turn information
        if(!mbAITrainingMode) {
            InDrawPaint.setARGB(255, 127, 127, 127);

            float TurnInfoAreaWidth = mAlkkagiBoard.RenderWidth() * 0.3f;
            float TurnInfoAreaHeight = mAlkkagiBoard.RenderHeight() * 0.1f;
            float TurnInfoAreaX = mAlkkagiBoard.RenderCoordX() + mAlkkagiBoard.RenderWidth() - TurnInfoAreaWidth;
            float TurnInfoAreaY = mAlkkagiBoard.RenderCoordY() - TurnInfoAreaHeight * 1.2f;
            InDrawCanvas.drawRect(TurnInfoAreaX, TurnInfoAreaY, TurnInfoAreaX + TurnInfoAreaWidth, TurnInfoAreaY + TurnInfoAreaHeight, InDrawPaint);

            InDrawPaint.setTextSize(TurnInfoAreaHeight * 0.6f);
            if(mbIsCurrentlyBlackTurn) {
                InDrawPaint.setARGB(255,0,0,0);
            }else{
                InDrawPaint.setARGB(255,255,255,255);
            }
            InDrawCanvas.drawText( mbIsCurrentlyBlackTurn ? "Black Turn" : "White Turn",
                    TurnInfoAreaX + TurnInfoAreaWidth * 0.04f, TurnInfoAreaY + TurnInfoAreaHeight * 0.8f, InDrawPaint);
        }

        // Match result image
        if((mbHumanJustWon && mHumanWinImage != null) || (mbAIJustWon && mAIWinImage != null)) {
            Bitmap DrawResultImage = mbHumanJustWon ? mHumanWinImage : mAIWinImage;

                Rect SrcRect = new Rect(0, 0, DrawResultImage.getWidth(), DrawResultImage.getHeight());
                float ImageRatio = (float) DrawResultImage.getHeight() / (float) DrawResultImage.getWidth();

                float DestWidth = mAlkkagiBoard.RenderWidth(); // Let's fill the board
                float DestHeight = DestWidth * ImageRatio;
                RectF DestRect = new RectF(mAlkkagiBoard.RenderCoordX(), mAlkkagiBoard.RenderCoordY() + mAlkkagiBoard.RenderHeight() * 0.5f - DestHeight * 0.5f,
                        mAlkkagiBoard.RenderCoordX() + DestWidth, mAlkkagiBoard.RenderCoordY() + mAlkkagiBoard.RenderWidth() * 0.5f + DestHeight * 0.5f);

                InDrawCanvas.drawBitmap(DrawResultImage, SrcRect, DestRect, InDrawPaint);
        }


        if(mbAITrainingMode){ // Drawing some info.

            InDrawPaint.setARGB(255, 0, 0, 0);

            InDrawPaint.setTextSize(mAlkkagiBoard.RenderHeight() * 0.08f);
            InDrawCanvas.drawText("AlkkaGo AI Training..", mAlkkagiBoard.RenderCoordX() + mAlkkagiBoard.RenderWidth() * 0.1f,
                    mAlkkagiBoard.RenderCoordY() + mAlkkagiBoard.RenderHeight() * 0.4f, InDrawPaint);

            InDrawCanvas.drawText( (System.currentTimeMillis() - mAITrainingStartedTime) / 1000 + " Seconds", mAlkkagiBoard.RenderCoordX() + mAlkkagiBoard.RenderWidth() * 0.3f,
                    mAlkkagiBoard.RenderCoordY() + mAlkkagiBoard.RenderHeight() * 0.5f, InDrawPaint);

            InDrawPaint.setTextSize(mAlkkagiBoard.RenderHeight() * 0.075f);
            InDrawCanvas.drawText(mAITrainingSessionTimes + " Matches, " + mAlkkagiAI.GetTotalTrainingKickCount() + " Kicks",
                    mAlkkagiBoard.RenderCoordX() + mAlkkagiBoard.RenderWidth() * 0.1f,
                    mAlkkagiBoard.RenderCoordY() + mAlkkagiBoard.RenderHeight() * 0.6f, InDrawPaint);
        }
    }



    //////////////////////////////////////////////////////////////////////
    // To simulated stone's physical movment.

    void PhysicsThreadMain(float DeltaSeconds)
    {
        // Collision detection and handling of stones.
        for(int SIA = 0; SIA < mAllStones.size(); ++SIA) {
            AKGStone StoneA = mAllStones.get(SIA);
            if(!StoneA.IsAlive()){
                continue;
            }
            for(int SIB = SIA + 1; SIB < mAllStones.size(); ++SIB){
                AKGStone StoneB = mAllStones.get(SIB);
                if(!StoneB.IsAlive()){
                    continue;
                }

                // First, check with line from previous frame's position to current frame's position
                // It can check collision even when the stone moves too fast.
                boolean bCollideByLineCheck = AKGUtil.FiniteRayCircleIntersect(StoneA.GetPreviousPos(), StoneA.WorldCoord(), StoneB.WorldCoord(), StoneB.Radius());

                AKGVector2D PosDelta = new AKGVector2D(StoneA.WorldCoordX() - StoneB.WorldCoordX(), StoneA.WorldCoordY() - StoneB.WorldCoordY());
                boolean bCollideByCircleOverlapCheck = (PosDelta.GetLength() < (StoneA.Radius() + StoneB.Radius()));

                // Any one of check pass can be considered as collision.
                if(bCollideByLineCheck ||bCollideByCircleOverlapCheck) {
                    // Collision handling.
                    StoneA.OnPhysicsThread_CollideOtherStone(StoneB);
                    StoneB.OnPhysicsThread_CollideOtherStone(StoneA);
                }
            }
        }

        for(int SI = 0; SI < mAllStones.size(); ++SI) {
            mAllStones.get(SI).PhysicsThread_UpdateMovement(DeltaSeconds);

            // Check if stone is out of board (i.e. dead) too.
            if(mAlkkagiBoard.IsStoneInside(mAllStones.get(SI)) == false){
                mAllStones.get(SI).SetIsAlive(false);
            }
        }
    }

    /** It starts only if not started yet. */
    public void StartPhysicsThread(){
        if(mPhysicsThread == null) {
            mPhysicsThread = new PhysicsUpdateThread();
            mPhysicsThread.start();
        }
    }
    public void StopPhysicsThread(){
        if(mPhysicsThread != null) {
            try{
                mPhysicsThread.SendThreadStopSignal();
                mPhysicsThread.join();
            } catch (InterruptedException ex) { }
            mPhysicsThread = null;
        }
    }

    class PhysicsUpdateThread extends Thread
    {
        /** Thread will be running while this is true. */
        private boolean bContinueThread = true;
        /** It just send stop signal, you need to check IsThreadRunning afterward. */
        public void SendThreadStopSignal(){
            bContinueThread = false;
        }

        /**
         * The minimum time between update frame. Inverse of FPS, but in millisecond unit
         * */
        private long mMinFrameTime = 10;
        /** Must be bigger than mMinFrameTime. If unmodified frame time gets bigger than this, physics simulation will get slower. */
        private long mMaxFrameTime = 20;

        public PhysicsUpdateThread()
        {
            super();
        }

        public void run()
        {
            long FrameStartTickTime = System.currentTimeMillis();
            float LastFrameDeltaSecond = 0.0f;
            while (bContinueThread)
            {
                if(!mbAITrainingMode) {
                    // Take actual frame delta and mark start time.
                    // No use for training mode.
                    long CurrTime = System.currentTimeMillis();
                    LastFrameDeltaSecond = (float) (CurrTime - FrameStartTickTime) * 0.001f;
                    FrameStartTickTime = CurrTime;
                }

                // Consider synchronization for methods being called from here.
                {
                    PhysicsThreadMain(
                            mbAITrainingMode ? (float)mAITrainingModePhysicsFrameTime * 0.001f : // Training mode uses fixed frame time
                            Math.min(LastFrameDeltaSecond, mMaxFrameTime)
                    );

                    if(!mbUseSeparateAIThread){
                        // AIThreadMain running in physics thread kk. Slower than running in its own thread, but less worry.
                        AIThreadMain(LastFrameDeltaSecond);
                    }

                    // Update for match state
                    synchronized (this) {
                        if (mCurrentMatch != null && mCurrentMatch.IsMatchStarted()) {
                            mCurrentMatch.PhysicsThread_CheckMatch();

                            // Non training mode match complete handling
                            if(!mbAITrainingMode && mCurrentMatch.IsMatchCompleted() && !mCurrentMatch.IsAITrainingMatch()){
                                OnPhysicsThread_NonTrainingMatchComplete();

                                mCurrentMatch = null; // Done now..
                            }
                        }

                        // Enough time passed after match complete.
                        if(bWaitingForMatchCompleteDialog && (System.currentTimeMillis() - CachedMatchCompletedTime) > MatchCompleteDialogShowUpTime ){
                            bWaitingForMatchCompleteDialog = false;
                            // Pop-up some dialog for next action.
                            mAlkkagiActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    AlertDialog NewDlg = CreateMatchCompleteDialogBox();
                                    if(NewDlg != null) {
                                        NewDlg.show();
                                    }
                                }
                            });
                        }
                    }
                }


                if(!mbAITrainingMode) {
                    // Frame rate controlling.. No use for training mode

                    long EndTickTime = System.currentTimeMillis();
                    // Use the abs value because I guess the currentTimeMillis might return reset value at some time..?
                    long FrameDeltaSoFar = Math.abs(EndTickTime - FrameStartTickTime);

                    long RemainingTime = mMinFrameTime - FrameDeltaSoFar;
                    if (RemainingTime > 1 && bContinueThread) {
                        try {
                            if (RemainingTime > 5) { // Don't sleep for too short remaining time.
                                sleep(RemainingTime);
                            } else {
                                // Instead of sleep..
                                while (mMinFrameTime - Math.abs(System.currentTimeMillis() - FrameStartTickTime) > 0) {
                                }
                            }
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    //////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////
    // AI update loop and other stuff.


    public void StartAITraining(){

        mbAITrainingMode = true;
        mAITrainingStartedTime = System.currentTimeMillis();
        mAlkkagiAI.StartDeepShittingTrainig(); // Also set the AI object.

        StartSingleMatch(SingleAlkkagiMatch.ALKKAGI_PLAYER_AI, SingleAlkkagiMatch.ALKKAGI_PLAYER_AI, true);
    }
    public void ContinueAITraining() {
        if (mbAITrainingMode) {
            mAlkkagiAI.SingleDeepShittingSessionEnd(); // Notify AI to refine self based on last results.
            mAITrainingSessionTimes++;
            StartSingleMatch(SingleAlkkagiMatch.ALKKAGI_PLAYER_AI, SingleAlkkagiMatch.ALKKAGI_PLAYER_AI, true); // Continue match
        }
    }
    public void EndAITraining(){
        mbAITrainingMode = false;
        mAlkkagiAI.EndDeepShittingTrainig();
    }

    void AIThreadMain(float DeltaSeconds) {

        synchronized (this) {

            // Get if AI got a chance to do some..
            ArrayList<AKGStone> AIStones = (mCurrentMatch != null && mCurrentMatch.IsMatchStarted() && mCurrentMatch.IsWaitingForKickingThisTurn()) ?
                    mCurrentMatch.GetAIStonesIfCurrentAITurn() : null;

            if (AIStones != null && AIStones.size() > 0) { // Will be valid only when it is AI's chance.
                AlkkagiAI.KickTargetPickInfo KickedStoneInfo = mAlkkagiAI.TryKickAStone(AIStones, mCurrentMatch.GetStonesForOtherTurn(), mAllStones);

                if (KickedStoneInfo != null) {
                    mCurrentMatch.NotifyKickedAStone(KickedStoneInfo.KickStone, KickedStoneInfo.TargetStone);
                }
            }

            if (mbAITrainingMode) { // Deep shitting training mode handling.
                if (mCurrentMatch != null && mCurrentMatch.IsMatchCompleted()) { // Check match complete only for AI training mode.

                    if ((System.currentTimeMillis() - mAITrainingStartedTime) > (long) mAITrainingLengthInSec * 1000) {
                        // Time to end training, let's do the real stuff now.
                        EndAITraining();
                        StartHumanAIMatch();
                    } else {
                        ContinueAITraining();
                    }
                }
            }
        }
    }

    /** It starts only if not started yet. */
    public void StartAIThread(){
        if(mbUseSeparateAIThread) {
            if (mAIThread == null) {
                mAIThread = new AIUpdateThread();
                mAIThread.start();
            }
        }
    }
    public void StopAIThread(){
        if(mAIThread != null) {
            try{
                mAIThread.SendThreadStopSignal();
                mAIThread.join();
            } catch (InterruptedException ex) { }
            mAIThread = null;
        }
    }

    class AIUpdateThread extends Thread
    {
        /** Thread will be running while this is true. */
        private boolean bContinueThread = true;
        /** It just send stop signal, you need to check IsThreadRunning afterward. */
        public void SendThreadStopSignal(){
            bContinueThread = false;
        }

        /**
         * The minimum time between update frame. Inverse of FPS, but in millisecond unit
         * */
        private long mMinFrameTime = 10;

        public AIUpdateThread()
        {
            super();
        }

        public void run()
        {
            long FrameStartTickTime = System.currentTimeMillis();
            float LastFrameDeltaSecond = 0.0f;
            while (bContinueThread)
            {
                // Take actual frame delta and mark start time.
                long CurrTime = System.currentTimeMillis();
                LastFrameDeltaSecond = (float)(CurrTime - FrameStartTickTime) * 0.001f;
                FrameStartTickTime = CurrTime;

                if(mbUseSeparateAIThread){ // This thread won't be created if mbUseSeparateAIThread if false, but double check anyway
                    AIThreadMain(LastFrameDeltaSecond);
                }

                // Frame rate controlling
                if(!mbAITrainingMode) {
                    long EndTickTime = System.currentTimeMillis();
                    // Use the abs value because I guess the currentTimeMillis might return reset value at some time..?
                    long FrameDeltaSoFar = Math.abs(EndTickTime - FrameStartTickTime);
                    // Don't sleep for too short remaining time.
                    if ((mMinFrameTime - FrameDeltaSoFar) > 5 && bContinueThread) {
                        try {
                            sleep(mMinFrameTime - FrameDeltaSoFar);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }


}
