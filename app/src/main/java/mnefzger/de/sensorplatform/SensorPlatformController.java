package mnefzger.de.sensorplatform;

import android.app.Activity;

import java.util.Iterator;

public class SensorPlatformController implements IDataCallback{

    private SensorModule sm;
    private IDataCallback appCallback;


    public SensorPlatformController(Activity app) {
        this.sm = new SensorModule(this, app);
        this.appCallback = (IDataCallback) app;
    }

    public boolean subscribeTo(DataType type, boolean log) {
        /**
         * If a subscription with the same type already exists, return
         */
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            if(it.next().getType() == type) {
                return false;
            }
        }

        Subscription s = new Subscription(type, log);

        switch (type) {
            case ACCELERATION_RAW:
                sm.startSensing(SensorType.ACCELEROMETER);
                break;
            case ACCELERATION_EVENT:
                sm.startSensing(SensorType.ACCELEROMETER);
                break;
            default:
                break;
        }

        ActiveSubscriptions.add(s);

        return true;
    }

    public boolean unsubscribe(DataType type) {
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            if(sub.getType() == type) {
                it.remove();
                ActiveSubscriptions.remove(sub);
                sm.StopSensing(type);
                return true;
            }
        }
        return false;
    }


    @Override
    public void onRawData(DataVector dv) {
        appCallback.onRawData(dv);
    }

    @Override
    public void onEventData(EventVector ev) {
        appCallback.onEventData(ev);
    }



}
