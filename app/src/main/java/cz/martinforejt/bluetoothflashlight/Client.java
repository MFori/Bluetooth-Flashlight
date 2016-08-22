package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Martin Forejt on 19.08.2016.
 * forejt.martin97@gmail.com
 */
public class Client extends Pipe {
    private BluetoothAdapter mBluetoothAdapter;
    public static final UUID MY_UUID = UUID.fromString("cad38610-c72f-438b-935d-c6bbe90a5a33");

    private ConnectThread connThread = null;

    public Client(Context context) {
        super(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connect(BluetoothDevice device){
        connThread = new ConnectThread(device);
        connThread.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mDevice = device;

            try {
                 tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mSocket = tmp;
        }

        public void run(){
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException connectException){
                connectListener.onFailure(mDevice);
                /*((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectListener.onFailure(mDevice);
                    }
                });*/
                try{
                    mSocket.close();
                } catch (IOException closeException){
                    closeException.printStackTrace();
                }
                return;
            }

            connectedDevice = mSocket.getRemoteDevice();
            openPipe(mSocket);
            connectListener.onSuccess(mDevice);
            /*((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectListener.onSuccess(mDevice);
                }
            });*/
        }

        public void cancel(){
            try{
                mSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        if(connThread!=null){
            connThread.cancel();
            connThread = null;
        }
    }

}
