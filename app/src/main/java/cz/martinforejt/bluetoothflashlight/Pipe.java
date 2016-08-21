package cz.martinforejt.bluetoothflashlight;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

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

    public Pipe(Context context){
        this.context = context;
    }

    public void openPipe(BluetoothSocket socket) {
        if(connectedThread != null) connectedThread.cancel();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    /**
     * @param listener ConnectListener
     */
    public void setConnectListener(ConnectListener listener) {
        connectListener = listener;
    }

    public void send(String jsonString) {
        if(connectedThread != null) {
            Log.d("PIPE", "SEND");
            connectedThread.write(jsonString.getBytes());
        } else {
            Log.d("PIPE", "NO SEND");
        }
    }

    public void cancel(){
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

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            Log.d("INPUT", "RUN");

            while (true) {
                Log.d("RUN", "AGAIN");
                try {
                    bytes = mInStream.read(buffer);
                    String m = new String(buffer, 0, bytes);
                    Log.d("INPUT", m);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("INPUT", "NO");
                    //break;
                }
            }
        }

        public void write(byte[] bytes) {
            try{
                mOutStream.write(bytes);
            } catch (IOException e){}
        }

        public void cancel(){
            try{
                mSocket.close();
            } catch (IOException e){}
        }
    }

}
