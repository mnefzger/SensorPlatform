package mnefzger.de.sensorplatform;

import java.util.ArrayList;
import java.util.Iterator;

class ActiveSubscriptions {
    private static ArrayList<Subscription> activeSubscriptions = new ArrayList<>();

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

    public static boolean rawActive() {
       return isActive(new DataType[]{DataType.RAW, DataType.ACCELERATION_RAW});
    }

    public static boolean drivingBehaviourActive() {
        return isActive( new DataType[]{DataType.ACCELERATION_EVENT} );
    }

    public static boolean usingAccelerometer() {
        return isActive(new DataType[]{DataType.RAW, DataType.ACCELERATION_RAW, DataType.ACCELERATION_EVENT});
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

    public static boolean loggingActive() {
        Iterator<Subscription> it = activeSubscriptions.iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            DataType type = sub.getType();
            if( (type == DataType.ACCELERATION_RAW ||
                 type == DataType.RAW) &&
                 sub.includesLogging()) {
                return true;
            }
        }
        return false;
    }
}