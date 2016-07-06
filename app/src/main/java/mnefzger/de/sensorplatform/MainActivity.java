package mnefzger.de.sensorplatform;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements IDataCallback{
    SensorPlatformController sPC;

    TextView accX;
    TextView accY;
    TextView accZ;

    TextView rotX;
    TextView rotY;
    TextView rotZ;

    TextView lat;
    TextView lon;
    TextView speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sPC = new SensorPlatformController(this);
        //sPC.subscribeTo(DataType.ACCELERATION_EVENT);
        sPC.subscribeTo(DataType.ACCELERATION_RAW);
        sPC.subscribeTo(DataType.LOCATION_RAW);
        sPC.subscribeTo(DataType.ROTATION_RAW);

        sPC.logRawData(false);
        sPC.logEventData(false);

        /**
         * Mock unsubscribe

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                sPC.unsubscribe(DataType.ACCELERATION_EVENT);
            }
        }, 5000);
         */
        accX = (TextView) findViewById(R.id.accXText);
        accY = (TextView) findViewById(R.id.accYText);
        accZ = (TextView) findViewById(R.id.accZText);

        rotX = (TextView) findViewById(R.id.rotXText);
        rotY = (TextView) findViewById(R.id.rotYText);
        rotZ = (TextView) findViewById(R.id.rotZText);

        lat = (TextView) findViewById(R.id.latText);
        lon = (TextView) findViewById(R.id.lonText);
        speed = (TextView) findViewById(R.id.speedText);

    }

    @Override
    public void onRawData(DataVector v) {
        Log.d("RawData @ App  ", v.toString());
        updateUI(v);
    }

    @Override
    public void onEventData(EventVector v) {
        Log.d("EventData @ App  ", v.toString());
    }

    @Override
    public void onImageData() {

    }

    public void updateUI(DataVector vector) {
        final DataVector v = vector;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                accX.setText("AccX: " + v.accX);
                accY.setText("AccY: " + v.accY);
                accZ.setText("AccZ: " + v.accZ);

                rotX.setText("RotX: " + v.rotX);
                rotY.setText("RotY: " + v.rotY);
                rotZ.setText("RotZ: " + v.rotZ);

                if(v.location != null) {
                    lat.setText("Lat: " + v.location.getLatitude());
                    lon.setText("Lon: " + v.location.getLongitude());
                    speed.setText("Speed: " + v.speed);
                }


            }
        });

    }
}
