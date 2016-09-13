package mnefzger.de.sensorplatform.UI;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.text.DecimalFormat;

import mnefzger.de.sensorplatform.Core.ActiveSubscriptions;
import mnefzger.de.sensorplatform.Core.DataVector;
import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.Core.SensorPlatformService;


public class AppFragment extends Fragment {

    RelativeLayout dataLayout;
    TextView waitingText;

    TextView accX;
    TextView accY;
    TextView accZ;

    TextView rotX;
    TextView rotY;
    TextView rotZ;

    TextView lat;
    TextView lon;
    TextView speed;

    TextView light;

    TextView street;
    TextView event;
    TextView face;

    TextView obdSpeed;
    TextView obdRPM;
    TextView obdFuel;

    TextView heart;

    Button stopButton;
    Button pauseButton;
    Button resumeButton;

    DecimalFormat df = new DecimalFormat("#.####");

    boolean rawRegistered = false;
    boolean eventRegistered = false;

    public AppFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }


    private void registerReceivers() {
        IntentFilter f = new IntentFilter("mnefzger.de.sensorplatform.RawData");
        rawRegistered = true;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(rawReceiver, f);

        IntentFilter f2 = new IntentFilter("mnefzger.de.sensorplatform.EventData");
        eventRegistered = true;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(eventReceiver, f2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (container != null) {
            container.removeAllViews();
        }

        View v = inflater.inflate(R.layout.fragment_app, container, false);

        dataLayout = (RelativeLayout) v.findViewById(R.id.dataLayout);
        waitingText = (TextView) v.findViewById(R.id.waiting_text);

        accX = (TextView) v.findViewById(R.id.accXText);
        accY = (TextView) v.findViewById(R.id.accYText);
        accZ = (TextView) v.findViewById(R.id.accZText);

        rotX = (TextView) v.findViewById(R.id.rotXText);
        rotY = (TextView) v.findViewById(R.id.rotYText);
        rotZ = (TextView) v.findViewById(R.id.rotZText);

        lat = (TextView) v.findViewById(R.id.latText);
        lon = (TextView) v.findViewById(R.id.lonText);
        speed = (TextView) v.findViewById(R.id.speedText);
        light = (TextView) v.findViewById(R.id.lightText);

        street = (TextView) v.findViewById(R.id.osmText);
        event = (TextView) v.findViewById(R.id.eventText);
        face = (TextView) v.findViewById(R.id.faceText);

        obdRPM = (TextView) v.findViewById(R.id.obdRPMText);
        obdSpeed = (TextView) v.findViewById(R.id.obdSpeedText);
        obdFuel = (TextView) v.findViewById(R.id.obdFuelText);

        heart = (TextView) v.findViewById(R.id.heartText);

        stopButton = (Button) v.findViewById(R.id.stopButton);
        pauseButton = (Button) v.findViewById(R.id.pauseButton);
        resumeButton = (Button) v.findViewById(R.id.resumeButton);

        stopButton.setOnClickListener(stopListener);
        pauseButton.setOnClickListener(pauseListener);
        resumeButton.setOnClickListener(resumeListener);

        return v;
    }

    View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent stopIntent = new Intent(getActivity(), SensorPlatformService.class);
            stopIntent.setAction("SERVICE_STOP");

            getActivity().startService(stopIntent);
            resumeButton.setEnabled(false);
            pauseButton.setEnabled(false);

            MainActivity app = (MainActivity) getActivity();
            app.started = false;
            app.mBound = false;
            app.goToStartFragment(25);

        }
    };

    View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent pauseIntent = new Intent(getActivity(), SensorPlatformService.class);
            pauseIntent.setAction("SERVICE_PAUSE");

            getActivity().startService(pauseIntent);
            resumeButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }
    };

    View.OnClickListener resumeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent resumeIntent = new Intent(getActivity(), SensorPlatformService.class);
            resumeIntent.setAction("SERVICE_RESUME");

            getActivity().startService(resumeIntent);
            resumeButton.setEnabled(false);
            pauseButton.setEnabled(true);
        }
    };

    public void updateUI(DataVector vector) {
        final DataVector v = vector;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accX.setText("AccX: " + df.format(v.accX) );
                accY.setText("AccY: " + df.format(v.accY) );
                accZ.setText("AccZ: " + df.format(v.accZ) );

                rotX.setText("RotX: " + v.rotX);
                rotY.setText("RotY: " + v.rotY);
                rotZ.setText("RotZ: " + v.rotZ);

                light.setText("Light: " + df.format(v.light) + " lumen");

                if(v.lat == 0 && ActiveSubscriptions.usingGPS()) {
                    lat.setText("Lat: Acquiring position…");
                    lon.setText("Lon: Acquiring position…");
                    speed.setText("Speed: Acquiring position…");
                }
                if(v.lat != 0 && ActiveSubscriptions.usingGPS()) {
                    lat.setText("Lat: " + df.format( v.lat ));
                    lon.setText("Lon: " + df.format( v.lon ));
                    speed.setText("Speed: " + df.format(v.speed) + " km/h");
                }

                obdRPM.setText("OBD RPM: " + v.rpm);
                obdSpeed.setText("OBD Speed: " + v.obdSpeed + " km/h");
                obdFuel.setText("OBD Fuel: " + v.fuel + " l/100km");

                heart.setText("Heart Rate: " + v.heartRate + "bpm");
            }
        });

    }

    public void updateUI(EventVector vector) {
        final EventVector v = vector;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (v.getEventDescription().contains("ROAD"))
                    street.setText(v.getEventDescription());
                else if (v.getEventDescription().equals("Face detected"))
                    face.setText("Face detected: YES");
                else if (v.getEventDescription().equals("No Face detected"))
                    face.setText("Face detected: NO");
                else if (v.getEventDescription().equals("Trip Start Detected")) {
                    waitingText.setVisibility(View.INVISIBLE);
                    dataLayout.setVisibility(View.VISIBLE);
                } else
                    event.setText("Last event: " + v.getEventDescription() + ", " + df.format(v.getValue()) );
            }
        });
    }

    BroadcastReceiver rawReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            DataVector v = new Gson().fromJson(b.getString("RawData"), DataVector.class);

            Log.d("RawData @ App  ", v.toString());

            if(dataLayout.getVisibility() == View.INVISIBLE) {
                waitingText.setVisibility(View.INVISIBLE);
                dataLayout.setVisibility(View.VISIBLE);
            }

            if(getActivity() != null)
                updateUI(v);
        }
    };

    BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            EventVector v = new Gson().fromJson(b.getString("EventData"), EventVector.class);

            Log.d("EventData @ App  ", v.toString());

            if(dataLayout.getVisibility() == View.INVISIBLE) {
                waitingText.setVisibility(View.INVISIBLE);
                dataLayout.setVisibility(View.VISIBLE);
            }

            if(getActivity() != null)
                updateUI(v);
        }
    };


    @Override
    public void onPause() {
        super.onPause();

        if(rawRegistered) {
            try {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(rawReceiver);
                rawRegistered = false;
            } catch (IllegalArgumentException e) {
                Log.e("APP FRAGMENT", e.toString());
            }

        }

        if(eventRegistered) {
            try {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(eventReceiver);
                eventRegistered = false;
            } catch (IllegalArgumentException e) {
                Log.e("APP FRAGMENT", e.toString());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!rawRegistered || !eventRegistered)
            registerReceivers();
    }


}
