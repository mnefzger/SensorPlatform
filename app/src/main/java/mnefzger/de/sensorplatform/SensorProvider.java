package mnefzger.de.sensorplatform;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class SensorProvider implements SensorEventListener{
    private Context context;
    protected ISensorCallback sensorCallback;

    protected SensorManager sensorManager;

    SensorProvider(Context c, SensorModule m) {
        this.context = c;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorCallback = (ISensorCallback) m;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void start() {

    }

    public  void stop() {
        sensorManager.unregisterListener(this);
    }
}
