package com.example.openandroid;

import android.app.Activity;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.example.openandroid.presenter.CameraPresenter;
import com.example.openandroid.util.WorkThreadManager;
import com.example.openandroid.view.focus.FocusViewController;

public interface ICameraImp {
    Activity getActivity();
    WorkThreadManager getWorkThreadManager();
    CameraPresenter getCameraPresenter();
    FocusViewController getFocusViewController();
    interface ICameraView{
        TextureView getCameraView();
        FrameLayout getFrameLayout();
    }
}
