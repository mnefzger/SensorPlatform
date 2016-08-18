package mnefzger.de.sensorplatform.UI;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DecimalFormat;

import mnefzger.de.sensorplatform.ActiveSubscriptions;
import mnefzger.de.sensorplatform.DataVector;
import mnefzger.de.sensorplatform.EventVector;
import mnefzger.de.sensorplatform.MainActivity;
import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.SensorModule;
import mnefzger.de.sensorplatform.SensorPlatformService;


public class AppFragment extends Fragment {

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

    Activity main;

    DecimalFormat df = new DecimalFormat("#.####");

    public AppFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        main = getActivity();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (container != null) {
            container.removeAllViews();
        }

        View v = inflater.inflate(R.layout.fragment_app, container, false);

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

            MainActivity app = (MainActivity)getActivity();
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

        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accX.setText("AccX: " + df.format(v.accX) );
                accY.setText("AccY: " + df.format(v.accY) );
                accZ.setText("AccZ: " + df.format(v.accZ) );

                rotX.setText("RotX: " + v.rotX);
                rotY.setText("RotY: " + v.rotY);
                rotZ.setText("RotZ: " + v.rotZ);

                light.setText("Light: " + df.format(v.light) + " lumen");

                if(v.location == null && ActiveSubscriptions.usingGPS()) {
                    lat.setText("Lat: Acquiring position…");
                    lon.setText("Lon: Acquiring position…");
                    speed.setText("Speed: Acquiring position…");
                }
                if(v.location != null) {
                    lat.setText("Lat: " + df.format( v.location.getLatitude() ));
                    lon.setText("Lon: " + df.format( v.location.getLongitude() ));
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

        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (v.eventDescription.contains("ROAD"))
                    street.setText(v.eventDescription);
                else if (v.eventDescription.equals("Face detected"))
                    face.setText("Face detected: YES");
                else if (v.eventDescription.equals("No Face detected"))
                    face.setText("Face detected: NO");
                else {
                    event.setText("Last event: " + v.eventDescription);
                }
            }
        });
    }


}
