package mnefzger.de.sensorplatform;


import android.app.Activity;
import android.content.Context;

public class SensorModule implements ISensorCallback{
    private IDataCallback callback;
    private SensorProvider accelerometer;
    private SensorProvider orientation;
    private DataVector current;

    public SensorModule(SensorPlatformController controller, Activity app) {
        callback = (IDataCallback)controller;

        accelerometer = new Accelerometer(app, this);
    }

    public void startSensing(SensorType t) {
        if(t == SensorType.ACCELERATION) {
            accelerometer.start();
        }

    }

    @Override
    public void onDataSensed(double[] dataValues) {
        current = new DataVector(System.currentTimeMillis());
        current.setAcc(dataValues[0], dataValues[1], dataValues[2]);
        callback.onRawData(current);
    }
}
