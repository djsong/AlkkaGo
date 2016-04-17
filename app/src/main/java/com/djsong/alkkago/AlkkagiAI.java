package com.djsong.alkkago;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * A normal distributed variable that can be used for defining various AI behavior.
 * */
class NDAIVar{

    Random mRandObj = new Random();

    private float mMeanValue = 0.0f;
    public float MeanValue() {return mMeanValue;}

    private float mStdDev = 1.0f; // Standard deviation
    public float StdDev() {return mStdDev;}

    private boolean mbPositiveDistributeOnly = false; // You can still get negative value by adjusting mMeanValue.
    public boolean IsOnlyPositiveDistribution() {return mbPositiveDistributeOnly;}

    public NDAIVar(float InMeanValue, float InStdDev, boolean bInPositiveDistributeOnly){
        mMeanValue = InMeanValue;
        mStdDev = InStdDev;
        mbPositiveDistributeOnly = bInPositiveDistributeOnly;
    }
    /** To modify mean and standard deviation. */
    public void UpdateProperties(float InNewMean, float InNewStdDev){
        mMeanValue = InNewMean;
        mStdDev = InNewStdDev;
    }

    /**
     * You use this to get a final value in most common circumstance.
     * */
    public float GetSampleValue(){
        // Normal distribution.
        float OriginalDistribution = (float)mRandObj.nextGaussian();
        float ScaledResult = mMeanValue + (mbPositiveDistributeOnly ? Math.abs(OriginalDistribution) : OriginalDistribution) * mStdDev;
        return ScaledResult;
    }

    // To be added.. setting min and dev value according to some learned result.
}

/** All necessary AI variables struct */
class AKGAIVarSet{

    //////////////////////////////////////////////////
    /** KickForceScaleA will be applied to the distance between kicking stone and target stone */
    private NDAIVar mKickForceScaleA = new NDAIVar(1.0f, 3.5f, true); // Initial values.. to be somewhat dumb.
    public float GetSampleKickForceScaleA(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mKickForceScaleA.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mKickForceScaleA = ThisSample;
        }
        return ThisSample;
    }
    /** Update KickForceScaleA with new value's weight (0 ~ 1) to existing value */
    public void UpdateKickForceScaleA(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mKickForceScaleA, NewMean, NewStdDev, NewValueWeight);
    }

    //////////////////////////////////////////////////
    /** KickForceScaleA will be applied to the distance between target stone and its closest edge */
    private NDAIVar mKickForceScaleB = new NDAIVar(1.0f, 3.5f, true);
    public float GetSampleKickForceScaleB(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mKickForceScaleB.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mKickForceScaleB = ThisSample;
        }
        return ThisSample;
    }
    /** Update KickForceScaleB with new value's weight (0 ~ 1) to existing value */
    public void UpdateKickForceScaleB(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mKickForceScaleB, NewMean, NewStdDev, NewValueWeight);
    }

    //
    // @TODO It would be interesting to add some scale to remaining stone count of both sides.
    // Still it is just simple kicking AI, not like playing game in overall.
    //

    //////////////////////////////////////////////////
    /**
     * How much the AI can be out of exact target direction (in radian unit).
     * This is actually what AI can be good at. Just simulating human's behavior.
     * The sampled value will be directly applied to exact target direction, so set the MeanValue to zeor, and use StdDev only.
     * */
    private NDAIVar mKickDirDeviation = new NDAIVar(0.0f, 0.6f, false);
    public float GetSampleKickDirDeviation(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mKickDirDeviation.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mKickDirDeviation = ThisSample;
        }
        return ThisSample;
    }
    /** Update KickDirDeviation with new value's weight (0 ~ 1) to existing value */
    public void UpdateKickDirDeviation(float NewStdDev, float NewValueWeight){
        NewValueWeight = Math.min(1.0f, Math.max(0.0f, NewValueWeight));
        // Only requires standard deviation. Not using UpdateSingleAIVarCommon here.
        float FinalStdDev = mKickDirDeviation.StdDev() * (1.0f - NewValueWeight) + NewStdDev * NewValueWeight;
        mKickDirDeviation = new NDAIVar(0.0f, FinalStdDev, mKickDirDeviation.IsOnlyPositiveDistribution());
    }

    //////////////////////////////////////////////////
    /** To be applied to the inverse of distance from kick stone candidate and target candidate */
    private NDAIVar mTargetSelectScaleA = new NDAIVar(2.0f, 4.0f, false); // Better be positive. Initial value is dumb.
    public float GetSampleTargetSelectScaleA(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleA.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleA =ThisSample;
        }
        return ThisSample;
    }
    public void UpdateTargetSelectScaleA(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mTargetSelectScaleA, NewMean, NewStdDev, NewValueWeight);
    }

    //////////////////////////////////////////////////
    /** To be applied to the inverse of distance from target candidate and board edge */
    private NDAIVar mTargetSelectScaleB = new NDAIVar(2.0f, 4.0f, false); // Better be positive. Initial value is dumb.
    public float GetSampleTargetSelectScaleB(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleB.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleB =ThisSample;
        }
        return ThisSample;
    }
    public void UpdateTargetSelectScaleB(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mTargetSelectScaleB, NewMean, NewStdDev, NewValueWeight);
    }

    //////////////////////////////////////////////////
    /** To be applied to the inverse of distance between two and additionally from kicking candidate to board edge
     * It is about getting out from my dangerous state. */
    private NDAIVar mTargetSelectScaleC = new NDAIVar(2.0f, 4.0f, false); // Better be positive. Initial value is dumb.
    public float GetSampleTargetSelectScaleC(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleC.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleC =ThisSample;
        }
        return ThisSample;
    }
    public void UpdateTargetSelectScaleC(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mTargetSelectScaleC, NewMean, NewStdDev, NewValueWeight);
    }

    //////////////////////////////////////////////////
    /** To be applied to the number of (other side) stones in the same direction to the target */
    private NDAIVar mTargetSelectScaleExtra = new NDAIVar(0.0f, 1.0f, false); // Probably positive..?
    public float GetSampleTargetSelectScaleExtra(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleExtra.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleExtra =ThisSample;
        }
        return ThisSample;
    }
    public void UpdateTargetSelectScaleExtra(float NewMean, float NewStdDev, float NewValueWeight){
        UpdateSingleAIVarCommon(mTargetSelectScaleExtra, NewMean, NewStdDev, NewValueWeight);
    }

    //////////////////////////////////////////////////
    private void UpdateSingleAIVarCommon(NDAIVar InVarToUpdate, float NewMean, float NewStdDev, float NewValueWeight){
        NewValueWeight = Math.min(1.0f, Math.max(0.0f, NewValueWeight));
        float FinalMean = InVarToUpdate.MeanValue() * (1.0f - NewValueWeight) + NewMean * NewValueWeight;
        float FinalStdDev = InVarToUpdate.StdDev() * (1.0f - NewValueWeight) + NewStdDev * NewValueWeight;
        InVarToUpdate.UpdateProperties(FinalMean, FinalStdDev);
    }
}

