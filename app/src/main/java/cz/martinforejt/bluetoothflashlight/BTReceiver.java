package cz.martinforejt.bluetoothflashlight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Martin Forejt on 21.08.2016.
 * forejt.martin97@gmail.com
 */
public class BTReceiver extends BroadcastReceiver {

    private BTFinder finder;

    public BTReceiver(BTFinder finder){
        this.finder = finder;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.d("BTReceiver", "DISCOVERY STARTED");
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.d("BTReceiver", "DISCOVERY FINISHED");
                finder.finished();
                break;
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                finder.newDevice(device);
                break;
        }
    }
}
