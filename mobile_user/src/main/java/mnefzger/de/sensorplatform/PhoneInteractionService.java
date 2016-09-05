package mnefzger.de.sensorplatform;


import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class PhoneInteractionService extends Service {
    NotificationReceiver nReceiver;
    ScreenUnlockReceiver sReceiver;
    private final String TAG = "USER_INTERACTION_SVC";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("mnefzger.de.sensorplatform.NOTIFICATION");
        registerReceiver(nReceiver,filter);

        sReceiver = new ScreenUnlockReceiver();
        IntentFilter filter_screen = new IntentFilter();
        filter_screen.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(sReceiver,filter_screen);

        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(nReceiver);
        unregisterReceiver(sReceiver);
        super.onDestroy();
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event");
            Log.d(TAG, temp);

            sendDataToPairedDevice(temp);
        }
    }

    class ScreenUnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, Intent.ACTION_SCREEN_ON);
            }
        }
    }

    public static void sendDataToPairedDevice(String message){
        byte[] toSend = message.getBytes();
        BluetoothSocket socket = BluetoothConnection.socket;
        try {
            OutputStream mmOutStream = socket.getOutputStream();
            mmOutStream.write(toSend);
            Log.d("Sent to: ", socket.getRemoteDevice().getName() + ", " + message);
        } catch (IOException e) {
            Log.e("BluetoothSend", "Exception during write", e);
        }


    }
}
