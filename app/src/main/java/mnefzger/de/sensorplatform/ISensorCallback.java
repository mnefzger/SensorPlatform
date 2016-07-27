package mnefzger.de.sensorplatform;


import android.location.Location;

public interface ISensorCallback {
    void onAccelerometerData(double[] values);

    // we need float to store the rotation matrix
    void onRotationData(float[][] values);

    void onLocationData(Location location, double speed);

    void onOBD2Data(double[] values);
}
