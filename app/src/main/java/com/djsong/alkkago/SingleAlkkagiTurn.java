package com.djsong.alkkago;

import java.util.ArrayList;

/**
 * Created by DJSong on 2016-03-24.
 * Represents a core element of alkkagi match, a turn to chance to kick a stone.
 */
public class SingleAlkkagiTurn {

    /** Stones list which can control for this turn */
    private ArrayList<AKGStone> mControlledStones = null;
    public ArrayList<AKGStone> GetControlledStones() {return mControlledStones;}
    private ArrayList<AKGStone> mOtherStones = null;

    private int mPlayerType = SingleAlkkagiMatch.ALKKAGI_PLAYER_UNKNOWN;
    public int GetPlayerType() {return mPlayerType;}
    /** Black if true, white if false. */
    private boolean mbIsBlack = false;
    public boolean IsBlackTurn() {return mbIsBlack;}

    /** Once it is being set to true, wait stones to settle down to end turn. */
    private boolean mbKickedForThisTurn = false;

    private boolean mbIsTurnEnd = false; // True possibly means this turn object is about to be destroyed..

    /** Valid only for the AI. Depend on the this turn's result (is kick/target stone dead) it will be between 0.0 ~ 1.0 */
    private float mbThisTurnSuccessRate = 0.0f;
    public float GetThisTurnSuccessRate() {return mbThisTurnSuccessRate;}

    // See also AlkkagiAI.TimedRecordWeight**
    public static final float TurnResultRate_CleanSuccess = 1.0f;
    public static final float TurnResultRate_BothDead = 0.6f;
    public static final float TurnResultRate_BothAlive = 0.1f;
    public static final float TurnResultRate_CleanFailure = 0.0f;

    private AKGStone CachedKickedStone = null;
    public AKGStone GetCachedKickedStone() {return CachedKickedStone;}
    private AKGStone CachedTargetStone = null; // It is likely to be valid only for AI turn.
    public AKGStone GetCachedTargetStone() {return CachedTargetStone;}

    public boolean IsThisTurnWaitingForKicking(){
        return (!mbKickedForThisTurn && !mbIsTurnEnd);
    }

    public SingleAlkkagiTurn(int InPlayerType, boolean bInIsBlack, ArrayList<AKGStone> InControlledStones, ArrayList<AKGStone> InOtherStones) {
        mPlayerType = InPlayerType;
        mbIsBlack = bInIsBlack;
        mControlledStones = InControlledStones;
        mOtherStones = InOtherStones;
    }

    /**
     * Send kicked stone, and intended target, in the case of AI.
     * */
    public void NotifyKickedAStoneForThisTurn(AKGStone KickedStone, AKGStone TargetIfAI){
        CachedKickedStone = KickedStone;
        CachedTargetStone = TargetIfAI;
        mbKickedForThisTurn = true;
    }

    public boolean PhysicsThread_CheckTurnEnd(){

        if(mbKickedForThisTurn == false){
            mbIsTurnEnd = false; // At least need to kick once.
        }
        else{
            boolean bAtLeastOneActive = false;
            for(int SI = 0; SI < mControlledStones.size(); ++SI) {
                if(mControlledStones.get(SI).IsAlive() && mControlledStones.get(SI).IsMoving()) {
                    bAtLeastOneActive = true;
                    break;
                }
            }
            if(bAtLeastOneActive == false) { // If nothing found yet, check for others too.
                for(int SI = 0; SI < mOtherStones.size(); ++SI) {
                    if(mOtherStones.get(SI).IsAlive() && mOtherStones.get(SI).IsMoving()) {
                        bAtLeastOneActive = true;
                        break;
                    }
                }
            }
            mbIsTurnEnd = !bAtLeastOneActive; // End only when nothing is active.
        }

        if(mbIsTurnEnd && CachedKickedStone != null && CachedTargetStone != null){
            // Check how was this turn
            if(!CachedTargetStone.IsAlive()){
                if(CachedKickedStone.IsAlive()) {
                    mbThisTurnSuccessRate = TurnResultRate_CleanSuccess; // Best result kk
                } else {
                    mbThisTurnSuccessRate = TurnResultRate_BothDead; // Both dead. It could be fine depend on the situation. (not counting it yet..)
                }
            }else{
                if(CachedKickedStone.IsAlive()) {
                    mbThisTurnSuccessRate = TurnResultRate_BothAlive; // Both alive
                } else {
                    mbThisTurnSuccessRate = TurnResultRate_CleanFailure; // Worst.
                }
            }
        }

        return mbIsTurnEnd;
    }
}
