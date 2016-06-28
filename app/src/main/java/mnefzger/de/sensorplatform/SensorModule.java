package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.hardware.Sensor;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class SensorModule implements ISensorCallback, IEventCallback{
    /**
     * Callback implemented by SensorPlatformController
     */
    private IDataCallback callback;
    /**
     * SensorProvider for acceleration data
     */
    private SensorProvider accelerometer;
    /**
     * SensorProvider for orientation data
     */
    private SensorProvider orientation;
    /**
     * PositionProvider to collect geolocation
     */
    private PositionProvider location;
    /**
     * List of all currently running SensorProviders
     */
    private List<DataProvider> activeProviders;
    /**
     * Vector containing the most recent raw data
     */
    private DataVector current;
    /**
     * A list containing the last BUFFERSIZE DataVectors
     */
    private ArrayList<DataVector> dataBuffer;
    /**
     * Indicator if a SensorProvider is currently active
     */
    private boolean sensing = false;
    /**
     * The EventProcessor for Driving Behaviour
     */
    private DrivingBehaviourProcessor drivingBehProc;
    /**
     * The EventProcessor for Driver Behaviour
     */
    private DriverBehaviourProcessor driverBehProc;
    /**
     * The size of the dataBuffer
     */
    private final int BUFFERSIZE = 100;
    /**
     * The onRawData() and event detection sampling rate in milliseconds
     */
    private final int SAMPLINGRATE = 250;
    /**
     * int identifier for GPS Sensor
     */
    private final int GPS_IDENTIFIER = 100;


    public SensorModule(SensorPlatformController controller, Activity app) {
        callback = (IDataCallback)controller;

        activeProviders = new ArrayList<>();
        accelerometer = new AccelerometerProvider(app, this);
        location = new PositionProvider(app, this);
        drivingBehProc = new DrivingBehaviourProcessor(this);

        current = new DataVector();
        dataBuffer = new ArrayList<>();
    }

    public void startSensing(DataType type) {
        if(!sensing) {
            aggregateData(SAMPLINGRATE);
            sensing = true;
        }

        int t = getSensorTypeFromDataType(type);

        if(t == Sensor.TYPE_ACCELEROMETER && !activeProviders.contains(accelerometer)) {
            accelerometer.start();
            activeProviders.add(accelerometer);
        }

        if(t == GPS_IDENTIFIER && !activeProviders.contains(location)) {
            location.start();
            activeProviders.add(location);
        }
    }

    /**
     * This method stops the sensor after checking that no active subscriptions depend on it
     * @param type: The unsubscribed DataType
     */
    public void StopSensing(DataType type) {
        int t = getSensorTypeFromDataType(type);

        if(t == Sensor.TYPE_ACCELEROMETER) {
            if(!ActiveSubscriptions.usingAccelerometer()) {
                Log.d("Sensor Stop", "" + Sensor.TYPE_ACCELEROMETER);
                accelerometer.stop();
                activeProviders.remove(accelerometer);
            }
        } else if(t == Sensor.TYPE_ALL) {
            //TODO: loop through sensors, check if sensor is needed, stop it if not
        }

        if(activeProviders.size() == 0) {
            sensing = false;
        }
    }

    private void aggregateData(final int ms) {
        DataVector last = current;
        current = new DataVector();

        /**
         * add last recorded DataVector to Buffer
         * init new DataVector with average acceleration of previous;
         */
        if(last != null) {
            dataBuffer.add(last);
            current.setAcc(last.accX, last.accY, last.accZ);
            current.setSpeed(last.speed);
        }
        /**
         * only store last BUFFERSIZE DataVectors
         */
        if(dataBuffer.size() > BUFFERSIZE) {
            dataBuffer.remove(0);
        }

        /**
         * report raw data after SAMPLINGRATE milliseconds
         */
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                current.setTimestamp(System.currentTimeMillis());
                startEventProcessing();
                if(ActiveSubscriptions.rawActive()){
                    callback.onRawData(current);
                }
                if(sensing) {
                    aggregateData(ms);
                }
            }
        }, ms);
    }

    private void startEventProcessing() {
        if(ActiveSubscriptions.drivingBehaviourActive()) {
            /**
             * Only process the previous xx entries of driving data
             * e.g. SAMPLINGRATE = 50ms, then 1000ms/50ms  = 20 entries
             */
            int lastSamplingIndex = dataBuffer.size() - 500 / SAMPLINGRATE;
            lastSamplingIndex = lastSamplingIndex < 0 ? 0 : lastSamplingIndex;
            drivingBehProc.processData( dataBuffer.subList(lastSamplingIndex, dataBuffer.size()) );
        }
    }

    /**
     * This method is the accelerometer callback function.
     * It receives raw data values and stores them in the current DataVector
     * @param dataValues: the values sensed by the accelerometer
     */
    @Override
    public void onAccelerometerData(double[] dataValues) {
        // store average acceleration in current DataVector
        if(dataBuffer.size() > 0) {
            current.setAcc( (current.accX+dataValues[0]) / 2.0, (current.accY+dataValues[1]) / 2.0, (current.accZ+dataValues[2]) / 2.0 );
        } else {
            current.setAcc( dataValues[0], dataValues[1], dataValues[2] );
        }
    }

    @Override
    public void onLocationData(Location location, double speed) {
        current.setLocation(location);
        current.setSpeed(speed);
    }

    /**
     * Hands the EventVector to the SensorPlatformController
     * @param v: the EventVector containing the event
     */
    @Override
    public void onEventDetected(EventVector v) {
        callback.onEventData(v);
    }

    /**
     * Helper function to convert a DataType to a Sensor
     * @param t: the DataType to be converted
     * @return returns the according Sensor Type (e.g. Sensor.TYPE_ACCELEROMETER)
     */
    public int getSensorTypeFromDataType(DataType t) {
        switch (t) {
            case ACCELERATION_EVENT:
            case ACCELERATION_RAW:
                return Sensor.TYPE_ACCELEROMETER;
            case LOCATION_RAW:
            case LOCATION_EVENT:
                return GPS_IDENTIFIER;
            case RAW:
                return Sensor.TYPE_ALL;
            default:
                return -1;
        }
    }

}
