package mnefzger.de.sensorplatform.External;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * This class is the static storage container for information on the connection.
 * It is necessary to prevent data/connection loss on orientation change
 */
public class OBD2Connection {
    public static BluetoothSocket sock;
    public static BluetoothDevice obd2Device;
    public static boolean connected = false;
}
