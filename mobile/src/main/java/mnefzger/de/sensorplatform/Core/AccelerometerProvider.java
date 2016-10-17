package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.ArrayList;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class AccelerometerProvider extends SensorProvider {
    private Sensor accSensor;

    private double[] gravity = new double[3];
    private double[] linear_acceleration = new double[3];
    private ArrayList<double[]> lastValues = new ArrayList<>();
    private double[] lastValue = {0,0,0};
    private int WINDOW = 25;

    /**
     * for accelerometer-gravity low-pass filter
     */
    private float timeConstant = 0.18f;
    private float alpha = 0.5f;
    private float dt = 0;
    private float timestamp = System.nanoTime();
    private float timestampOld = System.nanoTime();
    private int count = 0;

    public AccelerometerProvider(Context a, SensorModule m) {
        super(a, m);
        WINDOW = ( Preferences.getRawDataDelay(setting_prefs) / Preferences.getAccelerometerDelay(setting_prefs) );
    }

    public void start() {
        Log.d("ACCELEROMETER", "Started");

        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            sensorManager.registerListener(this, accSensor, Preferences.getAccelerometerDelay(setting_prefs) );
        }

    }

    public void stop() {
        super.stop();
        sensorManager.unregisterListener(this);
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
        //reportKalmanValues(linear_acceleration);
    }

    /**
     * calculates the exponentially moving average of the last WINDOW values,
     * then notifies the callback function
     * @param newest The most recent accelerometer reading
     */
    private void reportEMAValues(double[] newest) {
        /*
        lastValues.add(newest);
        if(lastValues.size() > WINDOW) {
            lastValues.remove(0);
        }
        double[] emaValues = MathFunctions.getAccEMA(lastValues);
        */

        double[] emaValues = MathFunctions.getEMA(newest, lastValue, 0.9);
        lastValue = emaValues;

        sensorCallback.onAccelerometerData(emaValues);
    }

    private double q = 0.125;
    private double r = 4;
    private double p = 1023;
    private double x = 0;
    private void reportKalmanValues(double[] values) {
        double lastZAcc = values[2];

        p = p + q;
        double k = p / (p+r);
        x = x + k * (lastZAcc - x);
        p = (1-k) * p;

        values[2] = x;
        sensorCallback.onAccelerometerData(values);
    }


}
