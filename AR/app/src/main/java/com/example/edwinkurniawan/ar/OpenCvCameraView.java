package com.example.edwinkurniawan.ar;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by Edwin Kurniawan on 6/1/2016.
 */
public class OpenCvCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "OpenCvCameraView";
    private String mPictureFileName;

    public OpenCvCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public OpenCvCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureSize(resolution.width, resolution.height);
        //params.setPreviewSize(resolution.width, resolution.height);
        mCamera.setParameters(params);
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

    }
}
