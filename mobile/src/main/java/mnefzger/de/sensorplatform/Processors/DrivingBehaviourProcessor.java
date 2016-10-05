package mnefzger.de.sensorplatform.Processors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mnefzger.de.sensorplatform.Core.DataVector;
import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.Core.Preferences;
import mnefzger.de.sensorplatform.Core.SensorModule;
import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.Utilities.IOSMResponse;
import mnefzger.de.sensorplatform.Utilities.MathFunctions;
import mnefzger.de.sensorplatform.Utilities.OSMQueryAdapter;
import mnefzger.de.sensorplatform.Utilities.OSMRespone;

public class DrivingBehaviourProcessor extends EventProcessor implements IOSMResponse {
    private SharedPreferences setting_prefs;
    private SharedPreferences sensor_prefs;
    private OSMQueryAdapter qAdapter;
    private boolean turned = false;
    private OSMRespone lastResponse;
    private OSMRespone.Element lastRecognizedRoad;
    private OSMRespone.Element nextSpeedSign;
    private OSMRespone.Element passedSpeedSign;
    private int currentSpeedLimit;
    private DataVector currentVector;
    private DataVector previousVector;

    private enum DIRECTION {FORWARD, BACKWARD, UNDEFINED};
    private DIRECTION currentDirection = DIRECTION.UNDEFINED;

    private double ACC_THRESHOLD_NORMAL, ACC_THRESHOLD_RISKY, ACC_THRESHOLD_DANGEROUS;
    private double TURN_THRESHOLD_NORMAL, TURN_THRESHOLD_RISKY, TURN_THRESHOLD_DANGEROUS;
    private int OSM_REQUEST_RATE;

    private int rawDataDelay;

