package au.carrsq.sensorplatform.Core;


public interface ISensorCallback {
    void onAccelerometerData(double[] values);

    // we need float to store the rotation matrix
    void onRotationData(float[][] values);

    void onLocationData(double lat, double lon, double speed);

    void onOBD2Data(double[] values);

    void onLightData(double value);

    void onWeatherData(double temp, String description, double wind);

    void onHeartData(double value);
}
