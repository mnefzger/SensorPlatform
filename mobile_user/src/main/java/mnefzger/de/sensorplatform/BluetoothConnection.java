package mnefzger.de.sensorplatform;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by matthias on 05/09/16.
 */
public class BluetoothConnection {
    private static final String TAG = "BLUETOOTH_CONNECTION";

    private static final String uuid = "00001101-0000-1000-8000-00805F9B34FB";

    public static boolean connected = false;
    public static BluetoothDevice device = null;
    public static BluetoothSocket socket = null;

    public static BluetoothSocket connect(BluetoothDevice dev) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback = null;

        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            sock = dev.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            sock.connect();
        } catch (Exception e1) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            Class<?> clazz = sock.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                sockFallback.connect();
                sock = sockFallback;
            } catch (Exception e2) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                throw new IOException(e2.getMessage());
            }
        }
        return sock;
    }
}
