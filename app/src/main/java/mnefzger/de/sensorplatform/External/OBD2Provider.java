package mnefzger.de.sensorplatform.External;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
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
    private Context app;

    private final String TAG = "OBD_BLUETOOTH";


    public OBD2Provider(Context app, ISensorCallback callback) {
        BluetoothManager.verifyBluetoothPermissions(app);
        this.callback = callback;
        this.app = app;

        prefs = PreferenceManager.getDefaultSharedPreferences(app);
        OBD_DELAY = Preferences.getOBDDelay(prefs);

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
        OBD2Connection.sock = null;
        setupComplete = false;
        OBD2Connection.connector = new OBD2Connector(app);
    }

    private void setup() {
        setupRunning = true;
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        /* Setup */
        runCommand(OBD2Connection.sock, "ATD");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "AT E0");
        runCommand(OBD2Connection.sock, "AT L0");
        runCommand(OBD2Connection.sock, "AT S0");
        runCommand(OBD2Connection.sock, "AT H0");

        SpeedCommand cmd = new SpeedCommand();
        runCommand(OBD2Connection.sock, cmd);

        RPMCommand rpm_cmd = new RPMCommand();
        runCommand(OBD2Connection.sock, rpm_cmd);

        setupComplete = true;
        setupRunning = false;
    }

    private void collectOBDData() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(OBD2Connection.sock != null && OBD2Connection.sock.isConnected()) {
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
        runCommand(OBD2Connection.sock, cmd);

        RPMCommand rpm_cmd = new RPMCommand();
        runCommand(OBD2Connection.sock, rpm_cmd);

        try { Thread.sleep(25); } catch (InterruptedException e) { e.printStackTrace(); }

        double speed = Double.valueOf( cmd.getCalculatedResult() );
        double rpm = Double.valueOf( rpm_cmd.getCalculatedResult() );

        double[] response = {speed, rpm};

        callback.onOBD2Data(response);
    }

    public void runCommand(BluetoothSocket s, String cmd) {
        if(OBD2Connection.obd2Device != null) {
            try{
                mRun(cmd, s.getInputStream(), s.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runCommand(BluetoothSocket s, ObdCommand cmd) {
        if(OBD2Connection.obd2Device != null) {
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
