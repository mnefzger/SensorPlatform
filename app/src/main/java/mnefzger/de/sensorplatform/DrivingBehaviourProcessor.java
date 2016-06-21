package mnefzger.de.sensorplatform;

import android.util.Log;

import java.util.Iterator;
import java.util.List;

public class DrivingBehaviourProcessor extends EventProcessor {

    public DrivingBehaviourProcessor(SensorModule m) {
        super(m);
    }

    public void processData(List<DataVector> data) {
        super.processData(data);
        checkForHardAcc();
    }

    private void checkForHardAcc() {
        Iterator<DataVector> it = data.iterator();
        DataVector d;
        while(it.hasNext()) {
            d = it.next();
            if(d.accZ > 2.0) {
                EventVector ev = new EventVector(d.timestamp, "Hard brake", d.accZ);
                callback.onEventDetected(ev);
            }
            if(d.accZ < -2.0) {
                EventVector ev = new EventVector(d.timestamp, "Hard acceleration", d.accZ);
                callback.onEventDetected(ev);
            }
        }
    }


}
