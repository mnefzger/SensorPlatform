package mnefzger.de.sensorplatform.External;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import mnefzger.de.sensorplatform.DataProvider;
import mnefzger.de.sensorplatform.ISensorCallback;
import mnefzger.de.sensorplatform.Preferences;


public class OBD2Provider extends DataProvider{
    SharedPreferences prefs;
    ISensorCallback callback;
    private int OBD_DELAY;

    private boolean setupComplete = false;
    private boolean setupRunning = false;
    private Activity app;

    private final String TAG = "OBD_BLUETOOTH";


    public OBD2Provider(Activity app, ISensorCallback callback) {
        BluetoothManager.verifyBluetoothPermissions(app);
        this.callback = callback;
        this.app = app;

        prefs = PreferenceManager.getDefaultSharedPreferences(app);
        OBD_DELAY = Preferences.getOBDDelay(prefs);

        Log.d(TAG, "init");
    }


    public void start() {
        super.start();

        collectOBDData();
    }

    public void stop() {
        super.stop();
    }

    // Is called in case of IOException with broken pipe
    public void reset() {
        Log.d(TAG, "reset");
        OBDConnection.sock = null;
        setupComplete = false;
        new OBDConnector(app);
    }

    private void setup() {
        setupRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
                /* Setup */
                runCommand(OBDConnection.sock, "ATD");
                runCommand(OBDConnection.sock, "ATZ");
                runCommand(OBDConnection.sock, "ATZ");
                runCommand(OBDConnection.sock, "ATZ");
                runCommand(OBDConnection.sock, "ATZ");
                runCommand(OBDConnection.sock, "AT E0");
                runCommand(OBDConnection.sock, "AT L0");
                runCommand(OBDConnection.sock, "AT S0");
                runCommand(OBDConnection.sock, "AT H0");

                SpeedCommand cmd = new SpeedCommand();
                runCommand(OBDConnection.sock, cmd);

                RPMCommand rpm_cmd = new RPMCommand();
                runCommand(OBDConnection.sock, rpm_cmd);

                setupComplete = true;
                setupRunning = false;
            }
        }).start();
    }

    private void collectOBDData() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(OBDConnection.sock != null && OBDConnection.sock.isConnected()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(setupComplete)
                                requestData();
                            else {
                                if(!setupRunning) setup();
                            }
                        }
                    }).start();
                }

                collectOBDData();
            }
        }, OBD_DELAY);
    }

    private void requestData() {
        SpeedCommand cmd = new SpeedCommand();
        runCommand(OBDConnection.sock, cmd);

        RPMCommand rpm_cmd = new RPMCommand();
        runCommand(OBDConnection.sock, rpm_cmd);

        try { Thread.sleep(25); } catch (InterruptedException e) { e.printStackTrace(); }

        double speed = Double.valueOf( cmd.getCalculatedResult() );
        double rpm = Double.valueOf( rpm_cmd.getCalculatedResult() );

        double[] response = {speed, rpm};

        callback.onOBD2Data(response);
    }

    public void runCommand(BluetoothSocket s, String cmd) {
        if(OBDConnection.obd2Device != null) {
            try{
                mRun(cmd, s.getInputStream(), s.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runCommand(BluetoothSocket s, ObdCommand cmd) {
        if(OBDConnection.obd2Device != null) {
            try{
                cmd.run(s.getInputStream(), s.getOutputStream());
            } catch (IOException io) {
                if(io.getMessage().contains("Broken pipe")) {
                    reset();
                }
            } catch(Exception e) {
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
