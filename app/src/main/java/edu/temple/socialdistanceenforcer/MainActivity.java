package edu.temple.socialdistanceenforcer;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button connectButton;
    Button disconnectButton;
    TextView statusText;
    BluetoothAdapter bluetoothAdapter;

    BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
            String curStateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(curStateExtra, -1);
            int prevState = intent.getIntExtra(prevStateExtra, -1);

            String stateToastText = "";
            if(state == BluetoothAdapter.STATE_TURNING_ON){
                stateToastText = "Bluetooth is turning on";
            }else if(state == BluetoothAdapter.STATE_ON) {
                stateToastText = "Bluetooth is on";
                displayUI();
            }else if(state == BluetoothAdapter.STATE_TURNING_OFF) {
                stateToastText = "Bluetooth is turning off";
            }else if(state == BluetoothAdapter.STATE_OFF){
                stateToastText = "Bluetooth is off";
                displayUI();
            }

            Toast.makeText(MainActivity.this, stateToastText, Toast.LENGTH_SHORT).show();
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

        this.displayUI();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
                String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                IntentFilter intentFilter = new IntentFilter(actionStateChanged);
                registerReceiver(bluetoothStateReceiver, intentFilter);
                startActivityForResult(new Intent(actionRequestEnable), 0);
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

    protected void displayUI(){
        this.enableBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothStateReceiver);
    }
}
