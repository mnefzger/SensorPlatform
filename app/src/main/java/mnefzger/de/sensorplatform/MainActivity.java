package mnefzger.de.sensorplatform;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements IDataCallback{
    SensorPlatformController sPC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sPC = new SensorPlatformController(this);
        sPC.subscribeTo(DataType.ACCELERATION_RAW, false);
    }


    @Override
    public void onRawData(DataVector v) {
        Log.d("RawData @ App", v.toString());
    }

    @Override
    public void onEventData(EventVector v) {
        Log.d("EventData @ App", v.toString());
    }
}
