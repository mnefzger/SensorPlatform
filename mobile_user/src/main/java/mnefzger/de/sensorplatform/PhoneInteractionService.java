package mnefzger.de.sensorplatform;


import android.app.Service;
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
    private final String TAG = "USER_INTERACTION_SVC";
    private LinearLayout touchLayout;

    private static BluetoothSocket socket = null;

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

    public static void sendDataToPairedDevice(String message){
        Log.d("BluetoothSend", socket + ", " + BluetoothConnection.connected);
        if(!BluetoothConnection.connected && socket == null)
            return;
        else if(socket == null)
            socket = BluetoothConnection.socket;

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
