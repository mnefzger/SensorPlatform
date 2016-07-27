package mnefzger.de.sensorplatform.External;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.github.pires.obd.commands.protocol.TimeoutCommand;

import java.io.IOException;

import mnefzger.de.sensorplatform.DataProvider;
import mnefzger.de.sensorplatform.ISensorCallback;


public class OBD2Provider extends DataProvider{
    ISensorCallback callback;
    private BluetoothDevice obd2Device;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket socket;

    private Activity app;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getName().equals("OBDII")) {
                    obd2Device = device;
                    btAdapter.cancelDiscovery();
                    connectToOBD2Device();
                }
            }
        }
    };

    public OBD2Provider(Activity app, ISensorCallback callback) {
        BluetoothManager.verifyBluetoothPermissions(app);
        this.callback = callback;
        this.app = app;

        searchForOBD2Device();
    }

    private void searchForOBD2Device() {
        Log.d("BLUETOOTH", "Start discovery...");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        app.registerReceiver(mReceiver, filter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.startDiscovery();
    }

    private void connectToOBD2Device() {
        try{
            socket = BluetoothManager.connect(obd2Device);
            app.unregisterReceiver(mReceiver);
            try {
                new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.e("BLUETOOTH", "Could not connect to OBD2 device.", e);
        }
    }

    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }
}
