package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements BTFinder, AdapterView.OnItemClickListener, ServiceCallBack {

    private static final int REQUEST_ENABLE_BT = 666;
    private static final int REQUEST_MAKE_DISCOVERABLE_BT = 333;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BTReceiver mReceiver = new BTReceiver(this);
    private BTService mService = null;

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private TextView title;
    private ListView devicesList;
    private ProgressBar loading;
    private Button search;
    private SwitchCompat switchBoth;
    private LinearLayout defaultLayout, connectLayout, controlLayout, controlMeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layouts();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            AlertHelper.Create(this, "Error", "Sorry, your device doesn't support bluetooth", "OK", true).show();
            return;
        }

        startBTService();
        registerReceiver();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            makeDeviceDiscoverable();
        else {
            findPairedDevices();
            mBluetoothAdapter.startDiscovery();
            loading.setVisibility(View.VISIBLE);
            search.setVisibility(View.INVISIBLE);
        }

        devicesList.setAdapter(adapter);
        devicesList.setOnItemClickListener(this);
    }

    @Override
    public void newDevice(BluetoothDevice device) {
        if (!devices.contains(device)) {
            devices.add(device);
            adapter.add(device.getName());
            devicesList.setSelection(adapter.getCount() - 1);
        }
    }

    @Override
    public void finished() {
        loading.setVisibility(View.INVISIBLE);
        search.setVisibility(View.VISIBLE);
    }

    private void findPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
                adapter.add(device.getName());
            }
        }
    }

    public void send1(View v) {
        if (mService.getClient() != null) {
            mService.getClient().send(Message.create(Message.TYPE_LIGHT));
        } else if (mService.getServer() != null) {
            mService.getServer().send(Message.create(Message.TYPE_LIGHT));
        }
    }

    /**
     * Search button clicked - start discovery devices
     *
     * @param v View
     */
    public void search(View v) {
        mBluetoothAdapter.startDiscovery();
        loading.setVisibility(View.VISIBLE);
        search.setVisibility(View.INVISIBLE);
    }

    private void layouts() {
        defaultLayout = (LinearLayout) findViewById(R.id.default_layout);
        connectLayout = (LinearLayout) findViewById(R.id.connect_layout);
        controlLayout = (LinearLayout) findViewById(R.id.control_layout);
        controlMeLayout = (LinearLayout) findViewById(R.id.control_me_layout);

        title = (TextView) findViewById(R.id.main_title);
        devicesList = (ListView) findViewById(R.id.devices_list);
        loading = (ProgressBar) findViewById(R.id.loading);
        loading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        search = (Button) findViewById(R.id.search);
        switchBoth = (SwitchCompat) findViewById(R.id.switchBoth);
    }

    public void connectDeviceLayout(String deviceName) {
        defaultLayout.setVisibility(View.GONE);
        connectLayout.setVisibility(View.VISIBLE);
        controlLayout.setVisibility(View.GONE);
        controlMeLayout.setVisibility(View.GONE);
        title.setText(deviceName);
    }

    public void defaultLayout() {
        defaultLayout.setVisibility(View.VISIBLE);
        connectLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.GONE);

        devicesList.setVisibility(View.VISIBLE);
        search.setVisibility(View.VISIBLE);
        loading.setVisibility(View.INVISIBLE);
        controlMeLayout.setVisibility(View.GONE);

        title.setText("Choose device");
    }

    public void controlDeviceLayout(boolean hasFlash) {
        defaultLayout.setVisibility(View.GONE);
        connectLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.VISIBLE);
        controlMeLayout.setVisibility(View.GONE);

        try {
            title.setText(mService.getConnectedDevice().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!hasFlash) {
            Toast.makeText(this, "Connected device doesn't has flash light", Toast.LENGTH_LONG).show();
        }
    }

    public void controlMeLayout() {
        defaultLayout.setVisibility(View.GONE);
        connectLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.GONE);
        controlMeLayout.setVisibility(View.VISIBLE);

        title.setText(mService.getConnectedDevice().getName());
    }

    /**
     * Register receiver for bt actions
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    /**
     * Start bt service
     */
    private void startBTService() {
        Intent intent = new Intent(this, BTService.class);
        if (!BTService.isActive) startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Make device visible for other for 300 sec
     */
    private void makeDeviceDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_MAKE_DISCOVERABLE_BT);
    }

    /**
     * Check if both switch is checkded
     *
     * @return bool
     */
    public boolean switchBoth() {
        return switchBoth.isChecked();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BTService.BTBinder binder = (BTService.BTBinder) iBinder;
            mService = binder.getService();
            mService.setCallBacks(MainActivity.this);
            //mService.server();
            switch (mService.getState()) {
                case BTService.STATE_CONTROL:
                    controlDeviceLayout(false);
                    break;
                case BTService.STATE_CONTROL_ME:
                    controlMeLayout();
                    break;
                case BTService.STATE_DEFAULT:
                case BTService.STATE_DISABLE:
                    mService.server();
                    break;
            }
            Toast.makeText(MainActivity.this, String.valueOf(mService.getState()), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService.finish();
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = devices.get(position);
        mService.client(device);
    }

    public void oneApply(View view) {
        switchBoth.setChecked(false);
    }

    public void twoApply(View view) {
        switchBoth.setChecked(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                mService.server();
                mBluetoothAdapter.startDiscovery();
                findPairedDevices();
            } else {
                Log.d("BTAdapter", "Cant enable BT");
            }
        } else if (requestCode == REQUEST_MAKE_DISCOVERABLE_BT) {
            if (mBluetoothAdapter.isEnabled()) {
                findPairedDevices();
            }
            if (resultCode != Activity.RESULT_CANCELED) {
                mService.server();
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (controlLayout.getVisibility() == View.VISIBLE || controlMeLayout.getVisibility() == View.VISIBLE) {
            AlertHelper.Create(this, "Disconnect?", "Do you really want to disconnect from the '" +
                    mService.getConnectedDevice().getName() + "' device?")
                    .addButton("Yes", true, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mService.onCloseConnection();
                        }
                    })
                    .addButton("No", false, null)
                    .show();
        } else if (defaultLayout.getVisibility() == View.VISIBLE) {
            AlertHelper.Create(this, "Close?", "Do you really want to close app?")
                    .addButton("Yes", true, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            BTService.canConnect = false;
                            mService.finish();
                            finish();
                        }
                    })
                    .addButton("No", false, null)
                    .show();
        }
    }

    @Override
    public void onDestroy() {
        BTService.canConnect = false;
        if (mService.isActive) unbindService(mConnection);
        if (mBluetoothAdapter != null) unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}