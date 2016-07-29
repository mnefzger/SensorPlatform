package mnefzger.de.sensorplatform.External;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class OBDConnection {
    public static BluetoothSocket sock;
    public static BluetoothDevice obd2Device;
    public static boolean connected = false;
}
