package mnefzger.de.sensorplatform.External;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothListener {
    private final String TAG = "BLUETOOTH_LISTENER";

    Thread messageListener;
    private boolean listening = true;

    private Handler mHandler;

    public BluetoothListener(){

    }

    public void listen(BluetoothSocket socket, Handler handler){
        mHandler = handler;
        BluetoothSocketListener bsl = new BluetoothSocketListener(socket);
        messageListener = new Thread(bsl);
        messageListener.start();
    }

    private class BluetoothSocketListener extends Thread {

        private BluetoothSocket socket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BluetoothSocketListener(BluetoothSocket socket) {
            Log.d(TAG, "started listening to " + socket.getRemoteDevice().getName());
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            this.socket = socket;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            while(listening) {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                try {
                    int bytesRead = -1;
                    while (true) {
                        bytesRead = mmInStream.read(buffer);
                        Log.d(TAG, bytesRead + "");
                        mHandler.obtainMessage(0, bytesRead, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    destroyListener();
                    Log.d(TAG, e.getMessage());
                }
            }
        }
    }



    public void destroyListener(){
        listening = false;
        try{
            messageListener.join();
        } catch (Exception e){
            Log.d(TAG, "Destroy failed: " + e.toString());
        }
    }

}