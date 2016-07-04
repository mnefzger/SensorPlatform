package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;


public class OrientationProvider extends SensorProvider {
    private Sensor rotationSensor;

    private ArrayList<double[]> lastValues = new ArrayList<>();
    private final int WINDOW = 5;

    public OrientationProvider(Context c, SensorModule m) {
        super(c,m);
    }

    public void start() {
        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null){
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        } else {
            Log.d("SENSOR", "TYPE_ROTATION_VECTOR not available on device");
        }

        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        super.stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values.clone();

        double[] eulerValues = MathFunctions.calculateEulerAngles(values);

        //reportEMAValues(values);
        sensorCallback.onRotationData(eulerValues);
    }

    /**
     * calculates the exponentially moving average of the last WINDOW values,
     * then notifies the callback function
     * @param newest The most recent accelerometer reading
     */
    private void reportEMAValues(double[] newest) {
        lastValues.add(newest);
        if(lastValues.size() > WINDOW) {
            lastValues.remove(0);
        }
        double[] emaValues = MathFunctions.getAccEMA(lastValues);
        sensorCallback.onRotationData(emaValues);
    }
}
