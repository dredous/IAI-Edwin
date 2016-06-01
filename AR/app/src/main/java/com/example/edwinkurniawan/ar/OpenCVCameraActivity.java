package com.example.edwinkurniawan.ar;

import android.app.Activity;
import android.graphics.*;
import android.graphics.Point;
import android.hardware.*;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.ListIterator;

/**
 * Created by Edwin Kurniawan on 6/1/2016.
 */
public class OpenCVCameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    private OpenCvCameraView mOpenCvCameraView;
    private List<android.hardware.Camera.Size> mResolutionList;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    private Bitmap bm;
    private GLSurfaceView mGlView;

    //callback buat enable preview punya OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV Camera", "OpenCV loaded Succesfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    public OpenCVCameraActivity() {

    }

    public Bitmap getBm () {
        return bm;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("OpenCV Camera", "Called onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mGlView = new GLSurfaceView(this);
        setContentView(R.layout.opencv_camera_screen);
        mOpenCvCameraView = (OpenCvCameraView) findViewById(R.id.CameraCVView);
        mOpenCvCameraView.setMaxFrameSize(1920, 1080);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mResolutionMenu = menu.addSubMenu("Resolutions");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Camera.Size> resolutionItr = mResolutionList.listIterator();
        int idx = 0;
        while(resolutionItr.hasNext()) {
            Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE, Integer.valueOf(element.width).toString() + " x " + Integer.valueOf(element.height).toString());
            idx++;
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("OpenCV Camera", "called onOptionsItemSelected; selected item: " + item);
        if(item.getGroupId() == 1) {
            int id = item.getItemId();
            Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            mOpenCvCameraView.setMinimumWidth(resolution.width);
            mOpenCvCameraView.setMinimumHeight(resolution.height);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = processImage(inputFrame.rgba());
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
        return mRgbaT;
    }

    private Mat processImage(Mat input) {
        Mat grayMat = new Mat(input.rows(), input.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_RGB2GRAY);
        return grayMat;
    }

}
