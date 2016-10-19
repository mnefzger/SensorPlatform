package au.carrsq.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import au.carrsq.sensorplatform.R;

public abstract class SensorProvider extends DataProvider implements SensorEventListener{
    private Context context;
    protected ISensorCallback sensorCallback;
    protected SharedPreferences setting_prefs;
    protected SharedPreferences sensor_prefs;

    protected SensorManager sensorManager;

    SensorProvider(Context app, SensorModule m) {
        this.context = app.getApplicationContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorCallback = m;
        setting_prefs = app.getSharedPreferences(app.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = app.getSharedPreferences(app.getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
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
