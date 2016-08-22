package cz.martinforejt.bluetoothflashlight;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Forejt on 22.08.2016.
 * forejt.martin97@gmail.com
 */
public class BTService extends Service implements ConnectListener {

    private final IBinder mBinder = new BTBinder();
    private ServiceCallBack activity = null;
    private Server server = null;
    private Client client = null;
    private FlashManager flashManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        flashManager = new FlashManager(this);
    }

    public void setCallBacks(ServiceCallBack callBacks) {
        this.activity = callBacks;
    }

    @Override
    public void onFailure(final BluetoothDevice serverDevice) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BTService.this, "Can't connect to " + serverDevice.getName(), Toast.LENGTH_LONG).show();
                activity.defaultLayout();
            }
        });
        server();
    }

    @Override
    public void onSuccess(BluetoothDevice serverDevice) {
        Map<String, String> params = new HashMap<>();
        params.put("both", activity.switchBoth() ? "1" : "0");
        params.put("has_flash", flashManager.hasFlash() ? "1" : "0");
        client.send(Message.create(Message.TYPE_INIT, params));
    }

    @Override
    public void onClientRequest(BluetoothDevice clientDevice, boolean both, final boolean hasFlash) {
        Map<String, String> params = new HashMap<>();
        params.put("has_flash", flashManager.hasFlash() ? "1" : "0");
        server.send(Message.create(Message.TYPE_ACCEPT, params));

        if (both) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.controlDeviceLayout(hasFlash);
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.controlMeLayout();
                }
            });
        }
    }

    @Override
    public void onServerAccept(BluetoothDevice serverDevice, final boolean hasFlash) {
        Log.d("Server:", "Accepted: " + serverDevice.getName());
        Log.d("Server:", "flash: " + String.valueOf(hasFlash));
        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.controlDeviceLayout(hasFlash);
            }
        });
    }

    @Override
    public void onCloseConnection() {
        if (server != null) server.cancel();
        else if (client != null) client.cancel();
        flashManager.flash(false);
        server();
        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.defaultLayout();
            }
        });
    }

    @Override
    public void onLight(int type) {
        flashManager.flash(!flashManager.isFlashOn);
    }

    /**
     * Start server
     */
    public void server() {
        server = new Server(this);
        server.listenSocket();
        server.setConnectListener(this);
    }

    /**
     * Start client - stop server
     *
     * @param device BluetoothDevice
     */
    public void client(final BluetoothDevice device) {
        if (client != null) {
            client.stop();
        }

        client = new Client(this);
        client.setConnectListener(this);
        client.connect(device);

        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.connectDeviceLayout(device.getName());
            }
        });

        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Return client instance
     *
     * @return Client|null
     */
    public Client getClient() {
        return client;
    }

    /**
     * Return server instance
     *
     * @return Server|null
     */
    public Server getServer() {
        return server;
    }

    /**
     * Stop bt sockets
     */
    public void finish() {
        if (server != null) server.cancel();
        else if (client != null) client.cancel();
        flashManager.flash(false);
    }

    public class BTBinder extends Binder {
        BTService getService() {
            return BTService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
