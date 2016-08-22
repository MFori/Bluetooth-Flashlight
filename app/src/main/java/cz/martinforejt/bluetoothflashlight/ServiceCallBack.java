package cz.martinforejt.bluetoothflashlight;

/**
 * Created by Martin Forejt on 22.08.2016.
 * forejt.martin97@gmail.com
 */
public interface ServiceCallBack {
    void defaultLayout();
    boolean switchBoth();
    void controlDeviceLayout(boolean hasFlash);
    void controlMeLayout();
    void connectDeviceLayout(String deviceName);
}
