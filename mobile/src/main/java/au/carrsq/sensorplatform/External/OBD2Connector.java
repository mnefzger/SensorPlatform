package au.carrsq.sensorplatform.External;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

/**
 * This class is responsible for setting up a Bluetooth connection to the OBD2 device
 * It stores the device and socket in the class variables of OBD2Connection
 */
public class OBD2Connector {

    public interface IConnectionEstablished {
        void onConnectionEstablished();
    }

    private Context app;
    private IConnectionEstablished callback;
    private final String TAG = "OBD_CONNECTOR";

    private boolean receiverRegistered = false;
    private BluetoothAdapter btAdapter;

    private boolean found = false;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d(TAG, device.getName() + "");

                // TODO do not hardcode device name, show list of possible options
                if(device.getName() != null && device.getName().equals("OBDII")) {
                    app.sendBroadcast(new Intent("OBD_FOUND"));
                    found = true;

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
    };


    public OBD2Connector(Context app, IConnectionEstablished callback) {
        Log.d("OBD", "Init connector.");
        this.app = app;
        this.callback = callback;

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!btAdapter.isEnabled())
            btAdapter.enable();

        registerReceiver();

        Log.d(TAG, "Start discovery...");
        btAdapter.startDiscovery();

        // after this timeout, cancel the discovery
        startTimeout(18000);
    }

    Handler h = new Handler(Looper.getMainLooper());
    Runnable r = new Runnable() {
        @Override
        public void run() {
            if(!found) {
                app.sendBroadcast(new Intent("OBD_NOT_FOUND"));
                try{
                    app.unregisterReceiver(mReceiver);
                } catch (Exception e) {
                    Log.e("OBD RECEIVER", e.toString());
                }
                receiverRegistered = false;
                btAdapter.cancelDiscovery();
            }
        }
    };

    private void startTimeout(int milliseconds) {
        h.postDelayed(r, milliseconds);
    }

    void stopTimeout() {
        h.removeCallbacks(r);
        unregisterReceiver();
        Log.d(TAG, "Stop timeout");
    }

    public void unregisterReceiver() {
        if(receiverRegistered)
            app.unregisterReceiver(mReceiver);
        receiverRegistered = false;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if(!receiverRegistered && (OBD2Connection.sock == null || !OBD2Connection.sock.isConnected()) ) {
            app.registerReceiver(mReceiver, filter);
            receiverRegistered = true;
            Log.d(TAG, "Receiver registered");
        }
    }

    /**
     * Once the correct device is found, establish the connection
     */
    private void connectToOBD2Device() {
        try{
            OBD2Connection.sock = BluetoothManager.connect(OBD2Connection.obd2Device);
            OBD2Connection.connected = true;
            Log.d(TAG, "Connected to: " + OBD2Connection.obd2Device.getName() + "-> " + OBD2Connection.sock.isConnected());
            app.sendBroadcast(new Intent("OBD_CONNECTED"));
            callback.onConnectionEstablished();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
