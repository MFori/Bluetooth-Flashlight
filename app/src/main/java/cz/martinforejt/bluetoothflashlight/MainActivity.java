package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements BTFinder, AdapterView.OnItemClickListener, ConnectListener {

    private static final int REQUEST_ENABLE_BT = 666;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BTReceiver mReceiver = new BTReceiver(this);

    private ListView devicesList;
    Server server = null;
    Client client = null;

    private List<BluetoothDevice> devices;
    ArrayAdapter<String> adapter;
    BluetoothDevice d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // TODO NO ADAPTER
            Toast.makeText(MainActivity.this, "NO BLUETOOTH", Toast.LENGTH_LONG).show();
            return;
        }

        registerReceiver();
        makeDeviceDiscoverable();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enaBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enaBt, REQUEST_ENABLE_BT);
        } else {
            server();
            mBluetoothAdapter.startDiscovery();
        }

        devicesList = (ListView) findViewById(R.id.devices_list);
        devices = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
                adapter.add(device.getName());
            }
        }
        devicesList.setAdapter(adapter);
        devicesList.setOnItemClickListener(this);
    }

    public void newDevice(BluetoothDevice device) {
        if (!devices.contains(device)) {
            devices.add(device);
            adapter.add(device.getName());
        }
    }

    public void server() {
        server = new Server(this);
        server.listenSocket();
    }

    public void client(BluetoothDevice device) {
        if (client != null) {
            client.stop();
        }

        client = new Client(this);
        client.setConnectListener(this);
        client.connect(device);

        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public void send1(View v) {
        if (server != null) {
            server.send("Zpráva jedna");
        } else if (client != null) {
            client.send("Zpráva jedna");
        }
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
     * Make device visible for other for 300 sec
     */
    private void makeDeviceDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    @Override
    public void onFailure(BluetoothDevice serverDevice) {
        Toast.makeText(MainActivity.this, "NEPODAŘILO", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSuccess(BluetoothDevice serverDevice) {
        Toast.makeText(MainActivity.this, "PODAŘILO", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = devices.get(position);
        client(device);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                mBluetoothAdapter.startDiscovery();
            } else {
                // TODO cant start
                Toast.makeText(MainActivity.this, "NE", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}