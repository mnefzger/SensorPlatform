package mnefzger.de.sensorplatform;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SensorPlatformController implements IDataCallback{

    private SensorModule sm;
    private IDataCallback appCallback;
    private List<Subscription> activeSubscriptions;

    public SensorPlatformController(Activity app) {
        this.sm = new SensorModule(this, app);
        this.appCallback = (IDataCallback) app;

        activeSubscriptions = new ArrayList<>();
    }

    public boolean subscribeTo(DataType type, boolean log) {
        /**
         * If a subscription with the same type already exists, return
         */
        Iterator<Subscription> it = activeSubscriptions.iterator();
        while(it.hasNext()) {
            if(it.next().getType() == type) {
                return false;
            }
        }

        Subscription s = new Subscription(type, log);

        switch (type) {
            case ACCELERATION_RAW:
                sm.startSensing(SensorType.ACCELERATION);
                break;
            case ACCELERATION_EVENT:
                sm.startSensing(SensorType.ACCELERATION);
                sm.addEvent(type);
                break;
            default:
                break;
        }

        activeSubscriptions.add(s);

        return true;
    }

    public boolean unsubscribe(DataType type) {
        Iterator<Subscription> it = activeSubscriptions.iterator();
        while(it.hasNext()) {
            Subscription temp = it.next();
            if(temp.getType() == type) {
                it.remove();
                activeSubscriptions.remove(temp);
                sm.StopSensing(type);
                sm.removeEvent(type);
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
