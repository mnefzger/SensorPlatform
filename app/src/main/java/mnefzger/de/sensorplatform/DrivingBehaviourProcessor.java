package mnefzger.de.sensorplatform;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.Iterator;
import java.util.List;

import mnefzger.de.sensorplatform.Utilities.IOSMResponse;
import mnefzger.de.sensorplatform.Utilities.MathFunctions;
import mnefzger.de.sensorplatform.Utilities.OSMQueryAdapter;
import mnefzger.de.sensorplatform.Utilities.OSMRespone;

public class DrivingBehaviourProcessor extends EventProcessor implements IOSMResponse {
    private OSMQueryAdapter qAdapter;
    private boolean turned = false;
    private OSMRespone lastResponse;
    private OSMRespone.Element lastRecognizedRoad;
    private DataVector currentVector;
    private DataVector previousVector;

    private enum DIRECTION {FORWARD, BACKWARD, UNDEFINED};
    private DIRECTION currentDirection = DIRECTION.UNDEFINED;
    private int lastClosestIndex = -1;

    /**
     * Hard acceleration / braking threshold
     * Value of 0.4g based on: DriveSafe (Bergasa, 2014)
     */
    private final double ACC_THRESHOLD = 0.4 * 9.81;

    /**
     * Turn threshold in rad/s
     * Value based on (Wang, 2013)
     */
    private final double TURN_THRESHOLD = 0.3;
    private final double TURN_SHARP_THRESHOLD = 0.5;

    /**
     * Time between two OpenStreetMap requests in ms
     */
    private final double OSM_REQUEST_RATE = 5000;


    public DrivingBehaviourProcessor(SensorModule m, Context c) {
        super(m);
        qAdapter = new OSMQueryAdapter(this, c);
    }

    public void processData(List<DataVector> data) {
        super.processData(data);


        if(data.size() >= 3) {
            currentVector = data.get(data.size()-1);
            previousVector = data.get(data.size()-2);

            checkForHardAcc(getLastData(500));
            checkForSharpTurn(getLastData(1000));
            checkForSpeeding(currentVector);
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

                if(rad[2] < leftDelta) leftDelta = rad[2];
                if(rad[2] > rightDelta) rightDelta = rad[2];

                prevMatrix = v.rotMatrix;
            }

        }

        /**
         * Did the data include a sharp turn?
         */
        if(leftDelta <= -TURN_SHARP_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Left Turn", leftDelta);
            callback.onEventDetected(ev);
        }
        if(rightDelta >= TURN_SHARP_THRESHOLD) {
            EventVector ev = new EventVector(lastData.get(0).timestamp, "Sharp Right Turn", rightDelta);
            callback.onEventDetected(ev);
        }

        /**
         * Did the data include a any turn (safe OR sharp)?
         * If so, request new information on road and speed limits
         */
        if(leftDelta <= -TURN_THRESHOLD || rightDelta >= TURN_THRESHOLD) {
            turned = true;
            lastTurn = System.currentTimeMillis();
            checkForSpeedingInstant(currentVector);
        }

