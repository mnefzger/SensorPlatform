package mnefzger.de.sensorplatform.External;

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
 * It stores the device and socket in the class variables of OBD2Connection
 */
public class OBD2Connector {
    private Context app;
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
                        OBD2Connection.obd2Device = device;
                        app.unregisterReceiver(mReceiver);
                        receiverRegistered = false;
                        btAdapter.cancelDiscovery();
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


    public OBD2Connector(Context app) {
        this.app = app;

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!btAdapter.isEnabled()) {
            btAdapter.enable();
        }

        registerReceiver();

        Log.d(TAG, "Start discovery...");
        btAdapter.startDiscovery();
    }

    public void unregisterReceiver() {
        if(receiverRegistered)
            app.unregisterReceiver(mReceiver);
        receiverRegistered = false;
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if(!receiverRegistered && (OBD2Connection.sock == null || !OBD2Connection.sock.isConnected()) ) {
            app.registerReceiver(mReceiver, filter);
            receiverRegistered = true;
            Log.d(TAG, "Receiver registered");
        }
    }


    private void connectToOBD2Device() {
        try{
            OBD2Connection.sock = BluetoothManager.connect(OBD2Connection.obd2Device);
            OBD2Connection.connected = true;
            Log.d(TAG, "Connected to: " + OBD2Connection.obd2Device.getName() + "-> " + OBD2Connection.sock.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
