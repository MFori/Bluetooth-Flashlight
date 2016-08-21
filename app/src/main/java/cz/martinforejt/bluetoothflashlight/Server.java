package cz.martinforejt.bluetoothflashlight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.util.UUID;

/**
 * Created by Martin Forejt on 19.08.2016.
 * forejt.martin97@gmail.com
 */
public class Server extends Pipe {

    private BluetoothAdapter mBluetoothAdapter;
    public static final String SERVER_NAME = "bluetoothserver";
    public static final UUID MY_UUID = UUID.fromString("cad38610-c72f-438b-935d-c6bbe90a5a33");

    private AcceptThread accThread = null;

    public Server(Context context) {
        super(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean listenSocket() {
        accThread = new AcceptThread();
        accThread.start();

        return true;
    }

    /**
     * class AcceptThread
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVER_NAME, MY_UUID);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mServerSocket.accept();
                } catch (Exception e) {
                    break;
                }

                if (socket != null) {
                    openPipe(socket);
                    /*new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            send("KOKOTE");
                        }
                    }, 3000);*/

                    try {
                        mServerSocket.close();
                    } catch (Exception e) {
                        break;
                    }
                    break;
                }
            }
        }

        /**
         * cancel listening socket
         */
        public void cancel() {
            try {
                mServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void stop(){
        if(accThread != null) {
            accThread.cancel();
            accThread = null;
        }
    }

}
