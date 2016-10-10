package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

/**
 * This class is responsible for getting the rotation vector data from the sensor
 * It reports the rotation matrix and euler angles to the defined callback
 */

public class OrientationProvider extends SensorProvider {
    private Sensor rotationSensor;

    public OrientationProvider(Context a, SensorModule m) {
        super(a,m);
    }

    public void start() {

        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null){
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            sensorManager.registerListener(this, rotationSensor, Preferences.getOrientationDelay(setting_prefs));
        } else {
            Log.d("SENSOR", "TYPE_ROTATION_VECTOR not available on device");
        }

    }

    public void stop() {
        super.stop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values.clone();

        float[] eulerValues = MathFunctions.calculateEulerAngles(values);

        // get the rotation matrix
        float[] temp_matrix = new float[9];
        SensorManager.getRotationMatrixFromVector(temp_matrix, values);

        float[][] result = new float[3][];
        result[0] = eulerValues;
        result[1] = temp_matrix;
        result[2] = values;

        sensorCallback.onRotationData(result);
    }





}
