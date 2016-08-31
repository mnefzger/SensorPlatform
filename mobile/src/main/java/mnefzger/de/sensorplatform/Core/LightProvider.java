package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class LightProvider extends SensorProvider {
    private Sensor lightSensor;

    LightProvider(Context app, SensorModule m) {
        super(app, m);
    }

    public void start() {
        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            sensorManager.registerListener(this, lightSensor, Preferences.getLightDelay(prefs));
        }

    }

    public void stop() {
        super.stop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorCallback.onLightData(event.values[0]);
    }


}
