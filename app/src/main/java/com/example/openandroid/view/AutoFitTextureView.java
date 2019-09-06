/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.openandroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {

    private GestureDetector mGestureDector;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private OnGestureListener mOuterGestureLsn;
    private ScaleGestureDetector mScaleGestureDector;

    public AutoFitTextureView(Context context) {
        this(context, null);
        initEvent(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initEvent(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initEvent(context);
    }

    private void initEvent(Context context) {
        mGestureDector = new GestureDetector(context, mGestureLsn);
        mScaleGestureDector = new ScaleGestureDetector(context, mScaleGestureLsn);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDector.onTouchEvent(event) || mScaleGestureDector.onTouchEvent(event)) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (null != mOuterGestureLsn) {
                    mOuterGestureLsn.onActionUp();
                }
            }
            return true;
        }
        if (event.getPointerCount() > 1) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setOnGestureListener(OnGestureListener listener) {
        mOuterGestureLsn = listener;
    }

    GestureDetector.OnGestureListener mGestureLsn = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            if (null != mOuterGestureLsn) {
                mOuterGestureLsn.showPress();
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (null != mOuterGestureLsn) {
                mOuterGestureLsn.onSingleTap(e);
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (null != mOuterGestureLsn) {
                mOuterGestureLsn.onLongPress();
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    };

    ScaleGestureDetector.OnScaleGestureListener mScaleGestureLsn = new ScaleGestureDetector.OnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (null != mOuterGestureLsn) {
                mOuterGestureLsn.onScale(detector.getScaleFactor());
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    };

    public interface OnGestureListener {
        boolean onSingleTap(MotionEvent e);
        void onScale(float factor);
        void showPress();
        void onLongPress();
        void onActionUp();
    }
}
