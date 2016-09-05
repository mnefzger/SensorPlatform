package mnefzger.de.sensorplatform.External;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by matthias on 05/09/16.
 */
public class UserPhoneBluetoothServer {

    private BluetoothAdapter bAdapter;
    private final String uuid = "00001101-0000-1000-8000-00805F9B34FB";

    private BroadcastReceiver mReceiver;

    public UserPhoneBluetoothServer(Context c) {
        setupServer(c);
    }

    private void setupServer(Context c) {
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // 0 = always discoverable
        c.startActivity(discoverableIntent);

        mReceiver = new ConnectionStatusReceiver();
        IntentFilter filter_screen = new IntentFilter();
        filter_screen.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        c.registerReceiver(mReceiver,filter_screen);

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    class ConnectionStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            Log.d("Bluetooth", intent.getAction());
            if (intent.getAction().equals(device.ACTION_ACL_DISCONNECTED)) {
                Log.d("Bluetooth", "Disconnect detected, start new accept thread.");
                AcceptThread acceptThread = new AcceptThread();
                acceptThread.start();
            }
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            Log.d("Bluetooth", "Started Thread");
            // Create a new listening server socket
            try {
                tmp = bAdapter.listenUsingRfcommWithServiceRecord("SensorPlatform", UUID.fromString(uuid));
            } catch (IOException e) {
                Log.e("Bluetooth", "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("Bluetooth", "accept() failed", e);
                    break;
                }

                if(socket != null) {
                    manageConnections(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e("Bluetooth", "close() failed", e);
                    }
                    break;
                }
            }

        }

        public void manageConnections(final BluetoothSocket socket) {
            Log.d("INCOMING CONNECTION", socket.getRemoteDevice().getName());
            BluetoothListener listener = new BluetoothListener();
            listener.listen(socket, handler);
        }

        private final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                processMessage(readMessage);
            }
        };

        public void processMessage(String m){
            Log.d("BLUETOOTH_SERVER", "Received: " +  m);
        }

        public void cancel() {
            Log.d("Bluetooth", "cancel()" + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "close() of server failed", e);
            }
        }
    }
}
