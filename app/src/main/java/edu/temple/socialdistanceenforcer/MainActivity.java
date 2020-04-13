package edu.temple.socialdistanceenforcer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.MalformedInputException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    Button connectButton;
    Button disconnectButton;
    TextView statusText;
    BluetoothAdapter bluetoothAdapter;
    private static final int DISCOVERY_REQUEST = 1;
    private static final int REQUEST_PERMISSION = 2;
    private String toastText = "";
    private BluetoothDevice remoteDevice;
    private static final String DEBUG_TAG = "BLUETOOTH_APP";

    BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
            String curStateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(curStateExtra, -1);
            int prevState = intent.getIntExtra(prevStateExtra, -1);

            if(state == BluetoothAdapter.STATE_TURNING_ON){
                toastText = "Bluetooth is turning on";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }else if(state == BluetoothAdapter.STATE_ON) {
                toastText = "Bluetooth is on";
                displayUI();
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }else if(state == BluetoothAdapter.STATE_TURNING_OFF) {
                toastText = "Bluetooth is turning off";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }else if(state == BluetoothAdapter.STATE_OFF){
                toastText = "Bluetooth is off";
                displayUI();
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }
        }
    };

    protected void enableBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter != null){
            // bluetooth exists
            if(bluetoothAdapter.isEnabled()){
                // bluetooth is already enabled

                String address = bluetoothAdapter.getAddress();
                String name = bluetoothAdapter.getName();

                String status = name + " : " + address;
                statusText.setText(status);

                this.connectButton.setVisibility(View.INVISIBLE);
                this.disconnectButton.setVisibility(View.VISIBLE);

            }else{
                // bluetooth is not enabled
                statusText.setText("Bluetooth is disabled");
                this.connectButton.setVisibility(View.VISIBLE);
                this.disconnectButton.setVisibility(View.INVISIBLE);
            }
        }else{
            // bluetooth does not exist
            statusText.setText("ERROR: Device Does Not Support Bluetooth");
            this.connectButton.setVisibility(View.INVISIBLE);
            this.disconnectButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.connectButton = findViewById(R.id.connectButton);
        this.disconnectButton = findViewById(R.id.disconnectButton);
        this.statusText = findViewById(R.id.statusText);

        Log.d(DEBUG_TAG, "Registering discovery receiver");
        registerReceiver(discoveryResultReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        this.displayUI();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
                //String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                //IntentFilter intentFilter = new IntentFilter(actionStateChanged);
                //registerReceiver(bluetoothStateReceiver, intentFilter);
                //startActivityForResult(new Intent(actionRequestEnable), 0);
                String scanModeChanged = BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
                String beDiscoverable = BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE;
                IntentFilter intentFilter = new IntentFilter(scanModeChanged);
                registerReceiver(discoveryResultReceiver, intentFilter);
                startActivityForResult(new Intent(beDiscoverable), DISCOVERY_REQUEST);
                // Begin discovery process, or request necessary permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    Log.d(DEBUG_TAG, "Valid Perms");
                else {
                    Log.d(DEBUG_TAG, "REQUEST PERM");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION);
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.disable();
                displayUI();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == DISCOVERY_REQUEST){
            Toast.makeText(MainActivity.this, "Discovery in progress", Toast.LENGTH_SHORT).show();
            displayUI();
            findDevices();
        }
    }

    private void findDevices(){
        String lastUsedRemoteDevice = getLastUsedRemoteBTDevice();
        if(lastUsedRemoteDevice != null){
            Log.d(DEBUG_TAG, "Last used remote device exists");
            // we have previously paired devices
            toastText = "Checking for previously paired devices: " + lastUsedRemoteDevice;
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for(BluetoothDevice pairedDevice : pairedDevices){
                if(pairedDevice.getAddress().equals(lastUsedRemoteDevice)){
                    toastText = "Found device: " + pairedDevice.getName() + "|" + lastUsedRemoteDevice;
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    remoteDevice = pairedDevice;
                }
            }
        }

        if(remoteDevice == null){
            // no known paired devices found
            toastText = "Starting discovery for remote devices...";
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

            // start discovery
            if(bluetoothAdapter.startDiscovery()) {
                toastText = "Discovery thread started. Scanning for devices...";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "Unable to scan for bluetooth devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    BroadcastReceiver discoveryResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                Log.d(DEBUG_TAG, "OnReceive");
                remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                toastText = "Discovered: " + remoteDevice.getName() + ":" + remoteDevice.getAddress();
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                Log.d(DEBUG_TAG, toastText);
            }
        }
    };

    private String getLastUsedRemoteBTDevice(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        return sharedPreferences.getString("LAST_REMOTE_DEVICE_ADDRESS", null);
    }

    protected void displayUI(){
        this.enableBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(discoveryResultReceiver);
    }
}