/**
 * Info set to record successful trial, for AI learning.
 * */
class AKGAITrialRecordInfo{
    public AKGAITrialRecordInfo(){
    }

    float mWeightForKickVars = 0.0f; // 0.0 ~ 1.0 Bigger weight will be counted more.

    /** They reflects AKGAIVarSet */
    float mKickForceScaleA = 0.0f;
    float mKickForceScaleB = 0.0f;
    float mKickDirDeviation = 0.0f;


    //////////////////////////////////////////////////
    // We can separate this class from below.. They are actually treated separately.

    float mWeightForTargetSelVars = 0.0f; // 0.0 ~ 1.0 Bigger weight will be counted more.

    float mTargetSelectScaleA = 0.0f;
    float mTargetSelectScaleB = 0.0f;
    float mTargetSelectScaleC = 0.0f;
    float mTargetSelectScaleExtra = 0.0f;

    // Now, it doesn't look so clean, but we put some game state when this trial was recorded, to compare those later..
    int mLiveMyStoneNum = 0;
    int mLiveOtherStoneNum = 0;

    boolean bIsForBlackStone = true; // Just for some check or debugging.. White if false.
}

/**
 * Created by DJSong on 2016-03-26.
 * It can kick a stone by some conditions, and can learn some stuff by deep deep deep shitting algorithm.
 */
public class AlkkagiAI {

    private AKGPlayWorld mPlayWorld = null;
    private AKGBoard mAlkkagiBoard = null; // AI need to know the board
    private AlkkagiActivity mAlkkagiActivity = null;

    //////////////////////////////////////////////////
    // Variables to define AI property. AI will refine them by learning.

    /** To be used in most cases. */
    private AKGAIVarSet AIVarSetMain = new AKGAIVarSet();

