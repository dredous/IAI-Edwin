package com.example.edwinkurniawan.ar;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Created by Edwin Kurniawan on 5/10/2016.
 */
public class ImageCount extends AppCompatActivity{

    private TextView imageCnt;
    private EditText urlText;
    private Button checkDB;
    private Button updateButton;
    private socketio sockets;
    private boolean socketCreated = false;
    private boolean isBackgroundThread = false;
    private boolean backGroundThreadRunning = false;
    private String countImg = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_count);

        imageCnt = (TextView) findViewById(R.id.imgCount);
        urlText = (EditText) findViewById(R.id.urlInput);
        checkDB = (Button) findViewById(R.id.checkDB);
        updateButton = (Button) findViewById(R.id.updateCntBtn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (socketCreated == false) {
                    String uri = urlText.getText().toString();
                    sockets = new socketio(uri);
                    socketCreated = true;
                }
                getImageCountSQL();

            }
        });
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(socketCreated == false) {
                    Log.d("Sockets Status"," Socket IO is not initialized");
                    return;
                }
                else {
                    countImg = sockets.getCountNum();
                    imageCnt.setText(countImg);
                }
            }
        });
        //runInBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketCreated = false;
        sockets.closeConnection();
    }

    private void getImageCountSQL() {
        sockets.sendMessage("countImg");
    }

    public void runInBackground() {
        Log.i("Background Thread", "Background Thread is Running");
        if(isBackgroundThread)
            return;
        isBackgroundThread = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(socketCreated == true) {
                    countImg = sockets.getCountNum();
                    Log.i("Image Count", "" + countImg + " inputs");
//                imageCnt.setText(countImg);
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i("UI Thread", "UI Thread is Running");
                                imageCnt.setText(countImg);
                                runInBackground();
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    runInBackground();
                }
            }
        }).start();
    }





}
