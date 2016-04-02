package com.djsong.alkkago;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by DJSong on 2016-03-25.
 * More or less manages the rule of game. A match is composed of multiple turns.
 */
public class SingleAlkkagiMatch {

    AKGPlayWorld mPlayWorld = null;

    AlkkagiActivity mAlkkagiActivity = null;

    /** Created for each turn */
    SingleAlkkagiTurn mCurrentTurn = null;

    /** Comes from PlayWorld. */
    private ArrayList<AKGStone> mBlackStones = null;
    private ArrayList<AKGStone> mWhiteStones = null;

    /** Definition of player type */
    public static final int ALKKAGI_PLAYER_UNKNOWN = 0;
    public static final int ALKKAGI_PLAYER_HUMAN = 1;
    public static final int ALKKAGI_PLAYER_AI = 2;

    /** It will define play type.. */
    private int mBlackStonePlayerType = ALKKAGI_PLAYER_HUMAN;
    public int GetBlackStonePlayerType() {return mBlackStonePlayerType;}
    private int mWhiteStonePlayerType = ALKKAGI_PLAYER_AI;
    public int GetWhiteStonePlayerType() {return mWhiteStonePlayerType;}

    /** Current turn is for black if true, white if false. */
    private boolean mbCurrentTurnBlack = true;

    private boolean mbMatchStarted = false;
    public boolean IsMatchStarted() {return mbMatchStarted;}
    private boolean mbMatchCompleted = false;
    public boolean IsMatchCompleted() {return mbMatchCompleted;}

    private boolean mbAllBlackStonesAreDead = false;
    private boolean mbAllWhiteStonesAreDead = false;
    public boolean IsBlackWon(){ // Is match completed and beat the white
        return (mbMatchCompleted && !mbAllBlackStonesAreDead && mbAllWhiteStonesAreDead);
    }
    public boolean IsWhiteWon(){ // Is match completed and beat the black
        return (mbMatchCompleted && mbAllBlackStonesAreDead && !mbAllWhiteStonesAreDead);
    }
    public boolean IsTied(){ // Is match completed and nobody won..
        return (mbMatchCompleted && mbAllBlackStonesAreDead && mbAllWhiteStonesAreDead);
    }

    private boolean mbIsAITrainingMatch = false; // Almost for Deep shitting AI training.
    public boolean IsAITrainingMatch() {return mbIsAITrainingMatch;}

    public SingleAlkkagiMatch(AKGPlayWorld InPlayWorld, AlkkagiActivity InActivity, int BlackPlayerType, int WhitePlayerType, ArrayList<AKGStone> BlackStoneList, ArrayList<AKGStone> WhiteStoneList){
        mPlayWorld = InPlayWorld;
        mAlkkagiActivity = InActivity;
        mBlackStonePlayerType = BlackPlayerType;
        mWhiteStonePlayerType = WhitePlayerType;
        mBlackStones = BlackStoneList;
        mWhiteStones = WhiteStoneList;
    }

    public void StartMatch(boolean bInAITrainingMatch){
        if(mPlayWorld == null || mBlackStones == null || mWhiteStones == null){
            return; // Cannot start..
        }

        mbMatchStarted = true;
        mbMatchCompleted = false;
        mbIsAITrainingMatch = bInAITrainingMatch; // Logs will be suppressed for this.

        // Give some random placement for AI training.
        mPlayWorld.SetStonesAtStartingPosition(mbIsAITrainingMatch ? mPlayWorld.AITrainingModeRandomPlacement : 0.0f);

        StartBlackTurn(); // Black is the first anyway.
    }

    private synchronized void StartBlackTurn(){
        mbCurrentTurnBlack = true;
        mCurrentTurn = new SingleAlkkagiTurn(mBlackStonePlayerType, mbCurrentTurnBlack, mBlackStones, mWhiteStones);
        mPlayWorld.NotifyTurnBegin(mCurrentTurn);
    }
    private synchronized void StartWhiteTurn(){
        mbCurrentTurnBlack = false;
        mCurrentTurn = new SingleAlkkagiTurn(mWhiteStonePlayerType, mbCurrentTurnBlack, mWhiteStones, mBlackStones);
        mPlayWorld.NotifyTurnBegin(mCurrentTurn);
    }

