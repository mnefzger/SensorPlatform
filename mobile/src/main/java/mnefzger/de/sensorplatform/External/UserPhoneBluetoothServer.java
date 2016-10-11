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

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.Core.IEventCallback;
import mnefzger.de.sensorplatformshared.BluetoothListener;
import mnefzger.de.sensorplatformshared.InteractionEvents;
import mnefzger.de.sensorplatformshared.InteractionObject;

public class UserPhoneBluetoothServer {

    private BluetoothAdapter bAdapter;
    private final String uuid = "00001101-0000-1000-8000-00805F9B34FB";

    private BroadcastReceiver mReceiver;

    private Context c;

    private AcceptThread acceptThread;

    private IEventCallback callback;

    public UserPhoneBluetoothServer(IEventCallback callback, Context c) {
        this.callback = callback;
        this.c = c;
    }

    public void setupServer(Context c) {
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bAdapter.isEnabled())
            bAdapter.enable();

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // 0 = always discoverable
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(discoverableIntent);

        mReceiver = new ConnectionStatusReceiver();
        IntentFilter filter_screen = new IntentFilter();
        filter_screen.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        c.registerReceiver(mReceiver,filter_screen);

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void cancel() {
        if(acceptThread != null)
            acceptThread.cancel();
        try {
            c.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("Bluetooth", "Receiver already unregistered");
        }
    }

    public void stop() {
        if(acceptThread != null) {
            cancel();
            acceptThread.sendStop();
        }
    }

    class ConnectionStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            Log.d("Bluetooth", intent.getAction());
            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
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
        private BluetoothSocket socket;

        AcceptThread() {
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

            socket = null;

            // Listen to the server socket if we're not connected
            while (true) {
                try {
                    c.sendBroadcast(new Intent("PHONE_FOUND"));
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

        private void manageConnections(final BluetoothSocket socket) {
            Log.d("INCOMING CONNECTION", socket.getRemoteDevice().getName());
            c.sendBroadcast(new Intent("PHONE_CONNECTED"));
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

        private void processMessage(String m){
            Log.d("BLUETOOTH_SERVER", "Received: " +  m);
            InteractionObject obj = new Gson().fromJson(m, InteractionObject.class);
            String description = "";
            switch (obj.message) {
                case InteractionEvents.TOUCH:
                    description = obj.message;
                    String application = obj.extra.split("/")[0];
                    application = application.substring(1);
                    description += ", " + application;
                    break;
                case InteractionEvents.NOTIFICATION:
                    description = obj.message;
                    break;
                case InteractionEvents.SCREEN_ON:
                    description = obj.message;
                    break;
                default:
                    break;
            }

            if(!description.equals(""))
                callback.onEventDetected(new EventVector(EventVector.LEVEL.HIGH_RISK, System.currentTimeMillis(), description, 1));
        }

        private void cancel() {
            Log.d("Bluetooth", "cancel()" + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth", "close() of server failed", e);
            }
        }

        private void sendStop() {
            if(socket != null && socket.isConnected())
                sendDataToPairedDevice("mnefzger.de.sensorplatform.STOP");
            else
                Log.d("BLUETOOTH SERVER", "No connection to user phone, cannot call stop");
        }

        public void sendDataToPairedDevice(String message){
            byte[] toSend = message.getBytes();
            try {
                Log.d("OutStream? ", socket + "");
                OutputStream mmOutStream = socket.getOutputStream();
                mmOutStream.write(toSend);
                Log.d("Sent to: ", socket.getRemoteDevice().getName() + ", " + message);
            } catch (IOException e) {
                Log.e("BluetoothSend", "Exception during write", e);
            }

        }
    }
}
