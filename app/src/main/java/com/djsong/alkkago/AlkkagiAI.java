package com.djsong.alkkago;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * A normal distributed variable that can be used for defining various AI behavior.
 * */
class NDAIVar{

    Random mRandObj = new Random();

    private float mMinValue = 0.0f;
    public float MinValue() {return mMinValue;}

    private float mStdDev = 1.0f; // Standard deviation
    public float StdDev() {return mStdDev;}

    private boolean mbPositiveDistributeOnly = false; // You can still get negative value by adjusting mMinValue.
    public boolean IsOnlyPositiveDistribution() {return mbPositiveDistributeOnly;}

    public NDAIVar(float InMinValue, float InStdDev, boolean bInPositiveDistributeOnly){
        mMinValue = InMinValue;
        mStdDev = InStdDev;
        mbPositiveDistributeOnly = bInPositiveDistributeOnly;
    }

    /**
     * You use this to get a final value in most common circumstance.
     * */
    public float GetSampleValue(){
        // Normal distribution.
        float OriginalDistribution = (float)mRandObj.nextGaussian();
        float ScaledResult = mMinValue + (mbPositiveDistributeOnly ? Math.abs(OriginalDistribution) : OriginalDistribution) * mStdDev;
        return ScaledResult;
    }

    // To be added.. setting min and dev value according to some learned result.
}

/** All necessary AI variables struct */
class AKGAIVarSet{

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
    public void UpdateKickForceScaleA(float NewMin, float NewStdDev, float NewValueWeight){
        float FinalMin = mKickForceScaleA.MinValue() * (1.0f - NewValueWeight) + NewMin * NewValueWeight;
        float FinalStdDev = mKickForceScaleA.StdDev() * (1.0f - NewValueWeight) + NewStdDev * NewValueWeight;
        mKickForceScaleA = new NDAIVar(FinalMin, FinalStdDev, true);
    }

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
    public void UpdateKickForceScaleB(float NewMin, float NewStdDev, float NewValueWeight){
        float FinalMin = mKickForceScaleB.MinValue() * (1.0f - NewValueWeight) + NewMin * NewValueWeight;
        float FinalStdDev = mKickForceScaleB.StdDev() * (1.0f - NewValueWeight) + NewStdDev * NewValueWeight;
        mKickForceScaleB = new NDAIVar(FinalMin, FinalStdDev, true);
    }

    //
    // @TODO It would be interesting to add some scale to remaining stone count of both sides.
    // Still it is just simple kicking AI, not like playing game in overall.
    //

    /**
     * How much the AI can be out of exact target direction (in radian unit).
     * This is actually what AI can be good at. Just simulating human's behavior.
     * The sampled value will be directly applied to exact target direction, so set the MinValue to zeor, and use StdDev only.
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
        // Only requires standard deviation.
        float FinalStdDev = mKickDirDeviation.StdDev() * (1.0f - NewValueWeight) + NewStdDev * NewValueWeight;
        mKickDirDeviation = new NDAIVar(0.0f, FinalStdDev, false);
    }


    /** To be applied to the inverse of distance from kick stone candidate and target candidate */
    private NDAIVar mTargetSelectScaleA = new NDAIVar(20.0f, 10.0f, true);
    public float GetSampleTargetSelectScaleA(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleA.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleA =ThisSample;
        }
        return ThisSample;
    }


    /** To be applied to the inverse of distance from target candidate and board edge */
    private NDAIVar mTargetSelectScaleB = new NDAIVar(10.0f, 10.0f, true);
    public float GetSampleTargetSelectScaleB(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleB.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleB =ThisSample;
        }
        return ThisSample;
    }

    /** To be applied to the inverse of distance between two and additionally from kick candidate and board edge (To get out from my dangerous state) */
    private NDAIVar mTargetSelectScaleC = new NDAIVar(20.0f, 10.0f, true);
    public float GetSampleTargetSelectScaleC(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleC.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleC =ThisSample;
        }
        return ThisSample;
    }

    /** To be applied to the number of stones in the same direction */
    private NDAIVar mTargetSelectScaleExtra = new NDAIVar(1.0f, 1.0f, false); // Probably positive..?
    public float GetSampleTargetSelectScaleExtra(AKGAITrialRecordInfo OptionalRecordInfo){
        float ThisSample = mTargetSelectScaleExtra.GetSampleValue();
        if(OptionalRecordInfo != null){
            OptionalRecordInfo.mTargetSelectScaleExtra =ThisSample;
        }
        return ThisSample;
    }
}

