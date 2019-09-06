package com.example.openandroid.view.focus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
public class RotateLayout extends ViewGroup implements Rotatable {
    private static final String TAG = "RotateLayout";
    
    private OnSizeChangedListener mListener;
    private int mOrientation;
    protected View mChild;
    
    public RotateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(android.R.color.transparent);
    }
    
    public interface OnSizeChangedListener {
        void onSizeChanged(int width, int height);
    }
    
    @SuppressLint("NewApi")
	@Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChild = getChildAt(0);
        mChild.setPivotX(0);
        mChild.setPivotY(0);
    }
    
    @Override
    protected void onLayout(boolean change, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        switch (mOrientation) {
        case 0:
        case 180:
            mChild.layout(0, 0, width, height);
            break;
        case 90:
        case 270:
            mChild.layout(0, 0, height, width);
            break;
        default:
            break;
        }
    }
    
    @SuppressLint("NewApi")
	@Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = 0;
        int h = 0;
        switch (mOrientation) {
        case 0:
        case 180:
            measureChild(mChild, widthSpec, heightSpec);
            w = mChild.getMeasuredWidth();
            h = mChild.getMeasuredHeight();
            break;
        case 90:
        case 270:
            measureChild(mChild, heightSpec, widthSpec);
            w = mChild.getMeasuredHeight();
            h = mChild.getMeasuredWidth();
            break;
        default:
            break;
        }
        setMeasuredDimension(w, h);
        
        switch (mOrientation) {
        case 0:
            mChild.setTranslationX(0);
            mChild.setTranslationY(0);
            break;
        case 90:
            mChild.setTranslationX(0);
            mChild.setTranslationY(h);
            break;
        case 180:
            mChild.setTranslationX(w);
            mChild.setTranslationY(h);
            break;
        case 270:
            mChild.setTranslationX(w);
            mChild.setTranslationY(0);
            break;
        default:
            break;
        }
        mChild.setRotation(-mOrientation);
    }
    
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }
    
    public void setOrientation(int orientation, boolean animation) {
        Log.v(TAG, "setOrientation(" + orientation + ", " + animation + ") mOrientation="
                + mOrientation);
        orientation = orientation % 360;
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        requestLayout();
    }
    
    public int getOrientation() {
        return mOrientation;
    }
    
    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mListener = listener;
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "onSizeChanged(" + w + ", " + h + ", " + oldh + ", " + oldh + ") " + this);
        if (mListener != null) {
            mListener.onSizeChanged(w, h);
        }
    }
}
