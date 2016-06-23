package mnefzger.de.sensorplatform;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import java.util.ArrayList;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class AccelerometerProvider extends SensorProvider {
    private Sensor accSensor;

    private double[] gravity = new double[3];
    private double[] linear_acceleration = new double[3];
    private ArrayList<double[]> lastValues = new ArrayList<>();
    private final int WINDOW = 20;

    public AccelerometerProvider(Context c, SensorModule m) {
        super(c, m);
    }

    public void start() {
        super.start();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        super.stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // alpha value taken from Google Dev Docs
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        reportEMAValues(linear_acceleration);
    }

    private void reportEMAValues(double[] newest) {
        lastValues.add(newest);
        if(lastValues.size() > WINDOW) {
            lastValues.remove(0);
        }
        double[] emaValues = MathFunctions.getAccEMA(lastValues);
        sensorCallback.onAccelerometerData(emaValues);
    }


}
