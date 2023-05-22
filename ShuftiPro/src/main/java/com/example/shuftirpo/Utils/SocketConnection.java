package com.example.shuftirpo.Utils;

import android.os.AsyncTask;
import android.util.Log;

import com.example.shuftirpo.Singleton.SetAndGetData;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

public class SocketConnection {
    private final String TAG = "LogsSocket";

    /**
     * checking socket is already connected or not
     * if socket is not connected then request for new socket connection
     */
    public SocketConnection() {
        try {
            if (SetAndGetData.getInstance().getmSocket() == null || !SetAndGetData.getInstance().getmSocket().connected()) {
                CheckSocket checkSocket = new CheckSocket();
                checkSocket.doInBackground();
            }
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }
    }

    /**
     * preparing socket connection request
     * Make connection with socket
     * Set Data In Singleton
     * ON some socket listener which we want to listen when our socket is connected
     */
    private class CheckSocket extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {
            IO.Options mOptions = new IO.Options();
            mOptions.transports = new String[]{WebSocket.NAME};
            mOptions.secure = true;
            mOptions.upgrade = false;
            mOptions.reconnection = true;
            mOptions.reconnectionDelay = 1000;
            mOptions.reconnectionDelayMax = 5000;
            mOptions.reconnectionAttempts = 200;
            mOptions.query = "q1=" + SetAndGetData.getInstance().getReference();
            mOptions.path = "/v2.0";
            handleSocketValues(mOptions);
            SetAndGetData.getInstance().setConnected(false);
            SetAndGetData.getInstance().getmSocket().connect();
            return null;
        }
    }

    /**
     * setting & handling socket values
     *
     * @param mOptions IO.Options object to set connection values
     */
    private void handleSocketValues(IO.Options mOptions) {
        try {
            SetAndGetData.getInstance().setmSocket(IO.socket("https://app.shuftipro.com/", mOptions));
//            SetAndGetData.getInstance().setmSocket(IO.socket("https://st-app.shuftiapps.com/", mOptions));
            SetAndGetData.getInstance().getmSocket().on(Socket.EVENT_CONNECT, onConnect);
            SetAndGetData.getInstance().getmSocket().on(Socket.EVENT_CONNECT_ERROR, onDisconnect);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is called when our socket is connected
     */
    protected Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (!SetAndGetData.getInstance().isConnected()) {
                SetAndGetData.getInstance().setConnected(true);
                if (SetAndGetData.getInstance().getLogsList().size() > 0) {
                    for (int i = 0; i < SetAndGetData.getInstance().getLogsList().size(); i++) {
                        SetAndGetData.getInstance().getmSocket().emit("save-log", SetAndGetData.getInstance().getLogsList().get(i));
                    }
                    SetAndGetData.getInstance().getLogsList().clear();
                }
            }
        }
    };


    /**
     * this method is called when our socket is disconnected
     */
    protected Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            SetAndGetData.getInstance().setConnected(false);
        }
    };

}
