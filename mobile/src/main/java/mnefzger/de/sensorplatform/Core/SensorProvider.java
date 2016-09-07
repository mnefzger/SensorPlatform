package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

import mnefzger.de.sensorplatform.R;

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
        prefs = app.getSharedPreferences(app.getString(R.string.preferences_key), Context.MODE_PRIVATE);
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
