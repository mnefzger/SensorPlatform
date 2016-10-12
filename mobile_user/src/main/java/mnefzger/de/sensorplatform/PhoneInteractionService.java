package mnefzger.de.sensorplatform;


import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import mnefzger.de.sensorplatformshared.BluetoothListener;
import mnefzger.de.sensorplatformshared.InteractionEvents;
import mnefzger.de.sensorplatformshared.InteractionObject;
import mnefzger.de.sensorplatformshared.UStats;


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

        if(socket != null)
            setupListener(socket);
    }

    private void setupListener(BluetoothSocket socket) {
        Log.d(TAG, "Setup Listener");
        BluetoothListener listener = new BluetoothListener();
        listener.listen(socket, handler);
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
            String readMessage = new String(readBuf, 0, msg.arg1);
            Log.d(TAG, readMessage);
            if(readMessage.equals("mnefzger.de.sensorplatform.STOP")) {
                Log.d(TAG, "Stop received, shut down");
                sendBroadcast(new Intent(readMessage));
                stopSelf();
            }
        }
    };

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
        String foregroundTaskAppName = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            foregroundTaskAppName = UStats.getForegroundApp(getApplicationContext());
        } else {
            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
            String app = recentTasks.get(0).topActivity.getPackageName();
            PackageManager pm = PhoneInteractionService.this.getPackageManager();
            try {
                PackageInfo foregroundAppPackageInfo = pm.getPackageInfo(app, 0);
                foregroundTaskAppName = foregroundAppPackageInfo.applicationInfo.loadLabel(pm).toString();
            } catch (Exception e) {

            }

        }

        Log.d(TAG, "Touched! " + foregroundTaskAppName);
        sendDataToPairedDevice(InteractionEvents.TOUCH, foregroundTaskAppName);
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

            sendDataToPairedDevice(InteractionEvents.NOTIFICATION, null);
        }
    }

    class ScreenUnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, Intent.ACTION_SCREEN_ON);
                sendDataToPairedDevice(InteractionEvents.SCREEN_ON, null);
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

    public void sendDataToPairedDevice(String message, String extra){
        if( socket == null && BluetoothConnection.socket != null ) {
            Log.d("BluetoothSend", "First time assignment");
            socket = BluetoothConnection.socket;
            deviceAddress = socket.getRemoteDevice().getAddress();
            setupListener(socket);
        }  else if( !BluetoothConnection.connected && deviceAddress != null ) {
            Log.d("BluetoothSend", "Socket dead, reconnect.");
            reconnect();
            return;
        }   else if(socket == null || !BluetoothConnection.connected ){
            Log.d("BluetoothSend", "Socket still dead.");
            return;
        }

        InteractionObject obj = new InteractionObject(message, extra);
        String json = new Gson().toJson(obj);

        byte[] toSend = json.getBytes();
        try {
            Log.d("OutStream? ", socket + "");
            OutputStream mmOutStream = socket.getOutputStream();
            mmOutStream.write(toSend);
            Log.d("Sent to: ", socket.getRemoteDevice().getName() + ", " + message);
        } catch (IOException e) {
            Log.e("BluetoothSend", "Exception during write", e);
            if(e.getMessage().contains("Broken pipe")) {
                if(deviceAddress != null)
                    reconnect();
            }
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
            setupListener(socket);
        } catch (IOException e) {
            Log.d(TAG, "Could not reconnect. " + e);
        }
    }
}
