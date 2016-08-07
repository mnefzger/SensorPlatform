package mnefzger.de.sensorplatform.UI;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;

import mnefzger.de.sensorplatform.ActiveSubscriptions;
import mnefzger.de.sensorplatform.DataVector;
import mnefzger.de.sensorplatform.EventVector;
import mnefzger.de.sensorplatform.R;


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
            container.removeViewAt(0);
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

        return v;
    }

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

                light.setText("Light: " + v.light);

                if(v.location == null && ActiveSubscriptions.usingGPS()) {
                    lat.setText("Lat: Acquiring position…");
                    lon.setText("Lon: Acquiring position…");
                    speed.setText("Speed: Acquiring position…");
                }
                if(v.location != null) {
                    lat.setText("Lat: " + v.location.getLatitude());
                    lon.setText("Lon: " + v.location.getLongitude());
                    speed.setText("Speed: " + df.format(v.speed) + " km/h");
                }

                obdRPM.setText("OBD RPM: " + v.rpm);
                obdSpeed.setText("OBD Speed: " + v.obdSpeed);
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
                else event.setText("Last event: " + v.eventDescription);
            }
        });
    }


}
