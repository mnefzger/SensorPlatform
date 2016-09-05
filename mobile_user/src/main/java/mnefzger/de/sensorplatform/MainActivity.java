package mnefzger.de.sensorplatform;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter bAdapter;
    private ListView deviceList;
    private DeviceListAdapter listAdapter;
    private TextView successText;

    private static final String TAG = "USER_PHONE_MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        deviceList = (ListView) findViewById(R.id.deviceListView);
        listAdapter = new DeviceListAdapter(this);
        deviceList.setAdapter(listAdapter);
        successText = (TextView) findViewById(R.id.successText);

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BluetoothDevice device = listAdapter.getItem(i);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectTo(device);
                    }
                }).start();

            }
        });

        verifyPermissions(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent overlay_p=new Intent("android.settings.ACTION_MANAGE_OVERLAY_PERMISSION");
            startActivityForResult(overlay_p,1337);
        } else {
            Intent notifications_p=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivityForResult(notifications_p, 1338);
            setup();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1337) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Intent notifications_p=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivityForResult(notifications_p, 1338);
            }
        } else if(requestCode == 1338) {
            setup();
        }
    }

    private void setup() {
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        startDiscovery();

        Intent intent = new Intent(this, PhoneInteractionService.class);
        startService(intent);
    }

    private void startDiscovery() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(mReceiver, filter);
        bAdapter.startDiscovery();
        Log.d(TAG, "Discovery started");
    }

    private void connectTo(BluetoothDevice device) {
        try{
            BluetoothConnection.socket = connect(device);
            BluetoothConnection.connected = true;
            BluetoothConnection.device = device;
            Log.d(TAG, "Connected to: " + BluetoothConnection.device.getName() + "-> " + BluetoothConnection.socket.isConnected());

            runOnUiThread(new Runnable() {
                public void run() {
                    deviceList.setVisibility(View.INVISIBLE);
                    successText.setText("Connected to: " + BluetoothConnection.device.getName() + ".");
                    successText.setVisibility(View.VISIBLE);
                }
            });

            /*Intent intent = new Intent(this, PhoneInteractionService.class);
            startService(intent);*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d(TAG, "Found: "  + device.getName() + "");

                if (device != null) {
                    device.fetchUuidsWithSdp();
                    if (!listAdapter.isAlreadyDiscovered(device)) {
                        listAdapter.add(device);
                        listAdapter.addToDeviceList(device);
                        listAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    public static BluetoothSocket connect(BluetoothDevice dev) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback = null;

        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            sock = dev.createRfcommSocketToServiceRecord(UUID.randomUUID());
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

    private static final int REQUEST_LOCATION = 1;
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final int REQUEST_ALERT = 2;
    private static String[] PERMISSIONS_ALERT = {
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     * @param context
     */
    private static void verifyPermissions(Context context) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions((Activity)context,
                    PERMISSIONS_LOCATION, REQUEST_LOCATION);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

}