/**
 * Info set to record successful trial, for AI learning.
 * */
class AKGAITrialRecordInfo{
    public AKGAITrialRecordInfo(){
    }

    float mWeight = 0.0f; // 0.0 ~ 1.0 Bigger weight will be counted more.

    /** They reflects AKGAIVarSet */
    float mKickForceScaleA = 0.0f;
    float mKickForceScaleB = 0.0f;
    float mKickDirDeviation = 0.0f;

    float mTargetSelectScaleA = 0.0f;
    float mTargetSelectScaleB = 0.0f;
    float mTargetSelectScaleC = 0.0f;
    float mTargetSelectScaleExtra = 0.0f;
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
    private AKGAITrialRecordInfo CachedLastTrialRecord = null;

    /** The main job that AI will do.
     * AllStones is just the sum of ControllingStones and OtherStones.
     * It will returns valid reference if successfully kicked one with target.
     * Do not forget to notify match class object, by SingleAlkkagiMatch.NotifyKickedAStone if returns valid reference. */
    public KickTargetPickInfo TryKickAStone(ArrayList<AKGStone> ControllingStones, ArrayList<AKGStone> OtherStones, ArrayList<AKGStone> AllStones){

        // Create new one for this turn's AI var recording. Almost for mbDeepShittingMode but just do this for all time.
        CachedLastTrialRecord = new AKGAITrialRecordInfo();

        KickTargetPickInfo PickedKickAndTarget = TryPickBestKickAndTargetStone(ControllingStones, OtherStones, AllStones, CachedLastTrialRecord);
        if(PickedKickAndTarget.KickStone == null || PickedKickAndTarget.TargetStone == null){
            return null; // Not a normal expected case. Probably found no live stones..
        }

        AKGVector2D KickDirection = ComputeKickDirection(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, CachedLastTrialRecord);
        float KickForceSize = ComputeKickForceSize(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, KickDirection, CachedLastTrialRecord);
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

        /** Cached score that to be compared to other pick info, to choose the best one. */
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
            int OtherStonesNumInSameLine = 0;
            for (int OSI = 0; OSI < OtherStones.size(); ++OSI) {
                if(OSI == SI){
                    continue; // Skip for the same one.
                }
                AKGStone OtherStoneCheck = OtherStones.get(OSI);
                if(AKGUtil.RayCircleIntersect(RayCheckOrigin, RayCheckDir, OtherStoneCheck.WorldCoord(), OtherStoneCheck.Radius())){
                    ++OtherStonesNumInSameLine;
                }
            }

            if(!bBlockedByOneOfMine) {
                // Now we can put this in visible list
                KickTargetPickInfo NewVisibleInfo = new KickTargetPickInfo();
                NewVisibleInfo.KickStone = KickStone;
                NewVisibleInfo.TargetStone = TargetCandidate;
                NewVisibleInfo.AdditionalTargetNumInSameLine = OtherStonesNumInSameLine; // It will do some for choosing the final target among visible list.
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

    /** Notified from AlkkagiMatch and playworld class at the end of turn, right before starting new turn */
    public void NotifyTurnEnd(SingleAlkkagiTurn JustEndedTurn) {
        // Record successful AI trial
        if(JustEndedTurn.GetPlayerType() == SingleAlkkagiMatch.ALKKAGI_PLAYER_AI && mbDeepShittingMode) {
            if(JustEndedTurn.GetThisTurnSuccessRate() >= SingleAlkkagiTurn.TurnResultRate_BothAlive) { // Let's count BothAlive case too, but with lower weight.
                // Check if notified turn was what we last kicked and targeted.
                if(JustEndedTurn.GetCachedKickedStone() == CachedLastKickTarget.KickStone && JustEndedTurn.GetCachedTargetStone() == CachedLastKickTarget.TargetStone) {
                    if(CachedLastTrialRecord != null) { // Then we will trust this record.
                        CachedLastTrialRecord.mWeight = JustEndedTurn.GetThisTurnSuccessRate();
                        SuccessfulTrials.add(CachedLastTrialRecord);
                        CachedLastTrialRecord = null;
                    }
                }
            }
        }
    }

    /** Deep shitting AI training */
    private boolean mbDeepShittingMode = false;

    private int mTotalTrainingKickCount = 0; // For some display kk
    public int GetTotalTrainingKickCount() {return mTotalTrainingKickCount;}

    /** At least more than this number of samples gets counted to improve the AI var set. */
    private final int MinAIVarApplySampleNum = 50;

    /** Successfully kicked samples will be recorded here until next AI update. */
    private ArrayList<AKGAITrialRecordInfo> SuccessfulTrials = new ArrayList<AKGAITrialRecordInfo>();

    public void StartDeepShittingTrainig(){
        mbDeepShittingMode = true;

        // and.. if any
    }
    /** Typically be called on single match end. */
    public void SingleDeepShittingSessionEnd(){
        // Refine AIVar set based on last result.. but after we get enough samples.
        if(SuccessfulTrials.size() >= MinAIVarApplySampleNum) {
            ApplySuccessfulSamplesToAIVar();
        }
    }
    public void EndDeepShittingTrainig(){

        mbDeepShittingMode = false;

        // and.. if any
    }

    public class WeightedFloat{
        public WeightedFloat(float InW, float InV){Weight = InW; Value = InV;}
        public float Weight = 0.0f;
        public float Value = 0.0f;
    }

    private void ApplySuccessfulSamplesToAIVar(){
        // Make each element's list
        ArrayList<WeightedFloat> KickForceScaleAList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> KickForceScaleBList = new ArrayList<WeightedFloat>();
        ArrayList<WeightedFloat> KickDirDeviationList = new ArrayList<WeightedFloat>();
        for(int SI = 0; SI < SuccessfulTrials.size(); ++SI){
            AKGAITrialRecordInfo ThisInfo = SuccessfulTrials.get(SI);
            KickForceScaleAList.add(new WeightedFloat(ThisInfo.mWeight, ThisInfo.mKickForceScaleA));
            KickForceScaleBList.add(new WeightedFloat(ThisInfo.mWeight, ThisInfo.mKickForceScaleB));
            KickDirDeviationList.add(new WeightedFloat(ThisInfo.mWeight, ThisInfo.mKickDirDeviation));
        }

        float MinWeight = AKGUtil.GetMeanWeightFromWeightedFloatArray(KickForceScaleAList); // MinWeight will be the AIVar final update weight.

        float ScaleAMin = AKGUtil.GetMeanFromWeightedFloatArray(KickForceScaleAList);
        float ScaleAStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleAList, ScaleAMin);

        float ScaleBMin = AKGUtil.GetMeanFromWeightedFloatArray(KickForceScaleBList);
        float ScaleBStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleBList, ScaleBMin);

        float DirDevMin = AKGUtil.GetMeanFromWeightedFloatArray(KickDirDeviationList);
        //float DirDevStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickDirDeviationList, DirDevMin);

        // @TODO We might apply some weight on update AIVarSet.
        AIVarSetMain.UpdateKickForceScaleA(ScaleAMin, ScaleAStdDev, MinWeight);
        AIVarSetMain.UpdateKickForceScaleB(ScaleBMin, ScaleBStdDev, MinWeight);
        AIVarSetMain.UpdateKickDirDeviation(DirDevMin, MinWeight); // Min value is the deviation here.

        SuccessfulTrials.clear(); // Then remove the history.
    }
}
