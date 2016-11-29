package au.carrsq.sensorplatform.External;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.CloseCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.HeadersOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import au.carrsq.sensorplatform.Core.DataProvider;
import au.carrsq.sensorplatform.Core.ISensorCallback;
import au.carrsq.sensorplatform.Core.Preferences;
import au.carrsq.sensorplatform.R;


public class OBD2Provider extends DataProvider implements OBD2Connector.IConnectionEstablished{
    private SharedPreferences setting_prefs;
    private SharedPreferences sensor_prefs;
    private ISensorCallback callback;
    private int OBD_DELAY;
    private OBD2Connector connector;

    private boolean setupComplete = false;
    private boolean setupRunning = false;
    private boolean collecting = false;
    private Context app;

    private boolean tryingToReconnect = false;
    private int outOfBoundsCounter = 0;

    private final String TAG = "OBD_BLUETOOTH";

    public OBD2Provider(Context app, ISensorCallback callback) {
        this.callback = callback;
        this.app = app;

        setting_prefs = app.getSharedPreferences(app.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = app.getSharedPreferences(app.getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
    }

    public void connect() {
        if(Preferences.OBDActivated(sensor_prefs) && !OBD2Connection.connected) {
            reset();
        } else {
            app.sendBroadcast(new Intent("OBD_FOUND"));
            app.sendBroadcast(new Intent("OBD_CONNECTED"));
            app.sendBroadcast(new Intent("OBD_SETUP_COMPLETE"));
        }
    }

    @Override
    public void start() {
        OBD_DELAY = Preferences.getOBDDelay(setting_prefs);
        collecting = true;
        collectOBDData();
    }

    @Override
    public void stop() {
        if(OBD2Connection.sock != null) {
            try {
                OBD2Connection.sock.getInputStream().close();
                OBD2Connection.sock.getOutputStream().close();
                OBD2Connection.sock.close();
                OBD2Connection.sock = null;
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(connector != null) {
            connector.stopTimeout();
        }

        collecting = false;
        OBD2Connection.connected = false;
        setupComplete = false;
    }

    /*
     * Resets any existing connection and sets up a new connection to the OBD-II device.
     * Is called for initial setup and in case of an IOException with a broken pipe
     */
    public void reset() {
        Log.d(TAG, "reset");

        // set up broadcast receiver
        IntentFilter f = new IntentFilter();
        f.addAction("OBD_CONNECTED");
        f.addAction("OBD_NOT_FOUND");
        app.registerReceiver(mReceiver,f);

        tryingToReconnect = true;

        OBD2Connection.sock = null;
        OBD2Connection.connected = false;
        setupComplete = false;

        connector = new OBD2Connector(app, this);
    }

    @Override
    public void onConnectionEstablished() {
        setup();
    }

    /**
     * Runs the commands that set up the OBDII settings
     * https://github.com/pires/obd-java-api
     */
    private void setup() {
        setupRunning = true;

        /* Setup */

        ObdRawCommand a = new ObdRawCommand("ATZ");
        runCommand(OBD2Connection.sock, a);
        Log.d("SETUP", "ATZ: " + a.getResult());

        ObdRawCommand b = new ObdRawCommand("ATZ");
        runCommand(OBD2Connection.sock, b);
        Log.d("SETUP", "ATZ: " + b.getResult());

        ObdRawCommand c = new ObdRawCommand("ATD");
        runCommand(OBD2Connection.sock, c);
        Log.d("SETUP", "ATD: " + c.getResult());

        ObdRawCommand d = new ObdRawCommand("ATH0");
        runCommand(OBD2Connection.sock, d);
        Log.d("SETUP", "ATH0: " + a.getResult());

        ObdRawCommand e = new ObdRawCommand("ATE0");
        runCommand(OBD2Connection.sock, e);
        Log.d("SETUP", "Air Temp: " + e.getResult());

        ObdRawCommand f = new ObdRawCommand("ATST FF");
        runCommand(OBD2Connection.sock, f);
        Log.d("SETUP", "ATST FF: " + f.getResult());

        ObdRawCommand g = new ObdRawCommand("ATSP6");
        runCommand(OBD2Connection.sock, g);
        Log.d("SETUP", "ATSP6: " + g.getResult());

        AvailablePidsCommand h = new AvailablePidsCommand_01_20();
        runCommand(OBD2Connection.sock, h);
        Log.d("SETUP", "ATST FF: " + h.getResult());

        AvailablePidsCommand i = new AvailablePidsCommand_01_20();
        runCommand(OBD2Connection.sock, i);
        Log.d("SETUP", "ATST FF: " + i.getResult());

        ObdRawCommand j = new ObdRawCommand("ATDPN");
        runCommand(OBD2Connection.sock, j);
        Log.d("SETUP", "ATDPN:  " + j.getResult());

        ObdRawCommand k = new ObdRawCommand("ATDP");
        runCommand(OBD2Connection.sock, k);
        Log.d("SETUP", "ATDP: " + k.getResult());

        ObdRawCommand l = new ObdRawCommand("03");
        runCommand(OBD2Connection.sock, l);
        Log.d("SETUP", "03: " + l.getResult());

        ObdRawCommand m = new ObdRawCommand("07");
        runCommand(OBD2Connection.sock, m);
        Log.d("SETUP", "07: " + m.getResult());

        ObdRawCommand n = new ObdRawCommand("0100");
        runCommand(OBD2Connection.sock, n);
        Log.d("SETUP", "0100: " + n.getResult());

        ObdRawCommand o = new ObdRawCommand("0900");
        runCommand(OBD2Connection.sock, o);
        Log.d("SETUP", "0900: " + o.getResult());



        /**
         * This setup procedure is working but not as nice to read
         */
        /*runCommand(OBD2Connection.sock, "ATD");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "ATZ");
        runCommand(OBD2Connection.sock, "AT E0");
        runCommand(OBD2Connection.sock, "AT L0");
        runCommand(OBD2Connection.sock, "AT S0");
        runCommand(OBD2Connection.sock, "AT H0");*/

        app.sendBroadcast(new Intent("OBD_SETUP_COMPLETE"));

        setupComplete = true;
        setupRunning = false;
    }

    private void collectOBDData() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(OBD2Connection.sock != null && OBD2Connection.sock.isConnected()) {  // connection is established, run commands
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(setupComplete)
                                requestData();
                            else {
                                if(!setupRunning)
                                    setup();
                            }
                        }
                    }).start();

                } else if(collecting && !tryingToReconnect) {     // connection is lost, try reconnecting...
                    // send -1 as indicator that no real data is coming through
                    double[] resp = {-1,-1,-1};
                    callback.onOBD2Data(resp);

                    // reset connection
                    Log.d("OBD", "Connection lost, reconnecting.");
                    reset();
                }

                // if data collection is still running, call again
                if(collecting)
                    collectOBDData();
            }
        }, OBD_DELAY);
    }

    /*
     * Run the commands and report values back to callback.
     * Speed, Engine Speed, Fuel consumption
     */
    private void requestData() {
        SpeedCommand sp_cmd = new SpeedCommand();
        runCommand(OBD2Connection.sock, sp_cmd);

        RPMCommand rpm_cmd = new RPMCommand();
        runCommand(OBD2Connection.sock, rpm_cmd);

        // not working with Honda, Toyota
        //ConsumptionRateCommand cr_cmd = new ConsumptionRateCommand();
        //runCommand(OBD2Connection.sock, cr_cmd);

        try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }

        double speed = Double.valueOf( sp_cmd.getCalculatedResult() );
        double rpm = Double.valueOf( rpm_cmd.getCalculatedResult() );
        //double cr = Double.valueOf( cr_cmd.getCalculatedResult() );

        double[] response = {speed, rpm, -1};

        callback.onOBD2Data(response);
    }

    public void runCommand(BluetoothSocket s, String cmd) {
        if(OBD2Connection.obd2Device != null && s != null) {
            try{
                mRun(cmd, s.getInputStream(), s.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runCommand(BluetoothSocket s, ObdCommand cmd) {
        if(OBD2Connection.obd2Device != null) {
            try {
                //Log.d("CMD", cmd.getCommandPID());
                cmd.run(s.getInputStream(), s.getOutputStream());

            } catch (IOException io) {
                if (io.getMessage().contains("Broken pipe")) {
                    if (!tryingToReconnect) {
                        Log.d("OBD", "Broken pipe, reconnecting.");
                        double[] resp = {-1,-1,-1};
                        callback.onOBD2Data(resp);
                        reset();
                    }
                }

            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                outOfBoundsCounter += 1;
                if (!tryingToReconnect && outOfBoundsCounter >= 3) {
                    Log.d("OBD", "Out of bounds, reconnecting.");
                    double[] resp = {-1,-1,-1};
                    callback.onOBD2Data(resp);
                    outOfBoundsCounter = 0;
                    CloseCommand close = new CloseCommand();
                    runCommand(OBD2Connection.sock, close);
                    AvailablePidsCommand_01_20 pIDs = new AvailablePidsCommand_01_20();
                    runCommand(OBD2Connection.sock, pIDs);
                    //reset();
                }

            } catch(Exception e) {
                if (e.getMessage().contains("STOPPED")) {
                    ObdResetCommand reset = new ObdResetCommand();
                    runCommand(OBD2Connection.sock, reset);
                    try{
                        Thread.sleep(500);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                //e.printStackTrace();
            }
        }
    }

    /**
     * Run the command manually, not with the obd-java-api
     */
    public void mRun(String cmd, InputStream in, OutputStream out) throws IOException,
            InterruptedException {
        Log.d("CMD", cmd);
        out.write((cmd + "\r").getBytes());
        out.flush();
        Thread.sleep(50);
        String raw = readRawData(in);
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

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals("OBD_CONNECTED") ) {
                tryingToReconnect = false;
                app.unregisterReceiver(mReceiver);
            }

            if(action.equals("OBD_NOT_FOUND")) {
                if(!collecting)
                    app.unregisterReceiver(mReceiver);
                if(collecting)
                    reset();
            }
        }

    };


}
