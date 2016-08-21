package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Martin Forejt on 19.08.2016.
 * forejt.martin97@gmail.com
 */
abstract class Pipe {
    private ConnectedThread connectedThread = null;
    protected ConnectListener connectListener = null;
    protected Context context;
    protected BluetoothDevice connectedDevice = null;

    /**
     * @param context Context
     */
    public Pipe(Context context) {
        this.context = context;
    }

    /**
     * @param socket BluetoothSocket
     */
    public void openPipe(BluetoothSocket socket) {
        if (connectedThread != null) connectedThread.cancel();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        send("");
    }

    /**
     * @param listener ConnectListener
     */
    public void setConnectListener(ConnectListener listener) {
        connectListener = listener;
    }

    public void send(String jsonString) {
        if (connectedThread != null) {
            Log.d("Pipe", "send: " + jsonString);
            connectedThread.write(jsonString.getBytes());
        } else {
            Log.d("Pipe", "Cant send: (" + jsonString + ")");
        }
    }

    public void cancel() {
        send(Message.create(Message.TYPE_END));
        connectedThread.cancel();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mInStream.read(buffer);
                    consumeMessage(new String(buffer, 0, bytes));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void consumeMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            int type = Integer.valueOf(json.get("type").toString());
            switch (type) {
                case Message.TYPE_INIT:
                    msgInit(json);
                    break;
                case Message.TYPE_END:
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectListener.onCloseConnection();
                        }
                    });
                    break;
                case Message.TYPE_LIGHT:
                    msgLight(json);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param msg JSONObject
     */
    private void msgInit(final JSONObject msg) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    connectListener.onClientRequest(connectedDevice, msg.get("both").equals("1"));
                } catch (JSONException e) {
                    connectListener.onClientRequest(connectedDevice, false);
                }
            }
        });
    }

    /**
     * @param msg JSONObject
     */
    private void msgLight(final JSONObject msg) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    connectListener.onLight(msg.getInt("light_type"));
                } catch (JSONException e) {
                    connectListener.onLight(Message.LIGHT_01);
                }
            }
        });
    }

}
