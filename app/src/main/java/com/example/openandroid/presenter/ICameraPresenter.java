package com.example.openandroid.presenter;

import android.app.Activity;
import android.view.MotionEvent;

public interface ICameraPresenter {
    void onResume();

    void onPause();

    void openCamera(Activity activity, int width, int height);

    void takePicture();

    void showFocusView(int eventX,int eventY);

    void focusOnTouch(MotionEvent event,int width,int height);

}
