package mnefzger.de.sensorplatform;


import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
     * List of all currently running SensorProviders
     */
    private List<SensorProvider> activeProviders;
    /**
     * Vector containing the most recent raw data
     */
    private DataVector current;
    /**
     * A list containing the last BUFFERSIZE DataVectors
     */
    private List<DataVector> dataBuffer;
    /**
     * A list containing the events that this SensorModule should monitor
     */
    private Set<DataType> events;
    /**
     * Indicator if a SensorProvider is currently active
     */
    private boolean sensing = false;
    private DrivingBehaviourProcessor drivingBehProc;
    private DriverBehaviourProcessor driverBehProc;


    /**
     * The size of the dataBuffer
     */
    private final int BUFFERSIZE = 100;
    /**
     * The onRawData() reporting sampling rate in milliseconds
     */
    private final int SAMPLINGRATE = 200;


    public SensorModule(SensorPlatformController controller, Activity app) {
        callback = (IDataCallback)controller;

        activeProviders = new ArrayList<>();
        accelerometer = new AccelerometerProvider(app, this);
        drivingBehProc = new DrivingBehaviourProcessor(this);

        current = new DataVector();
        dataBuffer = new ArrayList<>();
        events = new HashSet<DataType>();
    }

    public void startSensing(SensorType t) {
        if(!sensing) {
            aggregateData(SAMPLINGRATE);
            sensing = true;
        }

        if(t == SensorType.ACCELERATION && !activeProviders.contains(accelerometer)) {
            accelerometer.start();
            activeProviders.add(accelerometer);
        }
    }


    public void StopSensing(DataType type) {
        SensorType t = getSensorTypeFromDataType(type);

        if(t == SensorType.ACCELERATION) {
            Log.d("Unsubscribe", t.toString());
            accelerometer.stop();
            activeProviders.remove(accelerometer);
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
                callback.onRawData(current);
                if(sensing) {
                    aggregateData(ms);
                }
            }
        }, ms);
    }

    private void startEventProcessing() {
        if(events.contains(DataType.ACCELERATION_EVENT)) {
            /**
             * Only process the previous xx entries of driving data
             */
            int lastSamplingIndex = dataBuffer.size() - SAMPLINGRATE / SAMPLINGRATE;
            lastSamplingIndex = lastSamplingIndex < 0 ? 0 : lastSamplingIndex;
            drivingBehProc.processData(dataBuffer.subList(lastSamplingIndex, dataBuffer.size()));
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

    /**
     * Hands the EventVector to the SensorPlatformController
     * @param v: the EventVector containing the event
     */
    @Override
    public void onEventDetected(EventVector v) {
        callback.onEventData(v);
    }

    /**
     * Helper function to convert a DataType to a SensorType
     * @param t: the DataType to be converted
     * @return returns the according SensorType (e.g. SensorType.ACCELERATION)
     */
    public SensorType getSensorTypeFromDataType(DataType t) {
        switch (t) {
            case ACCELERATION_EVENT:
            case ACCELERATION_RAW:
                return SensorType.ACCELERATION;
            default:
                return null;
        }
    }

    public void addEvent(DataType t) {
        events.add(t);
    }

    public void removeEvent(DataType t) {
        if(events.contains(t)) {
            events.remove(t);
        }
    }


}
