package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class AccelerometerProvider extends SensorProvider {
    private Sensor accSensor;

    private double[] gravity = new double[3];
    private double[] linear_acceleration = new double[3];
    private ArrayList<double[]> lastValues = new ArrayList<>();
    private final int WINDOW = 5;

    /**
     * for accelerometer-gravity low-pass filter
     */
    private float timeConstant = 0.18f;
    private float alpha = 0.5f;
    private float dt = 0;
    private float timestamp = System.nanoTime();
    private float timestampOld = System.nanoTime();
    private int count = 0;

    public AccelerometerProvider(Activity a, SensorModule m) {
        super(a, m);
    }

    public void start() {
        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        sensorManager.registerListener(this, accSensor, Preferences.getAccelerometerDelay(prefs) );
    }

    public void stop() {
        super.stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        timestamp = System.nanoTime();
        // Find the sample period (between updates).
        // Convert from nanoseconds to seconds
        dt = 1 / (count / ((timestamp - timestampOld) / 1000000000.0f));
        count ++;

        alpha = timeConstant / (timeConstant + dt);

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];


        /**
         * report back smoothed values
         */
        reportEMAValues(linear_acceleration);
        /**
         * report back unfiltered values (only gravity influence eliminated)
         */
        //sensorCallback.onAccelerometerData(linear_acceleration);
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
        sensorCallback.onAccelerometerData(emaValues);
    }


}