    /** Sub variable set will be used for training. -> Not probably need for current simple scheme. */
    //private AKGAIVarSet AIVarSetSub = new AKGAIVarSet();

    private AKGAIVarSet GetThisTurnVarSet(){
        // Probably always use the same..
        return AIVarSetMain;
    }
    /** Just to prevent something ugly, not available for AI learning. */
   // private final float mKickForceLimit = 250.0f; // Let's clamp the velocity instead..

    //////////////////////////////////////////////////


    public AlkkagiAI(AKGPlayWorld InPlayWorld, AKGBoard InBoard, AlkkagiActivity InActivity) {
        mPlayWorld = InPlayWorld;
        mAlkkagiBoard = InBoard;
        mAlkkagiActivity = InActivity;
    }

    private KickTargetPickInfo CachedLastKickTarget = null; // Cached for single turn

    private AKGAITrialRecordInfo CachedTrialRecord_1 = null; // Cached AI sample variables of the most recent trial, regardless of stone type.

    private AKGAITrialRecordInfo CachedTrialRecord_2_B = null; // Cached AI sample variables of black stone's previous trial
    private AKGAITrialRecordInfo CachedTrialRecord_2_W = null; // Cached AI sample variables of white stone's previous trial

    /** The main job that AI will do.
     * AllStones is just the sum of ControllingStones and OtherStones.
     * It will returns valid reference if successfully kicked one with target.
     * Do not forget to notify match class object, by SingleAlkkagiMatch.NotifyKickedAStone if returns valid reference. */
    public KickTargetPickInfo TryKickAStone(ArrayList<AKGStone> ControllingStones, ArrayList<AKGStone> OtherStones, ArrayList<AKGStone> AllStones){

        // Create new one for this turn's AI var recording. Almost for mbDeepShittingMode but just do this for all time.
        CachedTrialRecord_1 = new AKGAITrialRecordInfo();

        // Before do the main stuff.. record some current state.. to compare the result later.
        CachedTrialRecord_1.mLiveMyStoneNum = 0;
        for(AKGStone CurrMy : ControllingStones){
            if(CurrMy.IsAlive()){
                ++CachedTrialRecord_1.mLiveMyStoneNum;
            }
            CachedTrialRecord_1.bIsForBlackStone = CurrMy.IsBlackStone(); // Not much here. Just wanna to check some stuff.. Could be just for debugging.
        }
        CachedTrialRecord_1.mLiveOtherStoneNum = 0;
        for(AKGStone CurrOther : OtherStones){
            if(CurrOther.IsAlive()){
                ++CachedTrialRecord_1.mLiveOtherStoneNum;
            }
        }

        // First job, select one kicking stone from my list and target stone from other list.
        KickTargetPickInfo PickedKickAndTarget = TryPickBestKickAndTargetStone(ControllingStones, OtherStones, AllStones, CachedTrialRecord_1);
        if(PickedKickAndTarget.KickStone == null || PickedKickAndTarget.TargetStone == null){
            return null; // Not a normal expected case. Probably found no live stones..
        }

        // Then, about how to kick it.
        AKGVector2D KickDirection = ComputeKickDirection(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, CachedTrialRecord_1);
        float KickForceSize = ComputeKickForceSize(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, KickDirection, CachedTrialRecord_1);
        //KickForceSize = Math.min(mKickForceLimit, KickForceSize); //-> Instead of limiting kicking force, limit the speed


        // Now, kick it!
        PickedKickAndTarget.KickStone.AddImpulse(KickDirection.X * KickForceSize, KickDirection.Y * KickForceSize);

        // Still need to notify SingleAlkkagiMatch object. See AKGPlayWorld.TryKickAStoneByTouchInfo

        CachedLastKickTarget = PickedKickAndTarget; // Cached it for later check, when it get notification of turn end from turn object.
        if(mbDeepShittingMode){
            ++mTotalTrainingKickCount;
        }

        return PickedKickAndTarget;
    }


    private float ComputeKickForceSize(AKGStone KickStone, AKGStone TargetStone, AKGVector2D KickDirection, AKGAITrialRecordInfo OptionalRecordInfo){

        float DistToTarget = AKGUtil.DistBetweenTwoStones(KickStone, TargetStone);

        AKGVector2D CopiedDirection = new AKGVector2D(KickDirection);
        CopiedDirection.NormalizeSelf();
        float DistFromTargetToEdge = mAlkkagiBoard.GetMinDistToEdgeInDirection(TargetStone.WorldCoord(), CopiedDirection);

        return (DistToTarget * GetThisTurnVarSet().GetSampleKickForceScaleA(OptionalRecordInfo) + DistFromTargetToEdge * GetThisTurnVarSet().GetSampleKickForceScaleB(OptionalRecordInfo));
    }

