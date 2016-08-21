package cz.martinforejt.bluetoothflashlight;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Martin Forejt on 21.08.2016.
 * forejt.martin97@gmail.com
 */
public interface BTFinder {
    void newDevice(BluetoothDevice device);
    void finished();
}
