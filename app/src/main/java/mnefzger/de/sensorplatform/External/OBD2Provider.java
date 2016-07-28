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

import com.github.pires.obd.exceptions.NonNumericResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Handler;

import mnefzger.de.sensorplatform.DataProvider;
import mnefzger.de.sensorplatform.External.Commands.ObdCommand;
import mnefzger.de.sensorplatform.External.Commands.RPMCommand;
import mnefzger.de.sensorplatform.External.Commands.SpeedCommand;
import mnefzger.de.sensorplatform.ISensorCallback;


public class OBD2Provider extends DataProvider{
    ISensorCallback callback;
    private BluetoothDevice obd2Device;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket sock;

    private Activity app;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d("Discovery", device.getName());

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

        if(obd2Device == null) searchForOBD2Device();
    }

    private void searchForOBD2Device() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        app.registerReceiver(mReceiver, filter);

        Log.d("BLUETOOTH", "Looking for paired devices...");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("OBDII")) {
                    obd2Device = device;
                    sock = BluetoothManager.connectPaired(obd2Device);
                    Log.d("BLUETOOTH", sock.isConnected()+"");
                    connectToOBD2Device();
                    break;
                }
            }
        } else {
            Log.d("BLUETOOTH", "Start discovery...");
            btAdapter.startDiscovery();
        }


    }

    private void connectToOBD2Device() {
        try{
            sock = BluetoothManager.connect(obd2Device);
            app.unregisterReceiver(mReceiver);
            Log.d("BLUETOOTH", "Connected to: " + obd2Device.getName() + "-> " + sock.isConnected());

            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

            /* Setup */
            runCommand(sock, "ATD");
            runCommand(sock, "ATZ");
            runCommand(sock, "AT E0");
            runCommand(sock, "AT L0");
            runCommand(sock, "AT S0");
            runCommand(sock, "AT H0");

            SpeedCommand cmd = new SpeedCommand();
            runCommand(sock, cmd);

            RPMCommand rpm_cmd = new RPMCommand();
            runCommand(sock, rpm_cmd);


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

    public void runCommand(BluetoothSocket s, String cmd) {
        if(obd2Device != null) {
            try{
                mRun(cmd, s.getInputStream(), s.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runCommand(BluetoothSocket s, ObdCommand cmd) {
        if(obd2Device != null) {
            try{
                cmd.run(s.getInputStream(), s.getOutputStream());
                Log.d("PIRES", cmd.getFormattedResult());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void mRun(String cmd, InputStream in, OutputStream out) throws IOException,
            InterruptedException {

        Log.d("CMD", cmd);
        out.write((cmd + "\r").getBytes());
        out.flush();
        Thread.sleep(100);
        String raw = readRawData(in);
        //Log.d("RAW", raw);
    }

    protected String readRawData(InputStream in) throws IOException {
        String rawData;
        byte b = 0;
        StringBuilder res = new StringBuilder();

        // read until '>' arrives OR end of stream reached
        char c;
        // -1 if the end of the stream is reached
        while (((b = (byte) in.read()) > -1)) {
            c = (char) b;
            if (c == '>') // read until '>' arrives
            {
                break;
            }
            res.append(c);
        }
        /*
         * Imagine the following response 41 0c 00 0d.
         *
         * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
         * attention to the fact that I've put the word byte in quotes, because 41
         * is actually TWO bytes (two chars) in the socket. So, we must do some more
         * processing..
         */
        rawData = res.toString().replaceAll("SEARCHING", "");

        /*
         * Data may have echo or informative text like "INIT BUS..." or similar.
         * The response ends with two carriage return characters. So we need to take
         * everything from the last carriage return before those two (trimmed above).
         */
        //kills multiline.. rawData = rawData.substring(rawData.lastIndexOf(13) + 1);
        rawData = rawData.replaceAll("\\s", "");//removes all [ \t\n\x0B\f\r]

        return rawData;
    }
}
