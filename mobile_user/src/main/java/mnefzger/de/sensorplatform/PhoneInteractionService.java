package mnefzger.de.sensorplatform;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.OutputStream;


public class PhoneInteractionService extends Service implements View.OnTouchListener {
    NotificationReceiver nReceiver;
    ScreenUnlockReceiver sReceiver;
    ConnectionStatusReceiver cReceiver;
    private final String TAG = "USER_INTERACTION_SVC";
    private LinearLayout touchLayout;

    private static BluetoothSocket socket = null;
    private static String deviceAddress = null;

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

        cReceiver = new ConnectionStatusReceiver();
        IntentFilter filter_bt = new IntentFilter();
        filter_bt.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(cReceiver,filter_bt);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if(Settings.canDrawOverlays(getApplicationContext()) == true) {
                addSpyPixel();
            }

        } else {
            addSpyPixel();
        }

        socket = BluetoothConnection.socket;
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
        unregisterReceiver(cReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.d(TAG, "Touched!");
        sendDataToPairedDevice(InteractionEvents.TOUCH);
        return false;
    }

    private void addSpyPixel() {
        touchLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(1,1);

        touchLayout.setLayoutParams(lp);
        touchLayout.setOnTouchListener(this);
        touchLayout.setBackgroundColor(1);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                1,1,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.START;
        wm.addView(touchLayout, mParams);
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event");
            Log.d(TAG, temp);

            sendDataToPairedDevice(InteractionEvents.NOTIFICATION);
        }
    }

    class ScreenUnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, Intent.ACTION_SCREEN_ON);
                sendDataToPairedDevice(InteractionEvents.SCREEN_ON);
            }
        }
    }

    class ConnectionStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            Log.d("Bluetooth", intent.getAction());
            if (intent.getAction().equals(device.ACTION_ACL_DISCONNECTED)) {
                Log.d("Bluetooth", "Disconnect detected, start reconnect..." + deviceAddress);
                BluetoothConnection.socket = null;
                BluetoothConnection.connected = false;
                BluetoothConnection.device = null;
                if(deviceAddress != null)
                    reconnect();
            }
        }
    }

    public void sendDataToPairedDevice(String message){
        if(socket == null && BluetoothConnection.socket != null) {
            Log.d("BluetoothSend", "First time assignment");
            socket = BluetoothConnection.socket;
            deviceAddress = socket.getRemoteDevice().getAddress();
        }  else if(BluetoothConnection.connected == false && deviceAddress != null) {
            Log.d("BluetoothSend", "Socket dead, reconnect.");
            reconnect();
            return;
        }   else if(socket == null || BluetoothConnection.connected == false){
            Log.d("BluetoothSend", "Socket still dead.");
            return;
        }

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

    private void reconnect() {
        Log.d(TAG, "Trying to reconnect....");
        BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice d = bAdapter.getRemoteDevice(deviceAddress);
        try{
            BluetoothConnection.socket = BluetoothConnection.connect(d);
            Log.d(TAG, "Reconnected!");
            BluetoothConnection.connected = true;
            BluetoothConnection.device = d;
            socket = BluetoothConnection.socket;
        } catch (IOException e) {
            Log.d(TAG, "Could not reconnect. " + e);
        }
    }
}