    public DrivingBehaviourProcessor(SensorModule m, Context a) {
        super(m);
        qAdapter = new OSMQueryAdapter(this, a);

        setting_prefs = a.getSharedPreferences(a.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = a.getSharedPreferences(a.getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        ACC_THRESHOLD_NORMAL = Preferences.getNormalAccelerometerThreshold(setting_prefs);
        ACC_THRESHOLD_RISKY = Preferences.getRiskyAccelerometerThreshold(setting_prefs);
        ACC_THRESHOLD_DANGEROUS= Preferences.getDangerousAccelerometerThreshold(setting_prefs);

        TURN_THRESHOLD_NORMAL = Preferences.getNormalTurnThreshold(setting_prefs);
        TURN_THRESHOLD_RISKY = Preferences.getRiskyTurnThreshold(setting_prefs);
        TURN_THRESHOLD_DANGEROUS = Preferences.getDangerousTurnThreshold(setting_prefs);

        OSM_REQUEST_RATE = Preferences.getOSMRequestRate(setting_prefs);

        rawDataDelay = Preferences.getRawDataDelay(setting_prefs);

    }

    public void processData(List<DataVector> data) {
        super.processData(data);

        if(data.size() >= 3) {
            currentVector = data.get(data.size()-1);
            previousVector = data.get(data.size()-2);

            int no_of_items = 2000/rawDataDelay;
            // minimum of 3 items
            //no_of_items = no_of_items >= 3 ? no_of_items : 3;

            checkForHardAcc(getLastDataItems(no_of_items));
            checkForSharpTurn(getLastDataItems(no_of_items));
            checkForSpeeding(currentVector);

        }

    }

    /**
     * Calculates the exponential moving average of several Z acceleration values and matches against threshold
     * @param lastData
     */
    private long lastAccDetected = 0;
    private void checkForHardAcc(List<DataVector> lastData) {
        List<Double> acc = new ArrayList<Double>();
        Iterator<DataVector> it = lastData.iterator();
        double max_abs = 0;
        double max = 0;
        long time = lastData.get(0).timestamp;
        while(it.hasNext()) {
            DataVector next = it.next();
            Double accelerationZ = next.accZ;
            acc.add(accelerationZ);

            // find the timestamp of the peak value
            if(Math.abs(accelerationZ) > max_abs) {
                max_abs = Math.abs(accelerationZ);
                time = next.timestamp;
                max = accelerationZ;
            }
        }

        double avg = max;
        //double avg = MathFunctions.getAccEMASingle(acc, 2/(acc.size()+1) );
        //double avg = MathFunctions.getAccEMASingle(acc, 1);

        // we already looked at this data or last detected was not long ago
        if(time == lastAccDetected || (time-lastAccDetected) < (lastData.size()*rawDataDelay) )
            return;

        //TODO
        if(avg > 0) {
            if(avg > ACC_THRESHOLD_DANGEROUS) {
                EventVector ev = new EventVector(EventVector.LEVEL.DANGEROUS, time, "Brake", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            } else if(avg > ACC_THRESHOLD_RISKY) {
                EventVector ev = new EventVector(EventVector.LEVEL.RISKY, time, "Brake", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            } else if(avg > ACC_THRESHOLD_NORMAL) {
                EventVector ev = new EventVector(EventVector.LEVEL.NORMAL, time, "Brake", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            }
        } else {
            if(avg < -ACC_THRESHOLD_DANGEROUS) {
                EventVector ev = new EventVector(EventVector.LEVEL.DANGEROUS, time, "Acceleration", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            } else if(avg < -ACC_THRESHOLD_RISKY) {
                EventVector ev = new EventVector(EventVector.LEVEL.RISKY, time, "Acceleration", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            } else if(avg < -ACC_THRESHOLD_NORMAL) {
                EventVector ev = new EventVector(EventVector.LEVEL.NORMAL, time, "Acceleration", avg/9.81);
                lastAccDetected = time;
                callback.onEventDetected(ev);

            }
        }

    }

    private long lastTurn = System.currentTimeMillis();
    private void checkForSharpTurn(List<DataVector> lastData) {
        double leftDelta = 0.0;
        double rightDelta = 0.0;
        float[] prevMatrix = null;

        long time = lastData.get(0).timestamp;;

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

                if(rad[2] < leftDelta)  {
                    leftDelta = rad[2];
                    time = v.timestamp;
                }
                if(rad[2] > rightDelta) {
                    rightDelta = rad[2];
                    time = v.timestamp;
                }

                prevMatrix = v.rotMatrix;
            }

        }

        // we already looked at this data or last detected was not long ago
        if(time == lastTurn || (time - lastTurn) < (lastData.size()*rawDataDelay))
            return;

        /**
         * Did the data include a turn?
         */
        if(leftDelta <= -TURN_THRESHOLD_DANGEROUS) {
            EventVector ev = new EventVector(EventVector.LEVEL.DANGEROUS, time, "Left Turn", leftDelta);
            callback.onEventDetected(ev);
        } else if(leftDelta <= -TURN_THRESHOLD_RISKY) {
            EventVector ev = new EventVector(EventVector.LEVEL.RISKY, time, "Left Turn", leftDelta);
            callback.onEventDetected(ev);
        } else if(leftDelta <= -TURN_THRESHOLD_NORMAL) {
            EventVector ev = new EventVector(EventVector.LEVEL.NORMAL, time, "Left Turn", leftDelta);
            callback.onEventDetected(ev);
        }

        if(rightDelta >= TURN_THRESHOLD_DANGEROUS) {
            EventVector ev = new EventVector(EventVector.LEVEL.DANGEROUS, time, "Right Turn", rightDelta);
            callback.onEventDetected(ev);
        } else if(rightDelta >= TURN_THRESHOLD_RISKY) {
            EventVector ev = new EventVector(EventVector.LEVEL.RISKY, time, "Right Turn", rightDelta);
            callback.onEventDetected(ev);
        } else if(rightDelta >= TURN_THRESHOLD_NORMAL) {
            EventVector ev = new EventVector(EventVector.LEVEL.NORMAL, time, "Right Turn", rightDelta);
            callback.onEventDetected(ev);
        }


        /**
         * Did the data include a any turn (safe OR sharp)?
         * If so, request new information on road and speed limits
         */
        if(leftDelta <= -TURN_THRESHOLD_NORMAL || rightDelta >= TURN_THRESHOLD_NORMAL) {
            turned = true;
            lastTurn = System.currentTimeMillis();
            checkForSpeedingInstant(currentVector);
        }

        // if no turn occurred in timeframe, reset variable
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
        if( !(last.lat == 0 && last.lon == 0) ) {
            // query every OSM_REQUEST_RATE seconds
            if( now-lastRequest > OSM_REQUEST_RATE ) {
                qAdapter.startSearchForRoad(currentVector.lat, currentVector.lon);
                lastRequest = now;
            }
        }

        // determine the direction of movement on the road, necessary for traffic_signs
        if(lastRecognizedRoad != null && lastRecognizedRoad.tags != null) {
            currentDirection = getDirectionOfMovement();
            Log.d("DIRECTION", currentDirection+", " + lastRecognizedRoad.tags.name);
        }

    }

    /**
     * To query new road and speed limits after a turn
     */
    private void checkForSpeedingInstant(DataVector last) {
        long now = System.currentTimeMillis();

        if( last.lat != 0 && last.lon != 0 ) {
            qAdapter.startSearchForRoad(currentVector.lat, currentVector.lon);
            lastRequest = now;
        }
    }

    @Override
    public void onOSMRoadResponseReceived(OSMRespone response) {
        if(response == null) {
            lastRecognizedRoad = null;
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: No road detected, empty response.", 0));
            return;
        }

        lastResponse = response;

        OSMRespone.Element road = getCurrentRoad(response);

        if(road != null) {
            // we successfully extracted the current road
            lastRecognizedRoad = road;

            OSMRespone.TagContainer tags = lastRecognizedRoad.tags;
            if(tags != null) {

                if(tags.maxspeed != null) {
                    currentSpeedLimit = Integer.parseInt(tags.maxspeed);
                    callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: You are on " + tags.name + ", Speed Limit: " + currentSpeedLimit, 0));
                } else {
                    callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: You are on " + tags.name, 0));
                }

            }
            // more advanced speed limit search based on traffic signs
            // ATTENTION: EXPERIMENTAL FEATURE
            //qAdapter.startSearchForSpeedLimitSign(currentVector.lat, currentVector.lon);

            // Uncomment this if you don't want to use the traffic sign feature
            detectOverspeeding();

        } else {
            // somehow, we could not extract the current road
            lastRecognizedRoad = null;
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: No road detected, getCurrentRoad() failed", 0));
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

        // TODO make less ugly
        OSMRespone.Element[] relevantSigns = findRelevantSpeedSign(speedLimits);

        nextSpeedSign = relevantSigns[0];
        passedSpeedSign = relevantSigns[1];

        if(nextSpeedSign != null)
            nextSpeedSign.tags.maxspeed = nextSpeedSign.tags.maxspeed_backward == null ? nextSpeedSign.tags.maxspeed_forward : nextSpeedSign.tags.maxspeed_backward;
        if(passedSpeedSign != null)
            passedSpeedSign.tags.maxspeed = passedSpeedSign.tags.maxspeed_backward == null ? passedSpeedSign.tags.maxspeed_forward : passedSpeedSign.tags.maxspeed_backward;

        if(passedSpeedSign != null) {
            if(nextSpeedSign != null)
                callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", Speed Sign Limit: " + passedSpeedSign.tags.maxspeed + ", upcoming: " + nextSpeedSign.tags.maxspeed, 0));
            currentSpeedLimit = Integer.parseInt(passedSpeedSign.tags.maxspeed);
        } else if(lastRecognizedRoad.tags.maxspeed != null) {
            if(nextSpeedSign != null)
                callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "ROAD: You are on " + lastRecognizedRoad.tags.name + ", Speed Limit: " + lastRecognizedRoad.tags.maxspeed + ", upcoming: " + nextSpeedSign.tags.maxspeed, 0));
            currentSpeedLimit = Integer.parseInt(lastRecognizedRoad.tags.maxspeed);
        }


        detectOverspeeding();

    }

    private void detectOverspeeding() {
        double currentSpeed, previousSpeed;
        if(Preferences.OBDActivated(sensor_prefs) && currentVector.obdSpeed != 0) {
            currentSpeed = currentVector.obdSpeed;
            previousSpeed = previousVector.obdSpeed;

        } else {
            currentSpeed = currentVector.speed;
            previousSpeed = previousVector.speed;
        }

        // Fire a event if the last two speed readings were above the speed limit
        if(currentSpeedLimit > 0 && currentSpeed > currentSpeedLimit && previousSpeed > currentSpeedLimit)
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DANGEROUS, System.currentTimeMillis(), "Speeding", currentSpeed, currentSpeedLimit));
        else if(currentSpeed > 110)
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DANGEROUS, System.currentTimeMillis(), "Speeding above 110km/h", currentSpeed));
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
                double temp_dist = getDistanceToRoad(roads.valueAt(i));
                // if there was no turn detected, we assume it is more likely that we are still on the same road
                // to express this likelihood, we multiply the distance to lastRecognizedRoad by 0.75
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
     */
    private double getDistanceToRoad(OSMRespone.Element element) {
        double distance;

        // First, we have to find the two nearest nodes of this street (element)
        OSMRespone.Element[] closest = get2ClosestNodes(element, false);
        OSMRespone.Element near_node1 = closest[0];
        OSMRespone.Element near_node2 = closest[1];

        // now, we calculate the distance between our position and the line between the two nearest nodes
        distance = MathFunctions.calculateDistanceToLine(near_node1.lat, near_node1.lon, near_node2.lat, near_node2.lon, currentVector.lat, currentVector.lon);
        Log.d("DISTANCE", "Distance to " + element.tags.name +":" + distance);

        return distance;
    }

    /**
     * Returns an array of the two nearest nodes on an OpenStreetMap road element in regard to the current position
     * @param element
     * @param orderByIndex: defines if the result should be ordered by index instead of distance
     * @return
     */
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

            double temp_dist = MathFunctions.calculateDistance(el.lat, el.lon, currentVector.lat, currentVector.lon);

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

    /**
     * Compares the vector of GPS signals to the OSM road vector
     * Dependent on the angle between both vectors, we can guess at the direction of movement on the road (Forward or Backward)
     * @return the OSM direction the vehicle is travelling
     */
    private DIRECTION getDirectionOfMovement() {
        OSMRespone.Element[] closest = get2ClosestNodes(lastRecognizedRoad, true);
        OSMRespone.Element near_node1 = closest[0];
        OSMRespone.Element near_node2 = closest[1];

        //Log.d("CLOSE", near_node1.lat + "," + near_node1.lon + "; " + near_node2.lat + "," + near_node2.lon);
        //Log.d("CLOSE", currentVector.location.getLatitude() + "," + currentVector.location.getLongitude() + "; " + previousVector.location.getLatitude() + "," + previousVector.location.getLongitude());

        double[] delta_nodes = {(near_node1.lat - near_node2.lat), (near_node1.lon - near_node2.lon)};

        double[] delta_pos = {( previousVector.lat - currentVector.lat),
                               (previousVector.lon - currentVector.lon)};

        double cos = MathFunctions.cosVectors(delta_nodes, delta_pos);
        double angle = Math.acos(cos) * (180/Math.PI);

        //Log.d("ANGLE", delta_nodes[0]+","+delta_nodes[1]+ "; " + delta_pos[0]+","+delta_pos[1] + "; " + angle);

        if(angle <= 90) {
            return DIRECTION.FORWARD;
        } else if(angle > 90){
            return DIRECTION.BACKWARD;
        } else {
            return DIRECTION.UNDEFINED;
        }
    }

    /**
     * Monstrosity of a method
     * There is probably a smarter way to do it
     * Returns the closest SpeedSign in the direction of driving
     */

    private OSMRespone.Element[] findRelevantSpeedSign(SparseArray<OSMRespone.Element> speedLimits) {
        // Helper class to combine an index and the Speedsign element
        class InfoWrapper {
            int index;
            OSMRespone.Element speedSign;

            InfoWrapper(int index, OSMRespone.Element sign) {
                this.index = index;
                this.speedSign = sign;
            }
        }

        double lat = currentVector.lat;
        double lon = currentVector.lon;

        double minDist = 10000;
        int index = 0;
        ArrayList<InfoWrapper> signs = new ArrayList<>();

        // get the index of the closest node and the indices of the speedsigns
        for(int i=0; i<lastResponse.elements.size(); i++) {
            OSMRespone.Element node = lastResponse.elements.get(i);
            if(node.lat != 0 && node.lon != 0) {
                double temp = MathFunctions.calculateDistance(node.lat, node.lon, lat, lon);
                if(temp < minDist) {
                    minDist = temp;
                    index = i;
                }
            }

            for(int s=0; s<speedLimits.size(); s++) {
                OSMRespone.Element sign = speedLimits.get(s);
                if(sign.id == node.id) {
                    signs.add(new InfoWrapper(i,sign));
                }
            }
        }

        // Separate Signs into ahead and passed categories
        ArrayList<OSMRespone.Element> signsAhead = new ArrayList<>();
        ArrayList<OSMRespone.Element> signsPassed = new ArrayList<>();
        for(InfoWrapper sign : signs) {
            if(currentDirection == DIRECTION.FORWARD) {
                if(sign.index < index)
                    signsPassed.add(sign.speedSign);
                else
                    signsAhead.add(sign.speedSign);

            } else if (currentDirection == DIRECTION.BACKWARD) {
                if(sign.index < index)
                    signsAhead.add(sign.speedSign);
                else
                    signsPassed.add(sign.speedSign);

            }

        }

        // calculate the distances to the speed signs ahead to find the closest
        double aheadDist = 10000;
        OSMRespone.Element closestSignAhead = null;
        for(OSMRespone.Element ahead : signsAhead) {
            double tempDist =  MathFunctions.calculateDistance(ahead.lat, ahead.lon, lat, lon);
            if(tempDist < aheadDist) {
                aheadDist = tempDist;
                closestSignAhead = ahead;
            }
        }

        // calculate the distances to the speed signs passed to find the closest
        double passedDist = 10000;
        OSMRespone.Element closestSignPassed = null;
        for(OSMRespone.Element ahead : signsAhead) {
            double tempDist =  MathFunctions.calculateDistance(ahead.lat, ahead.lon, lat, lon);
            if(tempDist < passedDist) {
                passedDist = tempDist;
                closestSignPassed = ahead;
            }
        }

        OSMRespone.Element[] aheadPassed = {closestSignAhead, closestSignPassed};
        return aheadPassed;
    }

}