        // if no turn occured in timeframe, reset variable
        if(System.currentTimeMillis() - lastTurn > OSM_REQUEST_RATE) {
            turned = false;
        }
    }


    private long lastRequest = System.currentTimeMillis();
    /**
     * To query new road and speed limits every OSM_REQUEST_RATE seconds
     */
    private void checkForSpeeding(DataVector last) {
        long now = System.currentTimeMillis();
        // without location, there is nothing to process
        if( last.location != null ) {
            // query every 5 seconds
            if( now-lastRequest > OSM_REQUEST_RATE ) {
                qAdapter.startSearchForRoad(last.location);
                lastRequest = now;
            }
        }

        // determine the direction of movement on the road, necessary for traffic_signs
        if(lastRecognizedRoad != null) {
            currentDirection = getDirectionOfMovement();
            Log.d("DIRECTION", currentDirection+"");
        }

    }

    /**
     * To query new road and speed limits after a turn
     */
    private void checkForSpeedingInstant(DataVector last) {
        long now = System.currentTimeMillis();

        if( last.location != null ) {
            qAdapter.startSearchForRoad(last.location);
            lastRequest = now;
        }
    }

    @Override
    public void onOSMRoadResponseReceived(OSMRespone response) {
        if(response == null) return;

        lastResponse = response;

        OSMRespone.Element road = getCurrentRoad(response);
        if(road != null) {
            // if the road has changed, reset index for direction
            if(lastRecognizedRoad != null)
                if(!road.tags.name.equals(lastRecognizedRoad.tags.name)) {
                    Log.d("CHANGE", road.tags.name + "," + lastRecognizedRoad.tags.name);
                    lastClosestIndex = -1;
                }

            lastRecognizedRoad = road;
            qAdapter.startSearchForSpeedLimit(currentVector.location);
        } else {
            lastRecognizedRoad = null;
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: No road detected", 0));
        }

    }

    @Override
    public void onOSMSpeedResponseReceived(OSMRespone response) {
        if(response.elements.size() == 0) return;

        // get only the speed limits for the current road
        SparseArray<OSMRespone.Element> speedLimits = new SparseArray<>();
        for(long id : lastRecognizedRoad.nodes) {
            for(OSMRespone.Element e : response.elements) {
                if(e.id == id) {
                    if(e.tags.maxspeed_forward != null && currentDirection == DIRECTION.FORWARD ||
                       e.tags.maxspeed_backward != null && currentDirection == DIRECTION.BACKWARD ||
                       e.tags.maxspeed != null)
                        speedLimits.put(speedLimits.size(), e);
                }
            }
        }

        if(speedLimits.size() > 0 && currentDirection == DIRECTION.FORWARD)
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", SpeedLimit forward: " + speedLimits.get(0).tags.maxspeed_forward, 0));
        else if(speedLimits.size() > 0 && currentDirection == DIRECTION.BACKWARD)
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", SpeedLimit backward: " + speedLimits.get(0).tags.maxspeed_backward, 0));
        else if(speedLimits.size() > 0)
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", SpeedLimit both: " + speedLimits.get(0).tags.maxspeed, 0));
        else if(lastRecognizedRoad.tags.maxspeed != null)
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", SpeedLimit road: " + lastRecognizedRoad.tags.maxspeed, 0));
        else
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name, 0));

        // TODO determine which speed sign is the right one for our current position
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
                // if there was no turn detected, we assume it is more likely that we are still on the same road
                // to express this likelyhood, we multiply the distance to lastRecognizedRoad by 0.75
                if(lastRecognizedRoad == roads.valueAt(i)) {
                    if(!turned) {
                        temp_dist *= 0.75;
                    }
                }
                if(temp_dist < min_dist) {
                    min_dist = temp_dist;

                    currentRoad = roads.valueAt(i);
                }
            }
        }

        return currentRoad;
    }

    /**
     * Returns the distance from current position to a given OpenStreetMap road
     * @param element   The road we want to calculate the distance
     * @param response  Contains all the nodes defining the road
     */
    private double getDistanceToRoad(OSMRespone.Element element, OSMRespone response) {
        double distance;

        // First, we have to find the two nearest nodes of this street (element)
        OSMRespone.Element[] closest = get2ClosestNodes(element, false);
        OSMRespone.Element near_node1 = closest[0];
        OSMRespone.Element near_node2 = closest[1];

        // now, we calculate the distance between our position and the line between the two nearest nodes
        distance = MathFunctions.calculateDistanceToLine(near_node1.lat, near_node1.lon, near_node2.lat, near_node2.lon, currentVector.location.getLatitude(), currentVector.location.getLongitude());
        Log.d("DISTANCE", "Distance to " + element.tags.name +":" + distance);

        return distance;
    }

    private OSMRespone.Element[] get2ClosestNodes(OSMRespone.Element element, boolean orderByIndex) {
        double min_dist1 = 10000;
        OSMRespone.Element near_node1 = null;
        int index1 = -1;
        double min_dist2 = 10000;
        OSMRespone.Element near_node2 = null;
        int index2 = -1;
        for(int r=0; r<element.nodes.size(); r++) {
            long id = element.nodes.get(r);
            OSMRespone.Element el = null;
            for(OSMRespone.Element e : lastResponse.elements) {
                if(e.id == id) {
                    el = e;
                    break;
                }
            }

            double temp_dist = MathFunctions.calculateDistance(el.lat, el.lon, currentVector.location.getLatitude(), currentVector.location.getLongitude());

            if(temp_dist < min_dist2) {
                if(temp_dist < min_dist1) {
                    min_dist2 = min_dist1;
                    near_node2 = near_node1;
                    index2 = index1;

                    min_dist1 = temp_dist;
                    near_node1 = el;
                    index1 = r;

                } else  {
                    min_dist2 = temp_dist;
                    near_node2 = el;
                    index2 = r;
                }
            }
        }

        OSMRespone.Element[] closest = {near_node1, near_node2};

        // if orderByIndex == true, node with the smaller index should come first
        if(orderByIndex && index2 < index1) {
            closest[0] = near_node2;
            closest[1] = near_node1;
        }

        return closest;
    }

    private DIRECTION getDirectionOfMovement() {
        OSMRespone.Element[] closest = get2ClosestNodes(lastRecognizedRoad, true);
        OSMRespone.Element near_node1 = closest[0];
        OSMRespone.Element near_node2 = closest[1];

        Log.d("CLOSE", near_node1.lat + "," + near_node1.lon + "; " + near_node2.lat + "," + near_node2.lon);
        Log.d("CLOSE", currentVector.location.getLatitude() + "," + currentVector.location.getLongitude() + "; " + previousVector.location.getLatitude() + "," + previousVector.location.getLongitude());

        double[] delta_nodes = {(near_node1.lat - near_node2.lat), (near_node1.lon - near_node2.lon)};

        double[] delta_pos = {( previousVector.location.getLatitude() - currentVector.location.getLatitude()),
                               (previousVector.location.getLongitude() - currentVector.location.getLongitude())};

        double dot = MathFunctions.dotProduct(delta_nodes, delta_pos);
        //double cross = MathFunctions.crossProduct(delta_nodes, delta_pos);
        double cos = dot / ( (Math.sqrt(delta_nodes[0]*delta_nodes[0] + delta_nodes[1]*delta_nodes[1])) *
                               (Math.sqrt(delta_pos[0]*delta_pos[0] + delta_pos[1]*delta_pos[1])) );
        double angle = Math.acos(cos) * (180/Math.PI);
        //double angle = Math.atan2(cross, dot);
        //angle += 180;
        //angle /= 2;

        Log.d("ANGLE", delta_nodes[0]+","+delta_nodes[1]+ "; " + delta_pos[0]+","+delta_pos[1] + "; " + angle);

        if(angle <= 90) {
            return DIRECTION.FORWARD;
        } else if(angle > 90){
            return DIRECTION.BACKWARD;
        } else {
            return DIRECTION.UNDEFINED;
        }
    }

    /*
    private DIRECTION getDirectionOfMovement() {
        double lat = currentVector.location.getLatitude();
        double lon = currentVector.location.getLongitude();

        double minDist = 10000;
        int index = 0;

        for(int i=0; i<lastResponse.elements.size(); i++) {
            OSMRespone.Element node = lastResponse.elements.get(i);
            if(node.lat != 0 && node.lon != 0) {
                double temp = MathFunctions.calculateDistance(node.lat, node.lon, lat, lon);
                if(temp < minDist) {
                    minDist = temp;
                    index = i;
                }
            }
        }

        Log.d("INDEX", index+"");

        if(lastClosestIndex == -1) {
            lastClosestIndex = index;
            Log.d("INDEX", index+" was -1");
            return DIRECTION.UNDEFINED;

        } else if(lastClosestIndex < index) {
            lastClosestIndex = index;
            return DIRECTION.FORWARD;

        } else if(lastClosestIndex > index) {
            lastClosestIndex = index;
            return DIRECTION.BACKWARD;

        } else if(lastClosestIndex == index && currentDirection != DIRECTION.UNDEFINED) {
            return currentDirection;

        } else {
            Log.d("INDEX", index+" else");
            lastClosestIndex = index;
            return DIRECTION.UNDEFINED;
        }
    }
    */
}
