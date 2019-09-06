package com.example.openandroid;


import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.openandroid.camera.CameraFragment;
import com.example.openandroid.presenter.CameraPresenter;
import com.example.openandroid.util.WorkThreadManager;
import com.example.openandroid.view.focus.FocusViewController;

public class CameraTest extends AppCompatActivity implements ICameraImp {
    private CameraFragment cameraFragment;
    private WorkThreadManager mWorkThreadManager;
    private CameraPresenter mCameraPresenter;
    private FocusViewController mFocusViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera2);
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (cameraFragment == null) {
            cameraFragment = CameraFragment.newInstance();
            cameraFragment.setICameraImp(this);
            getSupportFragmentManager().beginTransaction().add(R.id.main_content_layout, cameraFragment, CameraFragment.TAG).commitAllowingStateLoss();
        }
        initSourceData();
    }

    private void initSourceData() {
        mWorkThreadManager = WorkThreadManager.newInstance();
        mCameraPresenter = new CameraPresenter(cameraFragment,this);
        mFocusViewController = new FocusViewController(this);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public WorkThreadManager getWorkThreadManager() {
        return mWorkThreadManager;
    }

    @Override
    public CameraPresenter getCameraPresenter() {
        return mCameraPresenter;
    }

    @Override
    public FocusViewController getFocusViewController() {
        return mFocusViewController;
    }

}
