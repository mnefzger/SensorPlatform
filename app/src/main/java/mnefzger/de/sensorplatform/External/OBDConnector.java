package mnefzger.de.sensorplatform.External;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;

/**
 * This class is responsible for setting up a Bluetooth connection to the OBD2 device
 * It stores the device and socket in the class variables of OBDConnection
 */
public class OBDConnector {
    private Activity app;
    private final String TAG = "OBD_CONNECTOR";

    private boolean receiverRegistered = false;
    private BluetoothAdapter btAdapter;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d(TAG, device.getName() + "");

                if(device != null) {
                    if(device.getName() != null && device.getName().equals("OBDII")) {
                        OBDConnection.obd2Device = device;
                        app.unregisterReceiver(mReceiver);
                        receiverRegistered = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                connectToOBD2Device();
                            }
                        }).start();
                    }
                }
            }
        }
    };


    public OBDConnector(Activity app) {
        this.app = app;

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        registerReceiver();

        Log.d(TAG, "Start discovery...");
        btAdapter.startDiscovery();
    }

    public void unregisterReceiver() {
        app.unregisterReceiver(mReceiver);
        receiverRegistered = false;
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if(!receiverRegistered && (OBDConnection.sock == null || !OBDConnection.sock.isConnected()) ) {
            app.registerReceiver(mReceiver, filter);
            receiverRegistered = true;
            Log.d(TAG, "Receiver registered");
        }

    }


    private void connectToOBD2Device() {
        try{
            OBDConnection.sock = BluetoothManager.connect(OBDConnection.obd2Device);
            OBDConnection.connected = true;
            Log.d(TAG, "Connected to: " + OBDConnection.obd2Device.getName() + "-> " + OBDConnection.sock.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
