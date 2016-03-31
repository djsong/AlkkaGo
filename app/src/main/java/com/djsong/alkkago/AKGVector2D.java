package com.djsong.alkkago;

/**
 * Created by DJSong on 2016-03-19.
 */
public class AKGVector2D {

    public float X = 0.0f;
    public float Y = 0.0f;

    public AKGVector2D()
    {
        X = 0.0f;
        Y = 0.0f;
    }
    public AKGVector2D(float InX, float InY){
        X = InX;
        Y = InY;
    }
    public AKGVector2D(AKGVector2D other){
        X = other.X;
        Y = other.Y;
    }

    public void add(AKGVector2D other){
        X += other.X;
        Y += other.Y;
    }
    public void minus(AKGVector2D other){
        X -= other.X;
        Y -= other.Y;
    }
    public void multiply(AKGVector2D other){
        X *= other.X;
        Y *= other.Y;
    }
    public void multiply(float f){
        X *= f;
        Y *= f;
    }
    public void divide(AKGVector2D other){
        X /= other.X;
        Y /= other.Y;
    }
    public void divide(float f){
        X /= f;
        Y /= f;
    }



    public float GetLength(){
        return (float)Math.sqrt(X * X + Y * Y);
    }

    public void NormalizeSelf(){
        float length = GetLength();
        if(length < AKGUtil.KINDA_SMALL_NUMBER)
        {
            return;
        }
        X /= length;
        Y /= length;
    }

    public float Dot(AKGVector2D other){
        return (X * other.X + Y * other.Y);
    }

    public void RotateSelf(float RotateRadian)
    {
        X = X * (float)Math.cos(RotateRadian) - Y * (float)Math.sin(RotateRadian);
        Y = X * (float)Math.sin(RotateRadian) + Y * (float)Math.cos(RotateRadian);
    }

    public void ClampSize(float MaxSize){
        float CurrLength = GetLength();
        if(CurrLength > MaxSize){
            NormalizeSelf();
            multiply(MaxSize);
        }
    }
}
