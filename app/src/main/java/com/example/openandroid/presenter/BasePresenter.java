package com.example.openandroid.presenter;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public abstract class BasePresenter {
    protected Semaphore mCameraOpenCloseLock = new Semaphore(1);

    protected final int LOCK_TIME = 2000;
    protected static final int MAX_PREVIEW_WIDTH = 1920;
    protected static final int MAX_PREVIEW_HEIGHT = 1080;

    protected CameraDevice mCameraDevice;
    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected int mCurrentCameraDirection;

    public void setCurrentCameraDirection(int currentDirection) {
        mCurrentCameraDirection = currentDirection;
    }

    protected int eventX;
    protected int eventY;
    protected int mInintWidth = 120;
    protected int mInintHeight = 160;
    protected int mFocusStatus = 0;
    //处理对焦相关
    private ConcurrentLinkedQueue<String> mFocusQueue = new ConcurrentLinkedQueue<>();
    private static final String AUTOFOCUS = "autoFocus";
    private static final String CANCEL_AUTOFOCUS = "cancelAutoFocus";
    // Last frame for which CONTROL_AF_STATE was received.
    private long mLastControlAfStateFrameNumber = -1;
    private int mLastResultAFState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    private final Object mLock = new Object();


    protected final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            Log.d("wangchao", "cameraDevice　onOpened-------------------");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d("wangchao", "cameraDevice　onDisconnected-------------------");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.d("wangchao", "cameraDevice　onError-------------------");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    protected abstract void startPreview();

    protected final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //处理拍照后状态
            Log.d("wangchao", "ImageReader reader-------------------");
        }
    };
    protected static final int STATE_PREVIEW = 0;
    protected static final int STATE_WAITING_LOCK = 1;
    protected static final int STATE_WAITING_PRECAPTURE = 2;
    protected static final int STATE_WAITING_NON_PRECAPTURE = 3;
    protected static final int STATE_PICTURE_TAKEN = 4;
    protected static final int STATE_AUTO_FOCUS = 5;
    protected int mState = STATE_PREVIEW;
    protected CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

            switch (mState) {
                case STATE_PREVIEW: {
                    Log.d("wangchao_log", "STATE_PREVIEW-------------------");
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    Log.d("wangchao_log", "STATE_WAITING_PRECAPTURE-------------------");
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    Log.d("wangchao_log", "STATE_WAITING_NON_PRECAPTURE-------------------");
                    break;
                }
                case STATE_AUTO_FOCUS:
                    Log.d("wangchao_focus", "STATE_AUTO_FOCUS-------------------");
                    autoFocusStateChangeDispatcher(result);
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * 通知对焦更新
     *
     * @param result
     */
    private void autoFocusStateChangeDispatcher(CaptureResult result) {
        long currentFrameNumber = result.getFrameNumber();
        if (currentFrameNumber < mLastControlAfStateFrameNumber ||
                result.get(CaptureResult.CONTROL_AF_STATE) == null) {
            Log.w("wangchao_log", "[autofocusStateChangeDispatcher] frame number, last:current " +
                    mLastControlAfStateFrameNumber +
                    ":" + currentFrameNumber + " afState:" +
                    result.get(CaptureResult.CONTROL_AF_STATE));
            return;
        }
        mLastControlAfStateFrameNumber = result.getFrameNumber();
        int resultAFState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (mLastResultAFState != resultAFState) {
            notifyFocusStateChanged(resultAFState);
            Log.w("wangchao_log", "[autofocusStateChangeDispatcher] mLastResultAFState " +
                    mLastResultAFState + ",resultAFState " + resultAFState);
        }
        mLastResultAFState = resultAFState;
    }

    private void notifyFocusStateChanged(int afState) {
        if (afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ||
                afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
            synchronized (mFocusQueue) {
                if (!mFocusQueue.isEmpty() && AUTOFOCUS.equals(mFocusQueue.peek())) {
                    mFocusQueue.clear();
                    mFocusQueue.add(CANCEL_AUTOFOCUS);
                }
            }
        }
        synchronized (mLock) {
            showFocusUi(eventX,eventY);
        }
    }

    protected abstract void showFocusUi(int eventX, int eventY);


    protected abstract void runPrecaptureSequence();

    protected abstract void captureStillPicture();

    protected final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    protected abstract void configureTransform(int width, int height);

    protected abstract void openCamera(int width, int height);

}
