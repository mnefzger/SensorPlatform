package mnefzger.de.sensorplatform;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

public abstract class SensorProvider extends DataProvider implements SensorEventListener{
    private Context context;
    protected ISensorCallback sensorCallback;
    protected SharedPreferences prefs;
    protected Preferences preferences;

    protected SensorManager sensorManager;

    SensorProvider(Context app, SensorModule m) {
        this.context = app.getApplicationContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorCallback = m;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void start() {

    }

    @Override
    public  void stop() {

    }
}
