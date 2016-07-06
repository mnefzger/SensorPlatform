package mnefzger.de.sensorplatform;

import android.hardware.SensorManager;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class DrivingBehaviourProcessor extends EventProcessor {

    public DrivingBehaviourProcessor(SensorModule m) {
        super(m);
    }

    private final double ACC_THRESHOLD = 0.75;
    /**
     * Turn threshold in rad/s
     * Value based on literature
     */
    private final double TURN_THRESHOLD = 0.5;
    /**
     * Turn threshold in degrees / second
     * Above this value, a turn is considered as unsafe
     * 0.5 rad/s = 0.5 * 360/2pi deg/s
     */
    private final double TURN_THRESHOLD_DEG = 28.6478897565;

    public void processData(List<DataVector> data) {
        super.processData(data);

        if(data.size() >= 3) {
            checkForHardAcc(getLastData(500));
            checkForSpeeding();
            checkForSharpTurn(getLastData(1000));
        }


    }

    private void checkForHardAcc(List<DataVector> lastData) {
        Iterator<DataVector> it = lastData.iterator();
        DataVector d;
        double avg = 0.0;
        while(it.hasNext()) {
            d = it.next();
            avg += d.accZ;
        }

        avg = avg/lastData.size();

        if(avg > ACC_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Hard brake", avg);
            callback.onEventDetected(ev);
        }
        if(avg < -ACC_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Hard acceleration", avg);
            callback.onEventDetected(ev);
        }
    }

    private void checkForSharpTurn(List<DataVector> lastData) {
        double leftDelta = 0.0;
        double rightDelta = 0.0;
        float[] prevMatrix = new float[9];

        Iterator<DataVector> it = lastData.iterator();
        while(it.hasNext()) {

            DataVector v = it.next();
            if(prevMatrix == null) {
                prevMatrix = v.rotMatrix;
            }

            if(prevMatrix != null && v.rotMatrix != null) {
                float[] angleChange = new float[3];
                // calculate the angle change between rotation matrices
                SensorManager.getAngleChange(angleChange, prevMatrix, v.rotMatrix);
                // convert to euler angles
                float[] euler = MathFunctions.calculateEulerAngles(angleChange);

                float[] rad = MathFunctions.calculateRadAngles(angleChange);

                //TODO: check what is actually left and right
                if(rad[2] > leftDelta) leftDelta = rad[2];
                if(rad[2] < rightDelta) rightDelta = rad[2];

                prevMatrix = v.rotMatrix;
            }

        }

        if(leftDelta >= TURN_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Left Turn", leftDelta);
            callback.onEventDetected(ev);
        }
        if(rightDelta <= -TURN_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Right Turn", rightDelta);
            callback.onEventDetected(ev);
        }
    }

    private void checkForSpeeding() {
        //TODO
    }

}
