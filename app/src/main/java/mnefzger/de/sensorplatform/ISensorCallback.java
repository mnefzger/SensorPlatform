package mnefzger.de.sensorplatform;


import android.location.Location;

public interface ISensorCallback {
    void onAccelerometerData(double[] values);

    void onLocationData(Location location, double speed);
}
