package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.util.Log;

/**
 * Created by matthias on 20/06/16.
 */
public class SensorPlatformController implements IDataCallback{

    private SensorModule sm;
    private IDataCallback appCallback;

    public SensorPlatformController(Activity app) {
        this.sm = new SensorModule(this, app);
        this.appCallback = (IDataCallback) app;
        //start getting accelerometer data
        sm.startSensing(SensorType.ACCELERATION);
    }

    public void subscribeTo() {

    }

    public void unsubscribe() {

    }


    @Override
    public void onRawData(DataVector v) {
        appCallback.onRawData(v);
    }

    @Override
    public void onEventData(EventVector ev) {

    }
}
