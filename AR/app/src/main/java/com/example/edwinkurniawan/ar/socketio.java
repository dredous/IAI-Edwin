package com.example.edwinkurniawan.ar;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by Edwin Kurniawan on 5/15/2016.
 */
public class socketio {

    public Socket iosocket;
    private String count = "0";

    public socketio(String uri) {
        try {
            iosocket = IO.socket(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        count = "0";
        iosocket.connect();
        iosocket.on("countNum", onNewMessage);

    }

    public void sendMessage(String connMsg) {
        if(connMsg == "msg") {

        }
        else if(connMsg == "countImg") {
            iosocket.emit("countImg");
        }
    }

    public void closeConnection() {
        iosocket.disconnect();
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Integer data = (Integer) args[0];
            count = data.toString();
            Log.i("Socket IO", "Message Received: " + count + " inputs");
//            JSONObject data = (JSONObject) args[0];
//            try {
//                count = data.getString("cnt");
//            } catch (JSONException e) {
//                return;
//            }
        }
    };

    public String getCountNum() {
        return count+"";
    }

}
