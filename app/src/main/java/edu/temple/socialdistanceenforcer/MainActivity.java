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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryResultReceiver, intentFilter);

        this.displayUI();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDiscoverability();
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

    protected void enableDiscoverability(){
        String scanModeChanged = BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
        String beDiscoverable = BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE;
        IntentFilter intentFilter = new IntentFilter(scanModeChanged);
        registerReceiver(discoveryResultReceiver, intentFilter);
        Intent intent = new Intent(beDiscoverable);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
        startActivityForResult(intent, DISCOVERY_REQUEST);
        // Begin discovery process, or request necessary permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            Log.d(DEBUG_TAG, "Valid Perms");
        else {
            Log.d(DEBUG_TAG, "REQUEST PERM");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION);
        }
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
        }else{
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

    final BroadcastReceiver discoveryResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            Log.d(DEBUG_TAG, action);

            Log.d(DEBUG_TAG, "OnReceive");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                toastText = "Discovered: " + remoteDevice.getName() + ":" + remoteDevice.getAddress()
                        + ":" + Short.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                Log.d(DEBUG_TAG, toastText);
                final String file_data = remoteDevice.getName() + "|" + Short.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)) + "\n";
                final String dataFile = remoteDevice.getName() + "_class_" + remoteDevice.getBluetoothClass().getDeviceClass() + "_major_"
                        + remoteDevice.getBluetoothClass().getMajorDeviceClass() + "_15_foot.txt";
                Log.d(DEBUG_TAG, dataFile);

                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        FileOutputStream fileOutputStream = null;
                        try {
                            File file = new File(MainActivity.this.getExternalFilesDir(null), dataFile);
                            Log.d(DEBUG_TAG, file.getAbsolutePath());
                            String outPath = file.getAbsolutePath();
                            file.createNewFile();
                            fileOutputStream = new FileOutputStream(file, true);

                            fileOutputStream.write(file_data.getBytes(), 0, file_data.length());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            try {
                                // close stream
                                if(fileOutputStream != null){
                                    fileOutputStream.close();
                                }
                            } catch (IOException ignored) {
                            }
                        }

                    }
                }.start();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(DEBUG_TAG, "Discovery finished, relaunching");
                //enableDiscoverability();
                findDevices();
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
        unregisterReceiver(discoveryResultReceiver);
    }
}
