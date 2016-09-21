package mnefzger.de.sensorplatform.Core;

import java.util.ArrayList;
import java.util.Iterator;

public class ActiveSubscriptions {
    private static ArrayList<Subscription> activeSubscriptions = new ArrayList<>();
    private static boolean logRaw = false;
    private static boolean logEvent = false;

    public static ArrayList<Subscription> get() {
        return activeSubscriptions;
    }

    public static void add(Subscription s) {
        activeSubscriptions.add(s);
    }

    public static void remove(Subscription s) {
        if(activeSubscriptions.contains(s)) {
            activeSubscriptions.remove(s);
        }
    }

    public static boolean isEmpty() {
        return activeSubscriptions.isEmpty();
    }

    public static void removeAll() {
        activeSubscriptions.clear();
    }

    public static void setLogRaw(boolean log) {
        logRaw = log;
    }

    public static void setLogEvent(boolean log) {
        logEvent = log;
    }

    public static boolean rawLoggingActive() {
        return logRaw;
    }

    public static boolean eventLoggingActive() {
        return logEvent;
    }

    public static boolean rawActive() {
       return isActive(new DataType[]{DataType.ACCELERATION_RAW, DataType.ROTATION_RAW, DataType.LOCATION_RAW, DataType.LIGHT, DataType.OBD, DataType.HEART_RATE});
    }

    public static boolean drivingBehaviourActive() {
        return isActive( new DataType[]{DataType.ACCELERATION_EVENT, DataType.ROTATION_EVENT, DataType.LOCATION_EVENT} );
    }

    public static boolean usingAccelerometer() {
        return isActive(new DataType[]{DataType.ACCELERATION_RAW, DataType.ACCELERATION_EVENT});
    }

    public static boolean usingRotation() {
        return isActive(new DataType[]{DataType.ROTATION_RAW, DataType.ROTATION_EVENT, DataType.LOCATION_EVENT});
    }

    public static boolean usingGPS() {
        return isActive(new DataType[]{DataType.LOCATION_RAW, DataType.LOCATION_EVENT});
    }

    public static boolean usingLight() {
        return isActive(new DataType[]{DataType.LIGHT});
    }

    private static boolean isActive(DataType[] types) {
        Iterator<Subscription> it = activeSubscriptions.iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            DataType type = sub.getType();
            for(DataType t:types) {
                if(type == t) {
                    return true;
                }
            }
        }
        return false;
    }
}