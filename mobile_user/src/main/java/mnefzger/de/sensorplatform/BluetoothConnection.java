package mnefzger.de.sensorplatform;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by matthias on 05/09/16.
 */
public class BluetoothConnection {
    public static boolean connected = false;
    public static BluetoothDevice device = null;
    public static BluetoothSocket socket = null;
}
