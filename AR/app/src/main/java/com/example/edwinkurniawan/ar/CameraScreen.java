package com.example.edwinkurniawan.ar;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Edwin Kurniawan on 5/16/2016.
 */
public class CameraScreen extends AppCompatActivity {

    private GLSurfaceView glView;
    private Triangle triangle1, triangle2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        glView = new GLSurfaceView(this);
        glView.setRenderer(new MyOpenGLRenderer());
        setContentView(glView);
    }

    class MyOpenGLRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d("MyOpenGLRenderer", "Surface Changed. Width= " + width + " Height=" + height);
            triangle1 = new Triangle(0.5f, 1, 0, 0);
            triangle2 = new Triangle(0.5f, 0, 1, 0);
            gl.glViewport(0,0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f, 100.0f);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d("MyOpenGLRenderer", "Surface Created!");
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, 0.0f, -5.0f);
            triangle1.draw(gl);
            gl.glTranslatef(2.0f, 0.0f, -5.0f);
            triangle2.draw(gl);
        }
    }
}