    private AKGVector2D ComputeKickDirection(AKGStone KickStone, AKGStone TargetStone, AKGAITrialRecordInfo OptionalRecordInfo) {
        AKGVector2D KickDirection = new AKGVector2D(TargetStone.WorldCoordX() - KickStone.WorldCoordX(), TargetStone.WorldCoordY() - KickStone.WorldCoordY());
        KickDirection.NormalizeSelf(); // Exact direction until this point

        // AI surely know the exact kicking direction. Here I put some random deviation to mimic human behavior and see some training effect.

        float ThisAngleDev = GetThisTurnVarSet().GetSampleKickDirDeviation(OptionalRecordInfo);
        KickDirection.RotateSelf(ThisAngleDev);

        return KickDirection;
    }


    /** Helper struct to choose a kick and target stone. */
    public class KickTargetPickInfo{
        public AKGStone KickStone = null; // One of final result
        public AKGStone TargetStone = null; // One of final result

        /** Bonus data to help choose the final target stone. Number of other stones in the same direction from KickStone to TargetStone. */
        public int AdditionalTargetNumInSameLine = 0;

        /** Cached score that to be compared to other pick info, to choose the best one.
         * Just put this here, expecting to be referred later..? */
        public float CachedTargetSelectScore = 0.0f;
    }

    /** The main entry point to get AI's kicking target. */
    private KickTargetPickInfo TryPickBestKickAndTargetStone(ArrayList<AKGStone> ControllingStones, ArrayList<AKGStone> OtherStones, ArrayList<AKGStone> AllStones, AKGAITrialRecordInfo OptionalRecordInfo){
        ArrayList<KickTargetPickInfo> AllPossiblePickInfo = new ArrayList<KickTargetPickInfo>();

        for(int CI = 0; CI < ControllingStones.size(); ++CI){

            AKGStone CurrKickCandid = ControllingStones.get(CI);

            if(CurrKickCandid.IsAlive()) { // Better not forget live check..
                ArrayList<KickTargetPickInfo> PickInfoListForCurrKickCandid = TryGetVisibleTargetPickInfoListOfSingleKickStone(CurrKickCandid, ControllingStones, OtherStones);

                // Just add all to AllPossiblePickInfo for later evaluation
                for(KickTargetPickInfo CurrInfo : PickInfoListForCurrKickCandid){
                    AllPossiblePickInfo.add(CurrInfo);
                }
            }
        }

        if(AllPossiblePickInfo.size() == 0){
            // In this case, we could not find any visible target stone. All other stones are blocked by one of my stones.
            // Just try find the closest one, not care about visibility stuff, we need to do some anyhow.

            for(int CI = 0; CI < ControllingStones.size(); ++CI){
                AKGStone CurrKickCandid = ControllingStones.get(CI);

                if(CurrKickCandid.IsAlive()) {
                    AKGStone PossibleTarget = TryPickClosestTargetOfSingleKickStone(CurrKickCandid, OtherStones);

                    if(PossibleTarget != null && PossibleTarget.IsAlive()){ // Double check for IsAlive
                        KickTargetPickInfo NewPossibleInfo = new KickTargetPickInfo();
                        NewPossibleInfo.KickStone = CurrKickCandid;
                        NewPossibleInfo.TargetStone = PossibleTarget;
                        AllPossiblePickInfo.add(NewPossibleInfo);
                    }
                }
            }
        }

        // If AllPossiblePickInfo is still empty yet, the game must be over..

        return FinalChooseMostReasonableTargetInfo(AllPossiblePickInfo, OptionalRecordInfo);
    }

