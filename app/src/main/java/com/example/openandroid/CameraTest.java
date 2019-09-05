package com.example.openandroid;


import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CameraTest extends AppCompatActivity implements ICameraImp {
    private Camera2Fragment camera2Fragment;
    private WorkThreadManager mWorkThreadManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initData();
        camera2Fragment = (Camera2Fragment) getSupportFragmentManager().findFragmentByTag(Camera2Fragment.TAG);
        if (camera2Fragment == null) {
            camera2Fragment = Camera2Fragment.newInstance();
            camera2Fragment.setCameraImp(this);
            getSupportFragmentManager().beginTransaction().add(R.id.container, camera2Fragment, Camera2Fragment.TAG).commitAllowingStateLoss();
        }

    }
    private void initData() {
        mWorkThreadManager = WorkThreadManager.newInstance();
    }

    @Override
    public WorkThreadManager getWorkThreadManager() {
        return mWorkThreadManager;
    }
}
