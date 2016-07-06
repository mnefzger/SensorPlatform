package mnefzger.de.sensorplatform;

import android.util.Log;

import java.util.Iterator;
import java.util.List;

public class DrivingBehaviourProcessor extends EventProcessor {

    public DrivingBehaviourProcessor(SensorModule m) {
        super(m);
    }

    private final double ACC_THRESHOLD = 0.75;

    public void processData(List<DataVector> data) {
        super.processData(data);

        checkForHardAcc(getLastData(500));
        checkForSpeeding();

    }

    private void checkForHardAcc(List<DataVector> accData) {
        Log.d("ACC", "" + accData.size());
        Iterator<DataVector> it = accData.iterator();
        DataVector d;
        double avg = 0.0;
        while(it.hasNext()) {
            d = it.next();
            avg += d.accZ;
        }

        avg = avg/accData.size();

        if(avg > ACC_THRESHOLD) {
            EventVector ev = new EventVector(accData.get(0).timestamp, "Hard brake", avg);
            callback.onEventDetected(ev);
        }
        if(avg < -ACC_THRESHOLD) {
            EventVector ev = new EventVector(accData.get(0).timestamp, "Hard acceleration", avg);
            callback.onEventDetected(ev);
        }
    }

    private void checkForSpeeding() {
        //TODO
    }

}
