package com.example.edwinkurniawan.ar;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.OutputStream;
import java.net.URISyntaxException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MainScreen extends AppCompatActivity {

    ImageView procImg;
    private Button checkImgButton;
    private Button openGLButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        checkImgButton = (Button) findViewById(R.id.checkImgBtn);
        openGLButton = (Button) findViewById(R.id.openGLButton);
        checkImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainScreen.this, ImageCount.class);
                startActivity(intent);
            }
        });
        openGLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainScreen.this, CameraScreen.class);
                startActivity(intent);
            }
        });
    }

    private void loadImages(){
        Bitmap bmpImg = BitmapFactory.decodeResource(getResources(), R.drawable.sktt1);
        Mat m = new Mat(bmpImg.getWidth(),bmpImg.getHeight(), CvType.CV_8UC3);
        Utils.bitmapToMat(bmpImg,m);
        Log.d (this.getClass().getSimpleName(), "width of :"  + m.width());
        int lowThresh = 70;
        int highThresh = 100;

        Mat canny_mat = new Mat();
        Imgproc.Canny(m, canny_mat, lowThresh, highThresh);
        Bitmap canny_bmp = Bitmap.createBitmap(canny_mat.width(), canny_mat.height(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(canny_mat,canny_bmp);
        procImg.setImageBitmap(canny_bmp);
        //Imgcodecs.imwrite("drawable://canny_img.png", canny_mat);
    }

}
