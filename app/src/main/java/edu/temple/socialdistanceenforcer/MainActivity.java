package edu.temple.socialdistanceenforcer;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    Button connectButton;
    Button disconnectButton;
    TextView statusText;
    BluetoothAdapter bluetoothAdapter;

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

            }else{
                // bluetooth is not enabled
                statusText.setText("Bluetooth is disabled");
            }
        }else{
            // bluetooth does not exist
            statusText.setText("ERROR: Device Does Not Support Bluetooth");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.connectButton = findViewById(R.id.connectButton);
        this.disconnectButton = findViewById(R.id.disconnectButton);
        this.statusText = findViewById(R.id.statusText);

        this.enableBluetooth();
    }
}
