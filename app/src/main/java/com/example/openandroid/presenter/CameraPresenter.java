package com.example.openandroid.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import com.example.openandroid.CameraTest;
import com.example.openandroid.ICameraImp;
import com.example.openandroid.permission.PermissionsManager;
import com.example.openandroid.util.Camera2Utils;
import com.example.openandroid.util.CompareSizesByArea;
import com.example.openandroid.util.WorkThreadManager;
import com.example.openandroid.view.AutoFitTextureView;
import com.example.openandroid.view.focus.FocusViewController;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
public class CameraPresenter extends BasePresenter implements ICameraPresenter {
    private CameraManager manager;
    private WorkThreadManager mWorkThreadManager;
    private ICameraImp mICameraImp;
    private CameraTest mCameraActivity;
    private ICameraImp.ICameraView mICameraView;
    private String mCameraId;
    private CameraCharacteristics characteristics;
    private boolean mAutoFocusSupported;
    private ImageReader mImageReader;
    private int mSensorOrientation;
    private int mDisplayRotate;
    private Size mPreviewSize;
    private Rect mActiveArraySize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;

    public CameraPresenter(ICameraImp.ICameraView cameraView,ICameraImp iCameraImp) {
        mICameraView = cameraView;
        mICameraImp = iCameraImp;
        mWorkThreadManager = mICameraImp.getWorkThreadManager();
        mCameraActivity = (CameraTest) mICameraImp.getActivity();
        manager = (CameraManager) mCameraActivity.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onResume() {
        if (mWorkThreadManager != null){
            mWorkThreadManager.startWorkThread();
        }
        TextureView cameraView = mICameraView.getCameraView();
        Log.d("wangchao","cameraView-------->"+cameraView);
        if (cameraView.isAvailable()){
            openCamera(mCameraActivity,cameraView.getWidth(),cameraView.getHeight());
        }else{
            cameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        if (mWorkThreadManager != null) {
            closeCamera();
            mWorkThreadManager.stopBackgroundThread();
        }
    }

    @Override
    public void openCamera(Activity mCameraActivity,int width, int height) {
        if (PermissionsManager.checkCameraPermission(mCameraActivity)){
            setUpCameraOutputs(mCameraActivity,width, height);
            configureTransform(mCameraActivity,width, height);
            try {
                if (!mCameraOpenCloseLock.tryAcquire(LOCK_TIME, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                Log.d("wangchao","open_camera-------->");
                if (manager != null){//默认打开后摄
                    manager.openCamera(mCameraId, mStateCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }
    }

    private void setUpCameraOutputs(Activity activity,int width,int height){
        try {
            if (manager == null)return;
                String[] cameraIdList = manager.getCameraIdList();
                mCameraId = cameraIdList[0];//默认打开后摄
                Log.d("wangchao","setUpCameraOutputs getCameraIdList-----"+mCameraId);
                characteristics  = manager.getCameraCharacteristics(mCameraId);
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Log.d("wangchao","StreamConfigurationMap map-----"+map);
                if (map == null)return;
                //检查设备,是否支持自动对焦
                mAutoFocusSupported = Camera2Utils.checkAutoFocus(characteristics);
                Log.d("wangchao","StreamConfigurationMap mAutoFocusSupported-----"+mAutoFocusSupported);

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mICameraImp.getWorkThreadManager().getBackgroundHandler());

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mDisplayRotate = (mSensorOrientation - displayRotation + 360) % 360;
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e("wangchao", "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                mPreviewSize = Camera2Utils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest, new CompareSizesByArea());
                Log.d("wangchao","configureTransform viewWidth="+mPreviewSize.getHeight()+"   viewWidth="+mPreviewSize.getWidth());
                int orientation = activity.getResources().getConfiguration().orientation;
                AutoFitTextureView mTextureView = (AutoFitTextureView) mICameraView.getCameraView();
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio( mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio( mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                mActiveArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        } catch (CameraAccessException e) {
            Log.d("wangchao","setUpCameraOutputs CameraAccessException-----"+e);
            e.printStackTrace();
        }catch (NullPointerException e) {
            Log.d("wangchao","setUpCameraOutputs CameraAccessException----设备不支持Camera2 API");
        }
    }

    private void configureTransform(Activity activity, int viewWidth, int viewHeight) {
        Log.d("wangchao","configureTransform mPreviewSize-----"+mPreviewSize);
        Log.d("wangchao","configureTransform mICameraView.getCameraView()-----"+mICameraView.getCameraView());
        if (null == mICameraView.getCameraView() || null == mPreviewSize || null ==  activity) {
            return;
        }
        int rotation =  activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mICameraView.getCameraView().setTransform(matrix);
    }

    @Override
    public void takePicture() {
        if (mAutoFocusSupported) {
            Log.i("wangchao","camera 支持自动调焦，正在锁住焦点");
            lockFocus();
        } else {//设备不支持自动对焦，则直接拍照。
            Log.i("wangchao","camera 不支持自动调焦，直接拍照");
            captureStillPicture();
        }
    }

    @Override
    public void showFocusView(int eventX, int eventY) {
        FrameLayout frameLayout = mICameraView.getFrameLayout();
        Log.d("addFocusView","mFocusViewController frameLayout-----------------"+frameLayout);
        if (frameLayout == null)return;
        final FocusViewController mFocusViewController = mICameraImp.getFocusViewController();
        mFocusViewController.setRootView(frameLayout);
        mFocusViewController.addFocusView();
        //
        if (mFocusStatus == 0){
            mFocusViewController.showPassiveFocusAtCenter();
        }else {
            mFocusViewController.showActiveFocusAt(eventX,eventY);
        }
        Log.d("addFocusView","mFocusViewController showActiveFocusAt-x----------------"+eventX);
        Log.d("addFocusView","mFocusViewController showActiveFocusAt-y----------------"+eventY);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("addFocusView","mFocusViewController postDelayed clearFocusUi-----------------");
                //mFocusViewController.stopFocusAnimations();
                mFocusViewController.clearFocusUi();
            }
        },1800);
    }


    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closi" + "ng.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void lockFocus() {
        try {
            //锁住焦点
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // 标识，正在进行拍照动作
            mState = STATE_WAITING_LOCK;
            //进行拍照处理
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startPreview() {
        createCameraPreviewSession();
    }

    @Override
    protected void showFocusUi(int eventX ,int eventY) {
        FrameLayout frameLayout = mICameraView.getFrameLayout();
        Log.d("wangchao","showFocusUi------->"+eventX+" evenY="+eventY);
        Log.d("wangchao","frameLayout------->"+frameLayout);
        if (frameLayout != null){
            showFocusView(eventX,eventY);
        }
    }

    @Override
    protected void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set. 设置成预捕获状态，将需等待。
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,  mICameraImp.getWorkThreadManager().getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void captureStillPicture() {
        try {
            if (null == mCameraActivity || null == mCameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            //拍照自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Orientation
            int rotation = mCameraActivity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, Camera2Utils.getOrientation(ORIENTATIONS, mSensorOrientation, rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback  = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    Log.d("wangchao","onCaptureCompleted------->"+request);
                    unlockFocus(); //拍照完成，释放焦点
                }
            };
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void configureTransform(int width, int height) {
        configureTransform(mCameraActivity,width,height);
    }

    @Override
    protected void openCamera(int width, int height) {
        openCamera(mCameraActivity,width,height);
    }

    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        SurfaceTexture surfaceTexture = mICameraView.getCameraView().getSurfaceTexture();
        if (surfaceTexture == null)return;
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            Log.d("wangchao"," onConfigured--------------------------->mCameraDevice="+mCameraDevice);
                            if (null == mCameraDevice) {// The camera is already closed
                                return;
                            }
                            mCaptureSession = cameraCaptureSession;
                            setCameraCaptureSession();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.d("wangchao"," onConfigureFailed--------------------------->");
                        }
                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setCameraCaptureSession() {
        try {
            //设置连续自动对焦
            if (mPreviewRequestBuilder != null){
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewRequest = mPreviewRequestBuilder.build();
            }
            mState = STATE_AUTO_FOCUS;//进行对焦
            Log.d("wangchao_log","camera_capture_lock focus----------->");
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void focusOnTouch(MotionEvent event, int viewWidth, int viewHeight) {
        double x = event.getX();
        double y = event.getY();
        double tmp;
        int realPreviewWidth = mPreviewSize.getWidth(), realPreviewHeight = mPreviewSize.getHeight();
        if (90 == mDisplayRotate || 270 == mDisplayRotate) {
            realPreviewWidth = mPreviewSize.getHeight();
            realPreviewHeight = mPreviewSize.getWidth();
        }
        double imgScale = 1.0f, verticalOffset = 0, horizontalOffset = 0;
        if (realPreviewHeight * viewWidth > realPreviewWidth * viewHeight) {
            imgScale = viewWidth * 1.0 / realPreviewWidth;
            verticalOffset = (realPreviewHeight - viewHeight / imgScale) / 2;
        } else {
            imgScale = viewHeight * 1.0 / realPreviewHeight;
            horizontalOffset = (realPreviewWidth - viewWidth / imgScale) / 2;
        }
        x = x / imgScale + horizontalOffset;
        y = y / imgScale + verticalOffset;
        if (90 == mDisplayRotate) {
            tmp = x;
            x = y;
            y = mPreviewSize.getHeight() - tmp;
        } else if (270 == mDisplayRotate) {
            tmp = x;
            x = mPreviewSize.getWidth() - y;
            y = tmp;
        }
        Rect cropRegion = mPreviewRequest.get(CaptureRequest.SCALER_CROP_REGION);
        if (null == cropRegion) {
            Log.e("wangchao", "can't get crop region");
            cropRegion = mActiveArraySize;
        }
        int cropWidth = cropRegion.width(), cropHeight = cropRegion.height();
        if (mPreviewSize.getHeight()* cropWidth > mPreviewSize.getWidth() * cropHeight) {
            imgScale = cropHeight * 1.0 / mPreviewSize.getHeight();
            verticalOffset = 0;
            horizontalOffset = (cropWidth - imgScale * mPreviewSize.getWidth()) / 2;
        } else {
            imgScale = cropWidth * 1.0 / mPreviewSize.getWidth();
            horizontalOffset = 0;
            verticalOffset = (cropHeight - imgScale * mPreviewSize.getHeight()) / 2;
        }
        x = x * imgScale + horizontalOffset + cropRegion.left;
        y = y * imgScale + verticalOffset + cropRegion.top;
        double tapAreaRatio = 0.1;
        Rect rect = new Rect();
        rect.left = clamp((int) (x - tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.right = clamp((int) (x + tapAreaRatio / 2 * cropRegion.width()), 0, cropRegion.width());
        rect.top = clamp((int) (y - tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());
        rect.bottom = clamp((int) (y + tapAreaRatio / 2 * cropRegion.height()), 0, cropRegion.height());
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[] {new MeteringRectangle(rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[] {new MeteringRectangle(rect, 1000)});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();
        try {
            mState = STATE_AUTO_FOCUS;
            eventX = (int) event.getX();
            eventY = (int) event.getY();
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mICameraImp.getWorkThreadManager().getBackgroundHandler());
        } catch (CameraAccessException e) {
            Log.e("wangchao",  "setRepeatingRequest failed, " + e.getMessage());
        }
    }

    private int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

}