    /** Finally choose the best KickTargetPickInfo from previously acquired possible list.
     * This is where AI do some regarding target choosing. */
    private KickTargetPickInfo FinalChooseMostReasonableTargetInfo(ArrayList<KickTargetPickInfo> InPossibleList, AKGAITrialRecordInfo OptionalRecordInfo){

        KickTargetPickInfo RetInfo = new KickTargetPickInfo();

        float BestScore = -1000000000000.0f; // Start from negative..
        for(int PPI = 0; PPI < InPossibleList.size(); ++PPI) {
            KickTargetPickInfo CurrInfo = InPossibleList.get(PPI);

            // Evaluate some variables to choose the best target.

            float DistBetweenTwo = AKGUtil.DistBetweenTwoStones(CurrInfo.KickStone, CurrInfo.TargetStone);

            AKGVector2D KickDirection = new AKGVector2D(CurrInfo.TargetStone.WorldCoordX() - CurrInfo.KickStone.WorldCoordX(), CurrInfo.TargetStone.WorldCoordY() - CurrInfo.KickStone.WorldCoordY());
            KickDirection.NormalizeSelf();
            float DistFromTargetToEdge = mAlkkagiBoard.GetMinDistToEdgeInDirection(CurrInfo.TargetStone.WorldCoord(), KickDirection);

            KickDirection.multiply(-1.0f); // In the opposite way. Possible direction of target stone kick to me at next turn
            float DistFromKickToEdge = mAlkkagiBoard.GetMinDistToEdgeInDirection(CurrInfo.KickStone.WorldCoord(), KickDirection);

            // Apply scales to get the final score..
            CurrInfo.CachedTargetSelectScore = GetThisTurnVarSet().GetSampleTargetSelectScaleA(OptionalRecordInfo) * (1.0f / DistBetweenTwo) + // Take inverse to consider less one higher.
                    GetThisTurnVarSet().GetSampleTargetSelectScaleB(OptionalRecordInfo) * (1.0f / DistFromTargetToEdge) +
                    GetThisTurnVarSet().GetSampleTargetSelectScaleC(OptionalRecordInfo) * (1.0f / (DistFromKickToEdge + DistBetweenTwo)) + // See how kick stone can be in danger at next turn.
                    // AdditionalTargetNumInSameLine is integer and can get easily bigger than other inversed value, consider that to set proper SampleTargetSelectScaleExtra.
                    GetThisTurnVarSet().GetSampleTargetSelectScaleExtra(OptionalRecordInfo) * (float)CurrInfo.AdditionalTargetNumInSameLine;

            if(CurrInfo.CachedTargetSelectScore > BestScore) {
                RetInfo = CurrInfo;
                BestScore = CurrInfo.CachedTargetSelectScore;
            }
        }

        return RetInfo;
    }

    /** Pick visible ones (not blocked by any other stones) from OtherStones list, to be the possible targets of KickStone.
     * It will return empty list if there's nothing visible and alive */
    private ArrayList<KickTargetPickInfo> TryGetVisibleTargetPickInfoListOfSingleKickStone(AKGStone KickStone, ArrayList<AKGStone> ControllingStones, ArrayList<AKGStone> OtherStones){

        ArrayList<KickTargetPickInfo> VisibleList = new ArrayList<KickTargetPickInfo>();

        for (int SI = 0; SI < OtherStones.size(); ++SI) {
            AKGStone TargetCandidate = OtherStones.get(SI);

            if(!TargetCandidate.IsAlive()){ // Don't care about dead one.
                continue;
            }

            AKGVector2D RayCheckOrigin = new AKGVector2D(KickStone.WorldCoord());
            AKGVector2D RayCheckDir = new AKGVector2D(TargetCandidate.WorldCoordX() - KickStone.WorldCoordX(), TargetCandidate.WorldCoordY() - KickStone.WorldCoordY());
            RayCheckDir.NormalizeSelf();

            // Check visibility if kicking to current candidate can set one of my (controlling) stones in danger.
            boolean bBlockedByOneOfMine = false;
            for(int CSI = 0; CSI < ControllingStones.size(); ++CSI){
                AKGStone CheckStone = ControllingStones.get(CSI);

                // Skip for our kick stone
                if( AKGUtil.AreTwoStonesEntirelyOverlap(KickStone, CheckStone) ){
                    continue;
                }

                if(AKGUtil.RayCircleIntersect(RayCheckOrigin, RayCheckDir, CheckStone.WorldCoord(), CheckStone.Radius())){
                    bBlockedByOneOfMine = true; // CheckStone blocks the way between KickStone and current target candidate.
                    break;
                }
            }

            // How many other stones are in the same direction to the target candidate?
            // In this case, the stones in the same line can be considered as a good chance to get multiple in a single kick or can be either considered as obstacle.
            int MoreOtherStonesNumInSameLine = 0;
            for (int OSI = 0; OSI < OtherStones.size(); ++OSI) {
                if(OSI == SI){
                    continue; // Skip for the same one.
                }
                AKGStone OtherStoneCheck = OtherStones.get(OSI);
                if(AKGUtil.RayCircleIntersect(RayCheckOrigin, RayCheckDir, OtherStoneCheck.WorldCoord(), OtherStoneCheck.Radius())){
                    ++MoreOtherStonesNumInSameLine;
                }
            }

            if(!bBlockedByOneOfMine) {
                // Now we can put this in visible list
                KickTargetPickInfo NewVisibleInfo = new KickTargetPickInfo();
                NewVisibleInfo.KickStone = KickStone;
                NewVisibleInfo.TargetStone = TargetCandidate;
                NewVisibleInfo.AdditionalTargetNumInSameLine = MoreOtherStonesNumInSameLine; // It will do some for choosing the final target among visible list.
                VisibleList.add(NewVisibleInfo);
            }
        }

        // It can be still empty if found no visible live target.
        return VisibleList;//TryPickClosestTargetOfSingleKickStone(KickStone, VisibleList);
    }

