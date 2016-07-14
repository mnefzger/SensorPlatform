package mnefzger.de.sensorplatform;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;
import java.lang.reflect.Array;

import java.util.Iterator;
import java.util.List;


import mnefzger.de.sensorplatform.Utilities.IOSMResponse;
import mnefzger.de.sensorplatform.Utilities.MathFunctions;
import mnefzger.de.sensorplatform.Utilities.OSMQueryAdapter;
import mnefzger.de.sensorplatform.Utilities.OSMRespone;

public class DrivingBehaviourProcessor extends EventProcessor implements IOSMResponse {
    private OSMQueryAdapter qAdapter;
    private boolean turned = false;
    private OSMRespone.Element lastRecognizedRoad;
    private DataVector lastVector;

    public DrivingBehaviourProcessor(SensorModule m, Context c) {
        super(m);
        qAdapter = new OSMQueryAdapter(this, c);
    }

    private final double ACC_THRESHOLD = 0.75;
    /**
     * Turn threshold in rad/s
     * Value based on literature
     */
    private final double TURN_THRESHOLD = 0.3;
    private final double TURN_SHARP_THRESHOLD = 0.5;
    /**
     * Turn threshold in degrees / second
     * 0.5 rad/s = 0.5 * 360/2pi deg/s
     */
    private final double TURN_THRESHOLD_DEG = 28.6478897565;
    /**
     * Time between two OpenStreetMap requests in ms
     */
    private final double OSM_REQUEST_RATE = 5000;


    public void processData(List<DataVector> data) {
        super.processData(data);
        lastVector = data.get(data.size()-1);

        if(data.size() >= 3) {
            checkForHardAcc(getLastData(500));
            checkForSharpTurn(getLastData(1000));
            checkForSpeeding(lastVector);
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

    private long lastTurn = System.currentTimeMillis();
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

                // convert to radians
                float[] rad = MathFunctions.calculateRadAngles(angleChange);

                if(rad[2] > leftDelta) leftDelta = rad[2];
                if(rad[2] < rightDelta) rightDelta = rad[2];

                prevMatrix = v.rotMatrix;
            }

        }

        /**
         * Did the data include a sharp turn?
         */
        if(leftDelta >= TURN_SHARP_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Left Turn", leftDelta);
            callback.onEventDetected(ev);
        }
        if(rightDelta <= -TURN_SHARP_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Right Turn", rightDelta);
            callback.onEventDetected(ev);
        }

        /**
         * Did the data include a normal turn?
         */
        if(leftDelta >= TURN_THRESHOLD) {
            turned = true;
            lastTurn = System.currentTimeMillis();
        } else if(rightDelta <= -TURN_THRESHOLD) {
            turned = true;
            lastTurn = System.currentTimeMillis();
        }

        // if no turn occured in timeframe, reset variable
        if(System.currentTimeMillis() - lastTurn > OSM_REQUEST_RATE) {
            turned = false;
        }
    }

    private long lastRequest = System.currentTimeMillis();
    private void checkForSpeeding(DataVector last) {
        long now = System.currentTimeMillis();
        // without location, there is nothing to process
        if( last.location != null ) {
            // query every 5 seconds
            if( now-lastRequest > OSM_REQUEST_RATE ) {
                qAdapter.startSearch(last.location);
                lastRequest = now;
            }

        }
    }

    @Override
    public void onOSMResponseReceived(OSMRespone response) {
        if(response == null) return;

        OSMRespone.Element road = getCurrentRoad(response);
        if(road != null) {
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "You might be on " + road.tags.name, 0));
            lastRecognizedRoad = road;
        } else {
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "No road detected", 0));
        }

        // TODO request speeding data
    }

    /**
     * Returns the best estimate for the current road
     * If only one road is detected, we assume that it is the correct one.
     * If two or more roads are detected, we check which road is closest
     */
    private OSMRespone.Element getCurrentRoad(OSMRespone response) {
        SparseArray<OSMRespone.Element> roads = new SparseArray<>();
        OSMRespone.Element currentRoad = null;

        for(OSMRespone.Element e : response.elements) {
            if(e.tags != null) {
                if (e.tags.name != null && e.tags.highway != null) {
                    roads.put(roads.size(), e);
                }
            }
        }

        if(roads.size() == 1) {
            currentRoad = roads.valueAt(0);

        } else if(roads.size() > 1) {
            double min_dist = 10000;
            for(int i=0; i<roads.size(); i++) {
                double temp_dist = getDistanceToRoad(roads.valueAt(i), response);
                if(temp_dist < min_dist) {
                    min_dist = temp_dist;
                    currentRoad = roads.valueAt(i);
                }
            }
        }

        return currentRoad;
    }

    private double getDistanceToRoad(OSMRespone.Element element, OSMRespone response) {
        double distance = 10000;

        // First, we have to find the two nearest nodes of this street (element)
        double min_dist1 = 10000;
        OSMRespone.Element near_elem1 = null;
        double min_dist2 = 10000;
        OSMRespone.Element near_elem2 = null;
        for(int r=0; r<element.nodes.size(); r++) {
            int id = element.nodes.get(r);
            OSMRespone.Element el = null;
            for(OSMRespone.Element e : response.elements) {
                if(e.id == id) el = e;
            }
            double temp_dist = MathFunctions.calculateDistance(el.lat, el.lon, lastVector.location.getLatitude(), lastVector.location.getLongitude());

            if(temp_dist < min_dist2) {
                if(temp_dist < min_dist1) {
                    min_dist1 = temp_dist;
                    near_elem1 = el;

                } else  {
                    min_dist2 = temp_dist;
                    near_elem2 = el;
                }
            }
        }

        // now, we calculate the distance between our position and the line between the two nearest nodes
        distance = MathFunctions.calculateDistanceToLine(near_elem1.lat, near_elem1.lon, near_elem2.lat, near_elem2.lon, lastVector.location.getLatitude(), lastVector.location.getLongitude());
        //Log.d("DISTANCE", "Distance to " + element.tags.name +":" + distance);

        return distance;
    }
}