    public boolean IsCurrentlyHumanTurn(){
        return (
                (mbCurrentTurnBlack && mBlackStonePlayerType == ALKKAGI_PLAYER_HUMAN)
                || (!mbCurrentTurnBlack && mWhiteStonePlayerType == ALKKAGI_PLAYER_HUMAN)
        );
    }
    public boolean IsCurrentlyAITurn(){
        return (
                (mbCurrentTurnBlack && mBlackStonePlayerType == ALKKAGI_PLAYER_AI)
                        || (!mbCurrentTurnBlack && mWhiteStonePlayerType == ALKKAGI_PLAYER_AI)
        );
    }
    /** It will return valid array only if human player exists and currently human turn. */
    public synchronized ArrayList<AKGStone> GetHumanStonesIfCurrentHumanTurn(){
        if(mbCurrentTurnBlack && mBlackStonePlayerType == ALKKAGI_PLAYER_HUMAN){
            return mBlackStones;
        }
        else if(!mbCurrentTurnBlack && mWhiteStonePlayerType == ALKKAGI_PLAYER_HUMAN){
            return mWhiteStones;
        }
        return null;
    }
    /** It will return valid array only if AI player exists and currently AI turn. */
    public synchronized ArrayList<AKGStone> GetAIStonesIfCurrentAITurn(){
        if(mbCurrentTurnBlack && mBlackStonePlayerType == ALKKAGI_PLAYER_AI){
            return mBlackStones;
        }
        else if(!mbCurrentTurnBlack && mWhiteStonePlayerType == ALKKAGI_PLAYER_AI){
            return mWhiteStones;
        }
        return null;
    }
    /** Get stones list which is not for this turn, no matter human or AI. */
    public ArrayList<AKGStone> GetStonesForOtherTurn(){
        return mbCurrentTurnBlack ? mWhiteStones : mBlackStones;
    }

    public synchronized boolean IsWaitingForKickingThisTurn(){
        return (mCurrentTurn != null && mCurrentTurn.IsThisTurnWaitingForKicking());
    }

    /** Notification of stone kicking. AI kicking is expected to send the target information too. */
    public void NotifyKickedAStone(AKGStone KickedStone,AKGStone TargetIfAI){
        // Notify to turn, too.
        if(mCurrentTurn != null){
            mCurrentTurn.NotifyKickedAStoneForThisTurn(KickedStone, TargetIfAI);
        }
    }


    public void PhysicsThread_CheckMatch() {
        // Check if current turn is end, then switch turn

        if(mCurrentTurn != null && !mbMatchCompleted){
            if(mCurrentTurn.PhysicsThread_CheckTurnEnd()){
                // Turn check.

                // Before start the new turn, send play world some notification.
                mPlayWorld.NotifyTurnEnd(mCurrentTurn);

                // I would like to signal main thread to start turn is there some way?
                if (mbCurrentTurnBlack) {
                    StartWhiteTurn();

                    /*if(!mbIsAITrainingMatch) {
                        mAlkkagiActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(mAlkkagiActivity, "White Turn", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }*/
                } else {
                    StartBlackTurn();

                    /*if(!mbIsAITrainingMatch) {
                        mAlkkagiActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(mAlkkagiActivity, "Black Turn", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }*/
                }

            }
        }

        // Also check if all stones of one side are dead, then the match end..

        if(!mbMatchCompleted) {
            boolean bBlackStillActive = false;
            for (int SI = 0; SI < mBlackStones.size(); ++SI) {
                if (mBlackStones.get(SI).IsAlive()) {
                    bBlackStillActive = true;
                    break;
                }
            }
            mbAllBlackStonesAreDead = !bBlackStillActive;

            boolean bWhiteStillActive = false;
            for (int SI = 0; SI < mWhiteStones.size(); ++SI) {
                if (mWhiteStones.get(SI).IsAlive()) {
                    bWhiteStillActive = true;
                    break;
                }
            }
            mbAllWhiteStonesAreDead = !bWhiteStillActive;

            // In some occasion, both black and white might dead at the same time.

            if (mbAllBlackStonesAreDead || mbAllWhiteStonesAreDead) {
                mbMatchCompleted = true;

                if(!mbIsAITrainingMatch) {
                    mAlkkagiActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mAlkkagiActivity, "Match Completed! " +
                                            (IsBlackWon() ? "Black Won" : (IsWhiteWon() ? "White Won" : "Tied")),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }


                // @TODO Catch the match complete.. pop-up some dialog to give some selection..
            }
        }
    }
}
