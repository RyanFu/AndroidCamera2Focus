package com.example.openandroid.camera;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import com.example.openandroid.ICameraImp;
import com.example.openandroid.R;
import com.example.openandroid.presenter.CameraPresenter;
import com.example.openandroid.view.AutoFitTextureView;

public class CameraFragment extends Fragment implements ICameraImp.ICameraView,View.OnClickListener,AutoFitTextureView.OnGestureListener{
    public static final String TAG = CameraFragment.class.getSimpleName();
    private View mRootView;
    private AutoFitTextureView mCameraView;
    private ICameraImp mICameraImp;
    private CameraPresenter mCameraPresenter;
    private FrameLayout mMainCameraLayout;
    private Button btnTakePicture;
    private Button btnSuccess;
    private Button btnFail;

    public void setICameraImp(ICameraImp cameraImp){
        this.mICameraImp = cameraImp;
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraPresenter = mICameraImp.getCameraPresenter();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == mRootView){
            mRootView = inflater.inflate(R.layout.fragment_camera, container, false);
        }else{
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null){
                parent.removeView(mRootView);
            }
        }
        initView(mRootView);
        return mRootView;
    }

    private void initView(View mRootView) {
        mCameraView = mRootView.findViewById(R.id.camera_view);
        mCameraView.setOnGestureListener(this);
        mMainCameraLayout = mRootView.findViewById(R.id.main_camera_layout);
        btnTakePicture = mRootView.findViewById(R.id.btn_take_picture);
        btnSuccess = mRootView.findViewById(R.id.btn_test_success);
        btnFail = mRootView.findViewById(R.id.btn_test_fail);
        btnTakePicture.setOnClickListener(this);
        btnSuccess.setOnClickListener(this);
        btnFail.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCameraPresenter != null){
            mCameraPresenter.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraPresenter != null){
            mCameraPresenter.onPause();
        }
    }

    @Override
    public TextureView getCameraView() {
        return mCameraView;
    }

    @Override
    public FrameLayout getFrameLayout() {
        return mMainCameraLayout;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_take_picture:
                if (mCameraPresenter != null){
                    mCameraPresenter.takePicture();
                }
                break;
            case R.id.btn_test_success:
                if (mCameraPresenter != null){
                    mCameraPresenter.onPause();
                }
                break;
            case R.id.btn_test_fail:
                if (mCameraPresenter != null){
                    mCameraPresenter.onPause();
                }
                if (mCameraPresenter != null){
                    mCameraPresenter.onPause();
                }
                break;
        }
    }

    @Override
    public boolean onSingleTap(MotionEvent e) {
        if (null == mCameraView) {
            return false;
        }
        if (mCameraPresenter != null){
            mCameraPresenter.focusOnTouch(e, mCameraView.getWidth(), mCameraView.getHeight());
        }
        return false;
    }

    @Override
    public void onScale(float factor) {

    }

    @Override
    public void showPress() {
    }

    @Override
    public void onLongPress() {

    }

    @Override
    public void onActionUp() {
    }
}
