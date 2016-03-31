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

        KickTargetPickInfo PickedKickAndTarget = TryPickBestKickAndTargetStone(ControllingStones, OtherStones, AllStones);
        if(PickedKickAndTarget.KickStone == null || PickedKickAndTarget.TargetStone == null){
            return null; // Not a normal expected case. Probably found no live stones..
        }

        // Create new one for this turn's AI var recording. Almost for mbDeepShittingMode but just do this for all time.
        CachedLastTrialRecord = new AKGAITrialRecordInfo();

        AKGVector2D KickDirection = ComputeKickDirection(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, CachedLastTrialRecord);
        float KickForceSize = ComputeKickForceSize(PickedKickAndTarget.KickStone, PickedKickAndTarget.TargetStone, KickDirection, CachedLastTrialRecord);
        //KickForceSize = Math.min(mKickForceLimit, KickForceSize);


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


    /** Just a transient struct to make a list.. */
    public class KickTargetPickInfo{
        public AKGStone KickStone = null;
        public AKGStone TargetStone = null;
    }

    private KickTargetPickInfo TryPickBestKickAndTargetStone(ArrayList<AKGStone> ControllingStones, ArrayList<AKGStone> OtherStones, ArrayList<AKGStone> AllStones){
        ArrayList<KickTargetPickInfo> PossiblePickInfoList = new ArrayList<KickTargetPickInfo>();

        for(int CI = 0; CI < ControllingStones.size(); ++CI){

            AKGStone CurrKickCandid = ControllingStones.get(CI);

            if(CurrKickCandid.IsAlive()) { // Better not forget live check..
                AKGStone PossibleTarget = TryPickBestVisibleTargetOfSingleKickStone(CurrKickCandid, OtherStones, AllStones);

                if(PossibleTarget != null && PossibleTarget.IsAlive()){ // Double check for IsAlive
                    // We got one possible kick and target combination.
                    KickTargetPickInfo NewPossibleInfo = new KickTargetPickInfo();
                    NewPossibleInfo.KickStone = CurrKickCandid;
                    NewPossibleInfo.TargetStone = PossibleTarget;
                    PossiblePickInfoList.add(NewPossibleInfo);
                }
            }
        }

        if(PossiblePickInfoList.size() == 0){
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
                        PossiblePickInfoList.add(NewPossibleInfo);
                    }
                }
            }
        }

        // If PossiblePickInfoList is still 0 yet, the game must be over..

        KickTargetPickInfo RetInfo = new KickTargetPickInfo();
        // Now, just pick up the closest combination among PossiblePickInfoList
        float ClosestDist = 1000000000000.0f;
        for(int PPI = 0; PPI < PossiblePickInfoList.size(); ++PPI) {
            KickTargetPickInfo CurrInfo = PossiblePickInfoList.get(PPI);
            float CurrDist = AKGUtil.DistBetweenTwoStones(CurrInfo.KickStone, CurrInfo.TargetStone);
            if(CurrDist < ClosestDist){
                RetInfo = CurrInfo;
                ClosestDist = CurrDist;
            }
        }

        return RetInfo;
    }

    /** Pick visible one (not blocked by any other stones) from OtherStones list, to be the best target of KickStone.
     * It will return null if there's nothing visible and alive */
    private AKGStone TryPickBestVisibleTargetOfSingleKickStone(AKGStone KickStone, ArrayList<AKGStone> OtherStones, ArrayList<AKGStone> AllStones){

        ArrayList<AKGStone> VisibleList = new ArrayList<AKGStone>();

        for (int SI = 0; SI < OtherStones.size(); ++SI) {
            AKGStone CurrCandidate = OtherStones.get(SI);

            if(!CurrCandidate.IsAlive()){ // Don't care about dead one.
                continue;
            }

            AKGVector2D RayCheckOrigin = new AKGVector2D(KickStone.WorldCoord());
            AKGVector2D RayCheckDir = new AKGVector2D(CurrCandidate.WorldCoordX() - KickStone.WorldCoordX(), CurrCandidate.WorldCoordY() - KickStone.WorldCoordY());
            RayCheckDir.NormalizeSelf();

            // Check visibility
            boolean bBlockedBySomething = false;
            for(int ASI = 0; ASI < AllStones.size(); ++ASI){ // We may check for my (controlling) stones list.. It should return the same result..?
                AKGStone CheckStone = AllStones.get(ASI);

                // Skip for our kick stone and target candidate.
                if( AKGUtil.AreTwoStonesEntirelyOverlap(KickStone, CheckStone) || AKGUtil.AreTwoStonesEntirelyOverlap(CurrCandidate, CheckStone) ){
                    continue;
                }

                if(AKGUtil.RayCircleIntersect(RayCheckOrigin, RayCheckDir, CheckStone.WorldCoord(), CheckStone.Radius())){
                    bBlockedBySomething = true; // CheckStone blocks the way between KickStone and current target candidate.
                    break;
                }
            }

            if(!bBlockedBySomething) {
                // Now we can put this in visible list
                VisibleList.add(CurrCandidate);
            }
        }

        // It can be still null if found no visible live target.
        return TryPickClosestTargetOfSingleKickStone(KickStone, VisibleList);
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

        float MinWeight = AKGUtil.GetMinWeightFromWeightedFloatArray(KickForceScaleAList); // MinWeight will be the AIVar final update weight.

        float ScaleAMin = AKGUtil.GetMinFromWeightedFloatArray(KickForceScaleAList);
        float ScaleAStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleAList, ScaleAMin);

        float ScaleBMin = AKGUtil.GetMinFromWeightedFloatArray(KickForceScaleBList);
        float ScaleBStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickForceScaleBList, ScaleBMin);

        float DirDevMin = AKGUtil.GetMinFromWeightedFloatArray(KickDirDeviationList);
        //float DirDevStdDev = AKGUtil.GetStdDevFromWeightedFloatArray(KickDirDeviationList, DirDevMin);

        // @TODO We might apply some weight on update AIVarSet.
        AIVarSetMain.UpdateKickForceScaleA(ScaleAMin, ScaleAStdDev, MinWeight);
        AIVarSetMain.UpdateKickForceScaleB(ScaleBMin, ScaleBStdDev, MinWeight);
        AIVarSetMain.UpdateKickDirDeviation(DirDevMin, MinWeight); // Min value is the deviation here.

        SuccessfulTrials.clear(); // Then remove the history.
    }
}
