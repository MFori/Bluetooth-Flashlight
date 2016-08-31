package cz.martinforejt.bluetoothflashlight;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
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

    public static boolean isActive = false;
    public static boolean canConnect = true;

    public static final int STATE_DISABLE = 0;
    public static final int STATE_DEFAULT = 1;
    public static final int STATE_CONTROL = 2;
    public static final int STATE_CONTROL_ME = 3;
    public int state = STATE_DISABLE;

    @Override
    public void onCreate() {
        flashManager = new FlashManager(this);
        isActive = true;
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
        state = STATE_DEFAULT;
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

        startForegroundNotify();

        if (both) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.controlDeviceLayout(hasFlash);
                }
            });
            state = STATE_CONTROL;
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.controlMeLayout();
                }
            });
            state = STATE_CONTROL_ME;
        }
    }

    @Override
    public void onServerAccept(BluetoothDevice serverDevice, final boolean hasFlash) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.controlDeviceLayout(hasFlash);
            }
        });
        state = STATE_CONTROL;
        startForegroundNotify();
        Log.d("Server:", "Accepted: " + serverDevice.getName());
        Log.d("Server:", "flash: " + String.valueOf(hasFlash));
    }

    @Override
    public void onCloseConnection() {
        if (server != null) {
            server.stop();
            server.cancel();
            server = null;
        } else if (client != null) {
            client.stop();
            client.cancel();
            client = null;
        }
        flashManager.flash(false);
        server();
        stopForeground(true);
        handler.post(new Runnable() {
            @Override
            public void run() {
                activity.defaultLayout();
            }
        });
        state = STATE_DEFAULT;
        if (!canConnect) {
            stopSelf();
        }
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
            server.cancel();
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

    public int getState() {
        return state;
    }

    private void startForegroundNotify() {
        String name = "";
        try {
            if (server != null) name = server.getConnectedDevice().getName();
            else if (client != null) name = client.getConnectedDevice().getName();
        } catch (NullPointerException e) {
            name = "BT device";
        }

        Intent intent = new Intent(BTService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(BTService.this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Bluetooth Flashlight")
                .setContentText(name)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        startForeground(100, mBuilder.build());
    }

    public BluetoothDevice getConnectedDevice() {
        if (server != null) return server.getConnectedDevice();
        else if (client != null) return client.getConnectedDevice();

        return null;
    }

    /**
     * Stop bt sockets
     */
    public void finish() {
        //if (server != null) server.cancel();
        //else if (client != null) client.cancel();
        //flashManager.flash(false);
        stopSelf();
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

    @Override
    public void onDestroy() {
        if (client != null) {
            client.cancel();
            client.stop();
        } else if (server != null) {
            server.cancel();
            server.stop();
        }
        flashManager.destroy();
        isActive = false;
        Log.d("service", "destroy");
    }
}