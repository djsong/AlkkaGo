package com.djsong.alkkago;

import java.util.ArrayList;

/**
 * Created by DJSong on 2016-03-19.
 * Static utilities
 */
public class AKGUtil {

    public static final float KINDA_SMALL_NUMBER = 0.0001f;

    /** It can be used to check if StoneA and StoneB are actually the same one in physically simulated environment. */
    public static boolean AreTwoStonesEntirelyOverlap(AKGStone StoneA, AKGStone StoneB){
        AKGVector2D PosDelta = new AKGVector2D(StoneA.WorldCoordX() - StoneB.WorldCoordX(), StoneA.WorldCoordY() - StoneB.WorldCoordY());
        return (PosDelta.GetLength() < KINDA_SMALL_NUMBER);
    }
    public static float DistBetweenTwoStones(AKGStone StoneA, AKGStone StoneB){
        AKGVector2D DistVector = new AKGVector2D(StoneA.WorldCoordX() - StoneB.WorldCoordX(), StoneA.WorldCoordY() - StoneB.WorldCoordY());
        return DistVector.GetLength();
    }

    /** Infinite ray from certain origin to circle intersection check */
    public static boolean RayCircleIntersect(AKGVector2D RayOrigin, AKGVector2D RayDirection, AKGVector2D CircleCenter, float CircleRadius){

        // I guess there's a simpler solution using some quadratic or cubic equation.
        // Just don't really remember how to use it.
        // Works fine anyhow.

        AKGVector2D RayOriginToCircle = new AKGVector2D(CircleCenter.X - RayOrigin.X, CircleCenter.Y - RayOrigin.Y);
        float RayOriginToCircleDistance = RayOriginToCircle.GetLength();
        RayOriginToCircle.NormalizeSelf();
        // Consider ray is infinite, just need direction information.
        AKGVector2D CopiedRayDir = new AKGVector2D(RayDirection);
        CopiedRayDir.NormalizeSelf();

        float DotProduct = RayOriginToCircle.Dot(CopiedRayDir);
        // For some safety..
        DotProduct = Math.min(DotProduct, 1.0f);
        DotProduct = Math.max(DotProduct, -1.0f);

        // Now, this will be the deviation angle between the ray and ray-to-circle
        float DevAngleRadian = (float)Math.acos(DotProduct);
        // Then, we can also get the min distance between the circle and ray
        float RayCircleMinDist = RayOriginToCircleDistance * (float)Math.sin(DevAngleRadian);

        if(RayCircleMinDist <= CircleRadius){

            // @TODO Intersected distance?

            return true;
        }

        return false;
    }

    /** Finite ray to circle intersection check */
    public static boolean FiniteRayCircleIntersect(AKGVector2D RayOrigin, AKGVector2D RayEnd, AKGVector2D CircleCenter, float CircleRadius){

        // I guess there's a simpler solution using some quadratic or cubic equation.
        // Just don't really remember how to use it.
        // Works fine anyhow.

        AKGVector2D RayOriginToCircle = new AKGVector2D(CircleCenter.X - RayOrigin.X, CircleCenter.Y - RayOrigin.Y);
        float RayOriginToCircleDistance = RayOriginToCircle.GetLength();
        RayOriginToCircle.NormalizeSelf();

        AKGVector2D RayDirection = new AKGVector2D(RayEnd.X - RayOrigin.X, RayEnd.Y - RayOrigin.Y);
        float RayLength = RayDirection.GetLength();
        RayDirection.NormalizeSelf();

        float DotProduct = RayOriginToCircle.Dot(RayDirection);
        // For some safety..
        DotProduct = Math.min(DotProduct, 1.0f);
        DotProduct = Math.max(DotProduct, -1.0f);

        // Now, this will be the deviation angle between the ray and ray-to-circle
        float DevAngleRadian = (float)Math.acos(DotProduct);
        // Then, we can also get the min distance between the circle and ray
        float RayCircleMinDist = RayOriginToCircleDistance * (float)Math.sin(DevAngleRadian);

        if(RayCircleMinDist <= CircleRadius){

            // @TODO Intersected distance?

            // Until here, it is same to (infinite) RayCircleIntersect, now check whether the ray can reaches the circle

            float ProjectedRayLength = RayLength * DotProduct;
            // This is not correct. However, it will work fine enough in most cases.
            if(ProjectedRayLength > RayOriginToCircleDistance - CircleRadius) {
                return true;
            }
        }

        return false;
    }

    public static float GetMinFromFloatArray(float[] InArray){
        if(InArray.length == 0){
            return 0.0f;
        }
        double AllAdded = 0.0;
        for(int AI = 0; AI < InArray.length; ++AI) {
            AllAdded += InArray[AI];
        }
        return (float)(AllAdded / (double)InArray.length);
    }

    /** Calculate the min first, then send it to get StdDev. It will trust the InMinValue */
    public static float GetStdDevFromFloatArray(float[] InArray, float InMinValue){
        if(InArray.length == 0){
            return 0.0f;
        }
        double AllDevAdded = 0.0;
        for(int AI = 0; AI < InArray.length; ++AI) {
            AllDevAdded += (InArray[AI] - InMinValue) * (InArray[AI] - InMinValue);
        }

        return (float)Math.sqrt(AllDevAdded / (double)InArray.length);
    }

    public static float GetMinFromWeightedFloatArray(ArrayList<AlkkagiAI.WeightedFloat> InArray) {
        if(InArray.size() == 0){
            return 0.0f;
        }

        double AllWeightSum = 0.0;
        for(AlkkagiAI.WeightedFloat CurrElem : InArray){
            AllWeightSum += CurrElem.Weight;
        }

        // Add all values scaled by (OwnWeight / WeightSum)
        double RetMin = 0.0;
        for(AlkkagiAI.WeightedFloat CurrElem : InArray) {
            RetMin += CurrElem.Value * (CurrElem.Weight / AllWeightSum);
        }

        return (float)RetMin;
    }

    /** Like GetStdDevFromFloatArray, It will trust the InMinValue */
    public static float GetStdDevFromWeightedFloatArray(ArrayList<AlkkagiAI.WeightedFloat> InArray, float InMinValue) {
        if(InArray.size() == 0){
            return 0.0f;
        }
        double AllWeightSum = 0.0;
        for(AlkkagiAI.WeightedFloat CurrElem : InArray){
            AllWeightSum += CurrElem.Weight;
        }
        double AddedVar = 0.0;
        // I am not sure about this below..
        for(AlkkagiAI.WeightedFloat CurrElem : InArray) {
            AddedVar += (CurrElem.Value - InMinValue) * (CurrElem.Value - InMinValue) * (CurrElem.Weight / AllWeightSum);
        }
        return (float)Math.sqrt(AddedVar);
    }

    public static float GetMinWeightFromWeightedFloatArray(ArrayList<AlkkagiAI.WeightedFloat> InArray){
        if(InArray.size() == 0){
            return 0.0f;
        }
        double AllWeightSum = 0.0;
        for(AlkkagiAI.WeightedFloat CurrElem : InArray){
            AllWeightSum += CurrElem.Weight;
        }
        return (float)(AllWeightSum / (double)InArray.size());
    }
}