    /**
     * Simply pick closest target from OtherStones list. Does not care about visibility state.
     * It can still return null however, if no OtherStones are alive. (but in that case, game is already done)
     * */
    private AKGStone TryPickClosestTargetOfSingleKickStone(AKGStone KickStone, ArrayList<AKGStone> OtherStones) {

        AKGStone ClosestOne = null;
        float ClosestDist = 1000000000000.0f;

        for (int SI = 0; SI < OtherStones.size(); ++SI) {
            AKGStone CurrCandidate = OtherStones.get(SI);
            if(!CurrCandidate.IsAlive()){
                continue;
            }

            float CurrDist = AKGUtil.DistBetweenTwoStones(KickStone, CurrCandidate);

            if(CurrDist < ClosestDist){
                ClosestOne = CurrCandidate;
                ClosestDist = CurrDist;
            }
        }

        return ClosestOne; // It can be still null if no live target.
    }

    //////////////////////////////////////////////////

    /** Deep shitting AI training */
    private boolean mbDeepShittingMode = false;

    private int mTotalTrainingKickCount = 0; // For some display kk
    public int GetTotalTrainingKickCount() {return mTotalTrainingKickCount;}

    /** At least more than this number of samples gets counted to improve the AI var set. */
    private final int MinAIVarApplySampleNum = 50;

    /** AI var samples for one-time successful kicks will be recorded here until next AI update. */
    private ArrayList<AKGAITrialRecordInfo> SingleKickSuccessfulTrials = new ArrayList<AKGAITrialRecordInfo>();
    /** Another AI var samples, which count for some duration (probably just two turn though?) to see if previous selection was good. */
    private ArrayList<AKGAITrialRecordInfo> TimedSuccessfulTrials = new ArrayList<AKGAITrialRecordInfo>();

    // To calculate the weight for trial record. See also SingleAlkkagiTurn.TurnResultRate**
    static final float TimedRecordWeight_EqualResult = 0.1f; // Applied when the result after 2 turns are same for both side.
    static final float TimedRecordWeightScale = 0.5f; // Applied to the difference of dead stones of each side after 2 turns.

    public void StartDeepShittingTraining(){
        mbDeepShittingMode = true;

        // and.. if any
    }
    /** Typically be called on single match end. */
    public void SingleDeepShittingSessionEnd(){
        // Refine AIVar set based on last result.. but after we get enough samples.
        if(SingleKickSuccessfulTrials.size() >= MinAIVarApplySampleNum) {
            ApplySingleKickSuccessfulSamplesToAIVar();
        }
        if(TimedSuccessfulTrials.size() >= MinAIVarApplySampleNum){
            ApplyTimedSuccessfulSamplesToAIVar();
        }
    }
    public void EndDeepShittingTraining(){
        mbDeepShittingMode = false;

        // and.. if any
    }

