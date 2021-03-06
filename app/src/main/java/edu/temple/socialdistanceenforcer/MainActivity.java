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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private Map<String, Map> deviceData;
    private String inDir = "/data";
    private File[] inFiles;
    private Map<String, List<Point>> dataMap = new HashMap<>();
    private ArrayList<Device> deviceList = new ArrayList<>();
    ListView listView;
    DeviceAdapter deviceAdapter;

    // default key if we have not seen a device type before
    // we set this to our phone key as I assume phones are the most common mobile device
    private String default_key = "524_512.txt";

    // reads our dataset of labeled RSSI values from disk
    private void initialize_dataset() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                FileOutputStream fileOutputStream = null;
                try {
                    // attempt to read all files in the data directory
                    File inDir = new File(MainActivity.this.getExternalFilesDir(null), "data");
                    Log.d(DEBUG_TAG, "INDIR " + inDir.getAbsolutePath());

                    File[] inFiles = inDir.listFiles();
                    Log.d(DEBUG_TAG, "Size: " + inFiles.length);

                    // loop through each file
                    for (int i = 0; i < inFiles.length; i++) {
                        File curFile = inFiles[i];

                        String curName = curFile.getName();
                        Log.d(DEBUG_TAG, "FileName: [" + curName + "]");


                        // read the file line by line
                        FileInputStream fileInputStream = new FileInputStream(curFile);
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                        String line = bufferedReader.readLine();
                        while (line != null) {
                            // parse the data
                            String[] vals = line.split(",");
                            int dist = Integer.parseInt(vals[0]);
                            int RSSI = Integer.parseInt(vals[1]);
                            int count = Integer.parseInt(vals[2]);

                            // construct a single point
                            Point curPoint = new Point(dist, RSSI, count);

                            // add the device to our mapping
                            if(!dataMap.containsKey(curName)){
                                dataMap.put(curName, new ArrayList<Point>());
                            }

                            // add the point to our device's mapping
                            dataMap.get(curName).add(curPoint);

                            line = bufferedReader.readLine();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // attempt to turn on bluetooth
    protected void enableBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // check if device is bluetooth enabled
        if(bluetoothAdapter != null){
            // bluetooth exists
            if(bluetoothAdapter.isEnabled()){
                // bluetooth is already enabled
                String address = bluetoothAdapter.getAddress();
                String name = bluetoothAdapter.getName();

                String status = name + " : " + address;
                statusText.setText(status);

                // dynamically display connect disconnect button
                this.connectButton.setVisibility(View.INVISIBLE);
                this.disconnectButton.setVisibility(View.VISIBLE);
            }else{
                // bluetooth is not enabled
                statusText.setText("Bluetooth is disabled");

                // dynamically display connect disconnect button
                this.connectButton.setVisibility(View.VISIBLE);
                this.disconnectButton.setVisibility(View.INVISIBLE);
            }
        }else{
            // bluetooth does not exist
            statusText.setText("ERROR: Device Does Not Support Bluetooth");

            // dynamically display connect disconnect button
            this.connectButton.setVisibility(View.INVISIBLE);
            this.disconnectButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get our activity GUI components
        this.connectButton = findViewById(R.id.connectButton);
        this.disconnectButton = findViewById(R.id.disconnectButton);
        this.statusText = findViewById(R.id.statusText);
        this.listView = findViewById(R.id.deviceList);

        // instantiate our listview with a deviceadapter
        deviceAdapter = new DeviceAdapter(MainActivity.this, deviceList);
        listView.setAdapter(deviceAdapter);

        // get our dataset for our KNN from disk
        initialize_dataset();

        // set up the bluetooth receiver for discovery
        Log.d(DEBUG_TAG, "Registering discovery receiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryResultReceiver, intentFilter);

        // update our GUI
        this.displayUI();

        // define a listener for connection
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // turn on the discoverability
                enableDiscoverability();
            }
        });

        // define a listener for disconnection
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable our bluetooth adapter
                bluetoothAdapter.disable();

                // update the GUI
                displayUI();
            }
        });
    }

    // check perms for discovery and start discovery
    protected void enableDiscoverability(){
        // register bluetooth receiver
        String scanModeChanged = BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
        String beDiscoverable = BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE;
        IntentFilter intentFilter = new IntentFilter(scanModeChanged);
        registerReceiver(discoveryResultReceiver, intentFilter);

        // check perms
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
        // user approves discovery
        if(requestCode == DISCOVERY_REQUEST){
            // display that we are starting discovery
            Toast.makeText(MainActivity.this, "Discovery in progress", Toast.LENGTH_SHORT).show();

            // update the GUI
            displayUI();

            // attempt to find nearby devices
            findDevices();
        }
    }

    // attempt to find nearby devices for discovery
    private void findDevices(){
        // check if we have recently paired devices (we don't actually want this, I was just experimenting
        // with bluetooth capability
        String lastUsedRemoteDevice = getLastUsedRemoteBTDevice();
        if(lastUsedRemoteDevice != null){
            Log.d(DEBUG_TAG, "Last used remote device exists");
            // we have previously paired devices
            toastText = "Checking for previously paired devices: " + lastUsedRemoteDevice;
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

            // loop through recently paired devices
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for(BluetoothDevice pairedDevice : pairedDevices){
                if(pairedDevice.getAddress().equals(lastUsedRemoteDevice)){
                    toastText = "Found device: " + pairedDevice.getName() + "|" + lastUsedRemoteDevice;
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    remoteDevice = pairedDevice;
                }
            }
        }else{
            // no known paired devices found (this is where the discovery begins)
            toastText = "Starting discovery for remote devices...";
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

            // start discovery
            if(bluetoothAdapter.startDiscovery()) {
                toastText = "Discovery thread started. Scanning for devices...";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

            }else{
                // discovery failed
                Toast.makeText(MainActivity.this, "Unable to scan for bluetooth devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // create a broadcast receiver for bluetooth
    final BroadcastReceiver discoveryResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            // get what what received
            String action = intent.getAction();
            Log.d(DEBUG_TAG, action);

            Log.d(DEBUG_TAG, "OnReceive");
            // check if a device was found
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // bluetooth device found

                // get the device
                remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // notify user of device
                toastText = "Discovered: " + remoteDevice.getName() + ":" + Integer.toString(remoteDevice.getBluetoothClass().getDeviceClass())
                        + "_" + Integer.toString(remoteDevice.getBluetoothClass().getMajorDeviceClass())
                        + ":" + Short.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

                // log device
                Log.d(DEBUG_TAG, toastText);

                // get the rssi reading
                short RSSI_val = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // used for dataset generation
                // commented out for live demo
                /*
                // construct our file data for data acquistion (this is used in constructing the dataset)
                String file_data = remoteDevice.getName() + "|" + Short.toString(RSSI_val) + "\n";

                // get dataset file name
                String dataFile = remoteDevice.getName() + "_class_" + remoteDevice.getBluetoothClass().getDeviceClass() + "_major_"
                        + remoteDevice.getBluetoothClass().getMajorDeviceClass() + "_15_foot.txt";

                // comment out if not generating dataset
                log_to_file(dataFile, file_data);
                */

                // get the estimated device's distance
                float distance = get_distance(RSSI_val, Integer.toString(remoteDevice.getBluetoothClass().getDeviceClass()),
                        Integer.toString(remoteDevice.getBluetoothClass().getMajorDeviceClass()));

                // construct a device object with name and distance for listview
                Device curDevice = new Device(remoteDevice.getName(), distance);

                // track the device
                deviceList.add(curDevice);

                // if our device is closer than 6 feet
                if(curDevice.distance < 6){
                    // play an alarm
                    MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.alarm_loud);
                    mp.start();
                }

                // update our listview adapter
                ((DeviceAdapter)listView.getAdapter()).notifyDataSetChanged();

                // log the device and distance
                Log.d(DEBUG_TAG, remoteDevice.getName() + ": distance [" + distance + "]");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // reset bluetooth discovery after 10 seconds
                Log.d(DEBUG_TAG, "Discovery finished, relaunching");

                // display all seen devices
                for(Device temp : deviceList){
                    Log.d(DEBUG_TAG, "\t" + temp.name + " : " + temp.distance + " ft");
                }

                // reset the device list
                deviceList.clear();

                // launch a new discovery
                findDevices();
            }

        }
    };

    // get k closest elements to our rssi value
    protected List<Point> getClosestK(List<Point> points, int rssi, int k){
        // list to store the k closest points
        List<Point> ret_points = new ArrayList<>();

        // get distances to each point
        List<Integer> dists = new ArrayList<>();
        for(Point point : points){
            dists.add(point.getDistance(rssi));
        }

        // get indices to smallest k points (slow but better than sorting if k is small)
        Set<Integer> usedIndices = new HashSet<>();
        int count = 0;

        // loop k times
        for(int j = 0; j < k; j++){
            // set min to max possible
            int min_dist = Integer.MAX_VALUE;
            int min_index = -1;

            // loop through each distance
            for (int i = 0; i < dists.size(); i++) {
                int curDist = dists.get(i);

                // skip if we have already selected the point
                if (usedIndices.contains(i)) {
                    continue;
                }

                // find index of min
                if (curDist < min_dist) {
                    min_dist = curDist;
                    min_index = i;
                }
            }

            // stop if we cant find any more points
            if(min_index == -1){
                break;
            }

            // track our new index
            usedIndices.add(min_index);

            // increment the number of samples obtained
            count += points.get(min_index).count;

            // stop if we hit our limit
            if(count > k){
                break;
            }
        }

        // add our k min points to our list
        for(int index : usedIndices){
            ret_points.add(points.get(index));
        }

        // return our points
        return ret_points;
    }

    // get the distance from the RSSI value according to class and major class of device
    protected float get_distance(short RSSI_val, String class_a, String major){
        // get our device's key according to class
        String key = class_a + "_" + major + ".txt";

        // if the unknown device is not in our acquired dataset, assume it is a default device
        if(!dataMap.containsKey(key)){
            key = default_key;
        }

        // get k closest points
        List<Point> points = getClosestK(dataMap.get(key), RSSI_val, 15);

        // calculate the average distance
        float distance = 0;
        int total = 0;
        for(Point point : points){
            distance += point.dist * point.count;
            total += point.count;
        }

        distance /= total;

        // return the estimated distance
        return distance;
    }

    // log to file during dataset acquisition
    protected void log_to_file(final String dataFile, final String file_data){
        Log.d(DEBUG_TAG, dataFile);

        new Thread() {
            @Override
            public void run() {
                super.run();
                FileOutputStream fileOutputStream = null;
                try {
                    // open the device's corresponding output file in append, create one if it does not exist
                    File file = new File(MainActivity.this.getExternalFilesDir(null), dataFile);
                    Log.d(DEBUG_TAG, file.getAbsolutePath());
                    String outPath = file.getAbsolutePath();
                    file.createNewFile();
                    fileOutputStream = new FileOutputStream(file, true);

                    // write the RSSI value
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
    }

    // get the last used bluetooth device (not used for this project)
    private String getLastUsedRemoteBTDevice(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        return sharedPreferences.getString("LAST_REMOTE_DEVICE_ADDRESS", null);
    }

    // update the GUI
    protected void displayUI(){
        this.enableBluetooth();
    }

    // unregister the bluetooth receiver on screen rotation
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoveryResultReceiver);
    }
}
