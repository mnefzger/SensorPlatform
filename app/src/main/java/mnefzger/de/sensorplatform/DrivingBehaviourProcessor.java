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
        double avg = 0.0;
        while(it.hasNext()) {
            d = it.next();
            avg += d.accZ;
        }

        avg = avg/data.size();

        if(avg > 1.0) {
            EventVector ev = new EventVector(data.get(0).timestamp, "Hard brake", avg);
            callback.onEventDetected(ev);
        }
        if(avg < -1.0) {
            EventVector ev = new EventVector(data.get(0).timestamp, "Hard acceleration", avg);
            callback.onEventDetected(ev);
        }
    }


}