    /** Notified from AlkkagiMatch and playworld class at the end of turn, right before starting new turn */
    public void NotifyTurnEnd(SingleAlkkagiTurn JustEndedTurn) {
        // Record successful AI trial
        if(JustEndedTurn.GetPlayerType() == SingleAlkkagiMatch.ALKKAGI_PLAYER_AI && mbDeepShittingMode) {

            // Record for single kick success.
            if(JustEndedTurn.GetThisTurnSuccessRate() >= SingleAlkkagiTurn.TurnResultRate_BothAlive) { // Let's count BothAlive case too, but with lower weight.
                // Check if notified turn was what we last kicked and targeted.
                if(JustEndedTurn.GetCachedKickedStone() == CachedLastKickTarget.KickStone && JustEndedTurn.GetCachedTargetStone() == CachedLastKickTarget.TargetStone) {
                    if(CachedTrialRecord_1 != null) { // Then we will trust this record.
                        CachedTrialRecord_1.mWeightForKickVars = JustEndedTurn.GetThisTurnSuccessRate();
                        SingleKickSuccessfulTrials.add(CachedTrialRecord_1);
                    }
                }
            }

            int CurrentLiveBlackStoneNum = mPlayWorld.GetAliveBlackStoneNum();
            int CurrentLiveWhiteStoneNum = mPlayWorld.GetAliveWhiteStoneNum();
            int MyStoneNumDelta = 0; // How many of my stones are dead from two turns before.
            int OtherStoneNumDelta = 0; // How many of other stones are dead from two turns before.
            AKGAITrialRecordInfo PrevRecordToCheck = null;
            // Then, record for target selection and two turn history.
            /*if(JustEndedTurn.IsBlackTurn() && CachedTrialRecord_2_B != null) {
                PrevRecordToCheck = CachedTrialRecord_2_B;
                // Check how many stones are dead for each side.
                MyStoneNumDelta = CurrentLiveBlackStoneNum - PrevRecordToCheck.mLiveMyStoneNum;
                OtherStoneNumDelta = CurrentLiveWhiteStoneNum - PrevRecordToCheck.mLiveOtherStoneNum;
            } else if(!JustEndedTurn.IsBlackTurn() && CachedTrialRecord_2_W != null){
                PrevRecordToCheck = CachedTrialRecord_2_W;
                // Check how many stones are dead for each side.
                OtherStoneNumDelta = CurrentLiveBlackStoneNum - PrevRecordToCheck.mLiveOtherStoneNum;
                MyStoneNumDelta = CurrentLiveWhiteStoneNum - PrevRecordToCheck.mLiveMyStoneNum;
            }*/

            // Here, we use just ended turn's record, instead of CachedTrialRecord_2* (record of two turns before). Looks like it works better.. but not sure..
            if(JustEndedTurn.IsBlackTurn() && CachedTrialRecord_1 != null) {
                PrevRecordToCheck = CachedTrialRecord_1;
                // Check how many stones are dead for each side.
                MyStoneNumDelta = CurrentLiveBlackStoneNum - PrevRecordToCheck.mLiveMyStoneNum;
                OtherStoneNumDelta = CurrentLiveWhiteStoneNum - PrevRecordToCheck.mLiveOtherStoneNum;
            } else if(!JustEndedTurn.IsBlackTurn() && CachedTrialRecord_1 != null){
                PrevRecordToCheck = CachedTrialRecord_1;
                // Check how many stones are dead for each side.
                OtherStoneNumDelta = CurrentLiveBlackStoneNum - PrevRecordToCheck.mLiveOtherStoneNum;
                MyStoneNumDelta = CurrentLiveWhiteStoneNum - PrevRecordToCheck.mLiveMyStoneNum;
            }

            // Definitely, it is better to kill other stones more than my stones.
            if(PrevRecordToCheck != null && MyStoneNumDelta > OtherStoneNumDelta){ // Let's not count for equal case..
                PrevRecordToCheck.mWeightForTargetSelVars = //(MyStoneNumDelta == OtherStoneNumDelta) ? TimedRecordWeight_EqualResult :
                        (float)(MyStoneNumDelta - OtherStoneNumDelta) * TimedRecordWeightScale; // More weight if killed other stone more.

                TimedSuccessfulTrials.add(PrevRecordToCheck);
            }

            // Before null out the CachedTrialRecord, save it for one more turn. We will see how target selection affects next turn.
            if(JustEndedTurn.IsBlackTurn()){
                CachedTrialRecord_2_B = CachedTrialRecord_1;
            } else{
                CachedTrialRecord_2_W = CachedTrialRecord_1;
            }

            CachedTrialRecord_1 = null;
        }
    }

    public class WeightedFloat{
        public WeightedFloat(float InW, float InV){Weight = InW; Value = InV;}
        public float Weight = 0.0f;
        public float Value = 0.0f;
    }

