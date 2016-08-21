package cz.martinforejt.bluetoothflashlight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements BTFinder, AdapterView.OnItemClickListener, ConnectListener {

    private static final int REQUEST_ENABLE_BT = 666;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BTReceiver mReceiver = new BTReceiver(this);

    private Server server = null;
    private Client client = null;
    private List<BluetoothDevice> devices;
    private ArrayAdapter<String> adapter;

    private ListView devicesList;
    private ProgressBar loading;
    private Button search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loading = (ProgressBar) findViewById(R.id.loading);
        loading.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        search = (Button) findViewById(R.id.search);
        layouts();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // TODO NO ADAPTER
            Toast.makeText(MainActivity.this, "NO BLUETOOTH", Toast.LENGTH_LONG).show();
            return;
        }

        registerReceiver();
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            makeDeviceDiscoverable();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enaBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enaBt, REQUEST_ENABLE_BT);
        } else {
            server();
            mBluetoothAdapter.startDiscovery();
            loading.setVisibility(View.VISIBLE);
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

    /**
     * Start server thread and listen for clients
     */
    public void server() {
        server = new Server(this);
        server.listenSocket();
    }

    /**
     * Try to connect to server device as client
     *
     * @param device BluetoothDevice - server
     */
    public void client(BluetoothDevice device) {
        if (client != null) {
            client.stop();
        }

        client = new Client(this);
        client.setConnectListener(this);
        client.connect(device);

        connectDeviceLayout(device.getName());

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
     * Search button clicked - start discovery devices
     * @param v View
     */
    public void search(View v) {
        mBluetoothAdapter.startDiscovery();
        loading.setVisibility(View.VISIBLE);
        search.setVisibility(View.INVISIBLE);
    }

    private LinearLayout defaultLayout, connectLayout, controlLayout;

    private void layouts(){
        defaultLayout = (LinearLayout)findViewById(R.id.default_layout);
        connectLayout = (LinearLayout)findViewById(R.id.connect_layout);
        controlLayout = (LinearLayout)findViewById(R.id.control_layout);
    }

    private void connectDeviceLayout(String deviceName) {
        defaultLayout.setVisibility(View.GONE);
        connectLayout.setVisibility(View.VISIBLE);
        controlLayout.setVisibility(View.GONE);
    }

    private void defaultLayout() {
        defaultLayout.setVisibility(View.VISIBLE);
        connectLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.GONE);

        devicesList.setVisibility(View.VISIBLE);
        search.setVisibility(View.VISIBLE);
        loading.setVisibility(View.INVISIBLE);
    }

    private void controlDeviceLayout() {
        defaultLayout.setVisibility(View.GONE);
        connectLayout.setVisibility(View.GONE);
        controlLayout.setVisibility(View.VISIBLE);
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
        defaultLayout();
    }

    @Override
    public void onSuccess(BluetoothDevice serverDevice) {
        controlDeviceLayout();
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
                Log.d("BTAdapter", "Cant enable BT");
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}