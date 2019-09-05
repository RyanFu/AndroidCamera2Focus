package com.example.openandroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class Camera2Utils {

    private static final String TAG = Camera2Utils.class.getSimpleName();
    /**
     * 拍照模式
     */
    public static final  int  MODE_CAMERA = 1;
    /**
     * 录像模式
     */
    public static final  int MODE_VIDEO_RECORD =2;
    public static final String MIMETYPE_EXTENSION_NULL = "unknown_ext_null_mimeType";
    public static final String MIMETYPE_EXTENSION_UNKONW = "unknown_ext_mimeType";
    /**
     * 计算合适的大小Size,在相机拍照
     *
     * @param choices
     * @param textureViewWidth
     * @param textureViewHeight
     * @param maxWidth
     * @param maxHeight
     * @param aspectRatio
     * @return
     */
    public static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight,
                                         int maxWidth, int maxHeight, Size aspectRatio,CompareSizesByArea compareSizesByArea) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, compareSizesByArea);
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough,compareSizesByArea);
        } else {
            Log.e(" 计算结果", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation 屏幕的方向
     * @return JPEG的方向(例如：0,90,270,360)
     */
    public static int getOrientation(SparseIntArray ORIENTATIONS, int mSensorOrientation, int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }
    /**
     * 检查是否支持设备自动对焦
     * <p>
     * @param characteristics
     * @return
     */
    public static boolean checkAutoFocus(CameraCharacteristics characteristics) {
        int[] afAvailableModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (afAvailableModes.length == 0 || (afAvailableModes.length == 1 && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)) {
            return  false;
        } else {
            return  true;
        }
    }
    /**
     * 匹配指定方向的摄像头，前还是后
     *
     * LENS_FACING_FRONT是前摄像头标志
     * @param cameraCharacteristics
     * @param direction
     * @return
     */
    public static  boolean matchCameraDirection(CameraCharacteristics cameraCharacteristics,int direction){
        //设置后摄像头
        Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        return  (facing != null && facing == direction)?true:false;
    }

    public  static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }
    /**
     * 计算合适的大小，在视频录制
     * @param choices
     * @param width
     * @param height
     * @param aspectRatio
     * @return
     */
    public  static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * 计算zoom所对应的rect
     * @param originReact 相机原始的rect
     * @param currentZoom 当前的zoom值
     * @return
     */
    public static Rect createZoomRect(Rect originReact, float currentZoom){
        Rect zoomRect=null;
        try {
            if (originReact==null){
                return zoomRect;
            }else{
                float ratio=(float)1/currentZoom;
                int cropWidth=originReact.width()-Math.round((float)originReact.width() * ratio);
                int cropHeight=originReact.height() - Math.round((float)originReact.height() * ratio);
                zoomRect = new Rect(cropWidth/2, cropHeight/2, originReact.width() - cropWidth/2, originReact.height() - cropHeight/2);
            }
        }catch (Exception e){
            e.printStackTrace();
            zoomRect=null;
        }
        return zoomRect;
    }
    /**
     * 获取最大的数字变焦值，也就是缩放值
     * @param cameraCharacteristics
     * @return
     */
    public static  Float getMaxZoom(CameraCharacteristics cameraCharacteristics){
        Float maxZoom=null;
        try {
            maxZoom= cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            Log.d("getMaxZoom","getMaxZoom maxZoom==="+maxZoom);
        }catch (Exception e){
            e.printStackTrace();
        }
        return maxZoom;
    }


    /**
     * 通知图库更新图片
     * @param context
     * @param filePath
     */
    public static  void sendBroadcastNotify(Context context, String filePath){
        //扫描指定文件
        String action=Intent.ACTION_MEDIA_SCANNER_SCAN_FILE;
        //生成问价路径对应的uri
        Uri uri=Uri.fromFile(new File(filePath));
        Intent intent=new Intent(action);
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

    public static void OnIntentGallery(Context context,String path){
        Uri uri = null;
        String mimeType = getSystemMimeType(context,path);
        Log.d("OnIntentGallery","OnIntentGallery--------------------mimeType="+mimeType);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if ("video/mp4".equals(mimeType)){
            uri = Uri.parse(path);
        }else{//image/png
            uri = getItemContentUri(context,path);
        }
        if (uri!=null) {
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
           Log.d(TAG,"ActivityNotFoundException-------exception="+e.getMessage());
        }
    }

    private static String getSystemMimeType(Context context, String path) {
        File mFile = new File(path);
        String fileName = mFile.getName();
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return MIMETYPE_EXTENSION_NULL;
        }
            String mimeType = null;
            final String[] projection = {MediaStore.MediaColumns.MIME_TYPE};
            final String where = MediaStore.MediaColumns.DATA + " = ?";
            Uri baseUri = MediaStore.Files.getContentUri("external");
            String provider = "com.android.providers.media.MediaProvider";
            context.grantUriPermission(provider, baseUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Cursor c = null;
            try {
                c = context.getContentResolver().query(baseUri,
                        projection,
                        where,
                        new String[]{path},
                        null);
                if (c != null && c.moveToNext()) {
                    String type = c.getString(c.getColumnIndexOrThrow(
                            MediaStore.MediaColumns.MIME_TYPE));
                    if (type != null) {
                        mimeType = type;
                    }
                }
            } catch (Exception e) {
            } finally {
                if (c != null) {
                    c.close();
                }
        }
        Log.d(TAG,"mimeType====="+mimeType);
        return mimeType;
    }
    public  static Uri getItemContentUri(Context context,String path) {
        final String[] projection = {MediaStore.MediaColumns._ID};
        final String where = MediaStore.MediaColumns.DATA + " = ?";
        Uri baseUri = MediaStore.Files.getContentUri("external");
        Cursor c = null;
        String provider = "com.android.providers.media.MediaProvider";
        Uri itemUri = null;
        context.grantUriPermission(provider, baseUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            c = context.getContentResolver().query(baseUri,
                    projection,
                    where,
                    new String[]{path},
                    null);
            if (c != null && c.moveToNext()) {
                int type = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                if (type != 0) {
                    long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                    itemUri =  Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return itemUri;
    }
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        String extension = null;
        final int lastDot = fileName.lastIndexOf('.');
        if ((lastDot >= 0)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        return extension;
    }
    /**
     * Prepare matrix to transfer view point to native preview point.
     * @param matrix The matrix.
     * @param mirror Whether need mirror or not. For front camera,should the point be mirrored.
     * @param displayOrientation The current displayOrientation.
     * @param viewWidth The preview width.
     * @param viewHeight The preview height.
     */
    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }
    /**
     * Get current camera display rotation.
     * @param activity camera activity.
     * @return the activity orientation.
     */
    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Rotate the view orientation.
     * @param view The view need to rotated.
     * @param orientation The rotate orientation value.
     * @param animation Is need animation when rotate.
     */
   /* public static void rotateViewOrientation(View view, int orientation, boolean animation) {
        if (view == null) {
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(orientation, animation);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                rotateViewOrientation(group.getChildAt(i), orientation, animation);
            }
        }
    }*/

}