    private void ApplySingleKickSuccessfulSamplesToAIVar(){
        // Make each element's list
        ArrayList<WeightedFloat> KickForceScaleAList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> KickForceScaleBList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> KickDirDeviationList = new ArrayList<WeightedFloat>();
        for(int SI = 0; SI < SingleKickSuccessfulTrials.size(); ++SI){
            AKGAITrialRecordInfo ThisInfo = SingleKickSuccessfulTrials.get(SI);
            KickForceScaleAList.add(new WeightedFloat(ThisInfo.mWeightForKickVars, ThisInfo.mKickForceScaleA));
            KickForceScaleBList.add(new WeightedFloat(ThisInfo.mWeightForKickVars, ThisInfo.mKickForceScaleB));
            KickDirDeviationList.add(new WeightedFloat(ThisInfo.mWeightForKickVars, ThisInfo.mKickDirDeviation));
        }

        float MeanWeight = AKGUtil.GetMeanWeightFromWeightedFloatArray(KickForceScaleAList); // MeanWeight will be the AIVar final update weight.

        float ScaleAMean = AKGUtil.GetMeanFromWeightedFloatArray(KickForceScaleAList);
        float ScaleAStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleAList, ScaleAMean);

        float ScaleBMean = AKGUtil.GetMeanFromWeightedFloatArray(KickForceScaleBList);
        float ScaleBStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleBList, ScaleBMean);

        float DirDevMean = AKGUtil.GetMeanFromWeightedFloatArray(KickDirDeviationList);
        //float DirDevStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickDirDeviationList, DirDevMean);

        AIVarSetMain.UpdateKickForceScaleA(ScaleAMean, ScaleAStdDev, MeanWeight);
        AIVarSetMain.UpdateKickForceScaleB(ScaleBMean, ScaleBStdDev, MeanWeight);
        AIVarSetMain.UpdateKickDirDeviation(DirDevMean, MeanWeight); // Mean value is the deviation here.

        SingleKickSuccessfulTrials.clear(); // Then remove the history.
    }

    private void ApplyTimedSuccessfulSamplesToAIVar(){
        // Probably just the same..

        ArrayList<WeightedFloat> TargetSelectScaleAList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> TargetSelectScaleBList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> TargetSelectScaleCList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> TargetSelectScaleExtraList = new ArrayList<WeightedFloat>();
        for(int SI = 0; SI < TimedSuccessfulTrials.size(); ++SI){
            AKGAITrialRecordInfo ThisInfo = TimedSuccessfulTrials.get(SI);
            TargetSelectScaleAList.add(new WeightedFloat(ThisInfo.mWeightForTargetSelVars, ThisInfo.mTargetSelectScaleA));
            TargetSelectScaleBList.add(new WeightedFloat(ThisInfo.mWeightForTargetSelVars, ThisInfo.mTargetSelectScaleB));
            TargetSelectScaleCList.add(new WeightedFloat(ThisInfo.mWeightForTargetSelVars, ThisInfo.mTargetSelectScaleC));
            TargetSelectScaleExtraList.add(new WeightedFloat(ThisInfo.mWeightForTargetSelVars, ThisInfo.mTargetSelectScaleExtra));
        }

        float MeanWeight = AKGUtil.GetMeanWeightFromWeightedFloatArray(TargetSelectScaleAList); // MeanWeight will be the AIVar final update weight.

        float ScaleAMean = AKGUtil.GetMeanFromWeightedFloatArray(TargetSelectScaleAList);
        float ScaleAStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(TargetSelectScaleAList, ScaleAMean);

        float ScaleBMean = AKGUtil.GetMeanFromWeightedFloatArray(TargetSelectScaleBList);
        float ScaleBStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(TargetSelectScaleBList, ScaleBMean);

        float ScaleCMean = AKGUtil.GetMeanFromWeightedFloatArray(TargetSelectScaleCList);
        float ScaleCStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(TargetSelectScaleCList, ScaleCMean);

        float ScaleExtraMean = AKGUtil.GetMeanFromWeightedFloatArray(TargetSelectScaleExtraList);
        float ScaleExtraStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(TargetSelectScaleExtraList, ScaleExtraMean);

        AIVarSetMain.UpdateTargetSelectScaleA(ScaleAMean, ScaleAStdDev, MeanWeight);
        AIVarSetMain.UpdateTargetSelectScaleB(ScaleBMean, ScaleBStdDev, MeanWeight);
        AIVarSetMain.UpdateTargetSelectScaleC(ScaleCMean, ScaleCStdDev, MeanWeight);
        AIVarSetMain.UpdateTargetSelectScaleExtra(ScaleExtraMean, ScaleExtraStdDev, MeanWeight);

        TimedSuccessfulTrials.clear();
    }
}
