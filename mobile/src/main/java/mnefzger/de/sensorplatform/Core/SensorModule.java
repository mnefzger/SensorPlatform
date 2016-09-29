package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mnefzger.de.sensorplatform.External.FitnessSensorManager;
import mnefzger.de.sensorplatform.External.OBD2Provider;
import mnefzger.de.sensorplatform.Processors.DrivingBehaviourProcessor;
import mnefzger.de.sensorplatform.R;

/**
 * This class is the central unit of data collection (except image data).
 * It knows all the Sensor Provider classes and collects raw data values in one DataVector.
 */

public class SensorModule implements ISensorCallback, IEventCallback{
    private SharedPreferences setting_prefs;
    /**
     * Callback implemented by SensorPlatformService
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
     * Provides cabin light data
     */
    private SensorProvider light;
    /**
     * PositionProvider to collect geolocation
     */
    private PositionProvider location;
    /**
     * WeatherProvider to query current weather from Yahoo API
     */
    private WeatherProvider weather;
    /**
     * OBD2Provider to collect vehicle data
     */
    private OBD2Provider obd2;
    /**
     * The DataProvider to collect heart rate from the Android Wear watch
     */
    private FitnessSensorManager fitness;
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
     * The size of the dataBuffer
     */
    private final int BUFFERSIZE = 100;
    /**
     * int identifiers for 'non-Android sensors'
     */
    private final int GPS_IDENTIFIER = 100;
    private final int WEATHER_IDENTIFIER = 101;
    private final int OBD_IDENTIFIER = 102;

    // copy of the study parameters
    private String study_id;
    private String study_name;
    private String participant_id;
    private int participant_age;
    private String participant_gender;

    private Context app;


    public SensorModule(IDataCallback callback, Context app) {
        setting_prefs = app.getSharedPreferences(app.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        this.callback = callback;
        this.app = app;

        activeProviders = new ArrayList<>();

        accelerometer = new AccelerometerProvider(app, this);
        orientation = new OrientationProvider(app, this);
        light = new LightProvider(app, this);
        location = new PositionProvider(app, this);
        weather = new WeatherProvider(app, this);
        obd2 = new OBD2Provider(app, this);
        drivingBehProc = new DrivingBehaviourProcessor(this, app);
        fitness = FitnessSensorManager.getInstance(app);
        fitness.setCallback(this);

        current = new DataVector();
        current.setTimestamp(System.currentTimeMillis());
        dataBuffer = new ArrayList<>();

    }

    public void startSensing(DataType type) {

        if(!sensing) {
            int sampling = Preferences.getRawDataDelay(setting_prefs);
            aggregateData( sampling );
            sensing = true;
        }

        int t = getSensorTypeFromDataType(type);

        if(t == Sensor.TYPE_ACCELEROMETER && !activeProviders.contains(accelerometer)) {
            accelerometer.start();
            activeProviders.add(accelerometer);
        }

        if(t == Sensor.TYPE_ROTATION_VECTOR && !activeProviders.contains(orientation)) {
            orientation.start();
            activeProviders.add(orientation);
        }

        if(t == Sensor.TYPE_LIGHT && !activeProviders.contains(light)) {
            light.start();
            activeProviders.add(light);
        }

        if(t == GPS_IDENTIFIER && !activeProviders.contains(location)) {
            location.start();
            activeProviders.add(location);
            // orientation is needed for road detection
            if( !activeProviders.contains(orientation) ) {
                orientation.start();
                activeProviders.add(orientation);
            }
        }

        if(t == WEATHER_IDENTIFIER && !activeProviders.contains(weather)) {
            weather.start();
            activeProviders.add(weather);
        }

        if(t == OBD_IDENTIFIER && !activeProviders.contains(obd2)) {
            obd2.start();
            activeProviders.add(obd2);
        }

        if(t == Sensor.TYPE_HEART_RATE && !activeProviders.contains(fitness)) {
            fitness.start();
            activeProviders.add(fitness);
        }
    }

    /**
     * This method stops the sensor after checking that no active subscriptions depend on it
     * @param type: The unsubscribed DataType
     */
    public void stopSensing(DataType type) {
        int t = getSensorTypeFromDataType(type);

        Log.d("STOPPED", type + "");

        if (t == Sensor.TYPE_ACCELEROMETER) {
            accelerometer.stop();
            if (activeProviders.contains(accelerometer))
                activeProviders.remove(accelerometer);

        } else if (t == Sensor.TYPE_ROTATION_VECTOR) {
            orientation.stop();
            if (activeProviders.contains(orientation))
                activeProviders.remove(orientation);

        } else if (t == Sensor.TYPE_LIGHT) {
            light.stop();
            if (activeProviders.contains(light))
                activeProviders.remove(light);

        } else if (t == GPS_IDENTIFIER) {
            location.stop();
            orientation.stop();
            if (activeProviders.contains(location))
                activeProviders.remove(location);
            if (activeProviders.contains(orientation))
                activeProviders.remove(orientation);

        } else if (t == WEATHER_IDENTIFIER){
            weather.stop();
            if(activeProviders.contains(weather))
                activeProviders.remove(weather);

        } else if(t == OBD_IDENTIFIER) {
            obd2.stop();
            if(activeProviders.contains(obd2))
                activeProviders.remove(obd2);

        } else if(t == Sensor.TYPE_HEART_RATE) {
            fitness.stop();
            if(activeProviders.contains(fitness))
                activeProviders.remove(fitness);
        }

        Log.d("STOPPED", activeProviders.size()+"");
        if(activeProviders.size() == 0) {
            sensing = false;
        }
    }

    double lon = 152.97542002;
    double lat = -26.98606427;
    private void aggregateData(final int ms) {
        DataVector last = current;

        /*Location mock = new Location("mock");
        mock.setLongitude(lon);
        mock.setLatitude(lat);
        last.setLocation(mock);
        lon -= 0.00001;
        lat += 0.00001;
        weather.updateLocation(mock);*/

        current = new DataVector();

        /**
         * add last recorded DataVector to Buffer
         * init new DataVector with previous values to prevent empty entries;
         */
        if(last != null) {
            dataBuffer.add(last);
            current.setStudyParams(study_id, study_name, participant_id, participant_age, participant_gender);
            current.setAcc(0,0,0);
            current.setRotMatrix(last.rotMatrix);
            current.setLight(last.light);
            current.setLocation(last.lat, last.lon);
            current.setSpeed(last.speed);
            current.setOBDSpeed(last.obdSpeed);
            current.setRPM(last.rpm);
            current.setHeartRate(last.heartRate);
        }
        /**
         * only store last BUFFERSIZE DataVectors
         */
        if(dataBuffer.size() > BUFFERSIZE) {
            dataBuffer.remove(0);
        }

        /**
         * report raw data after defined delay
         */
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(current != null) {
                    current.setTimestamp(System.currentTimeMillis());
                    startEventProcessing();
                    if(ActiveSubscriptions.rawActive()){
                        callback.onRawData(current);
                    }
                }
                if(sensing) {
                    aggregateData(ms);
                }
            }
        }, ms);
    }

    private void startEventProcessing() {
        if(ActiveSubscriptions.drivingBehaviourActive()) {
            drivingBehProc.processData( dataBuffer );
        }
    }

    public void clearDataBuffer() {
        dataBuffer.clear();
    }

    public void initiateOBDSetup() {
        obd2.connect();
    }
    public void cancelOBDSetup() {
        obd2.stop();
    }

    /**
     * This method is the accelerometer callback function.
     * It receives raw data values and stores them in the current DataVector
     * @param dataValues: the values sensed by the accelerometer
     */
    @Override
    public void onAccelerometerData(double[] dataValues) {
        /**
         * Replace the current value only if the new value is more extreme.
         * This guarantees that the same value is stored independent of the <code>RawDataSampling</code> setting.
         */
        if(Math.abs(dataValues[2]) > Math.abs(current.accZ))
            current.setAcc( dataValues[0], dataValues[1], dataValues[2] );
    }

    @Override
    public void onRotationData(float[][] values) {
        current.setRot( values[0][0], values[0][1], values[0][2] );
        current.setRotMatrix( values[1] );
        current.setRotRad(values[2][0], values[2][1], values[2][2]);
    }

    @Override
    public void onLocationData(double lat, double lon, double speed) {
        current.setLocation(lat, lon);
        current.setSpeed(speed);
        // the WeatherProvider needs the most recent location for queries
        if(weather != null)
            weather.updateLocation(lat, lon);
    }

    @Override
    public void onLightData(double lux) {
        current.setLight(lux);
    }

    @Override
    public void onWeatherData(double temp, String des, double wind) {
        current.setWeather(des + ", " + temp + "°C, wind: " + wind + "km/h");
    }

    @Override
    public void onHeartData(double bpm) {
        current.setHeartRate(bpm);
    }

    @Override
    public void onOBD2Data(double[] values) {
        double obdSpeed = values[0];
        double rpm = values[1];
        double currentFuel = values[2];
        current.setOBDSpeed(obdSpeed);
        current.setRPM(rpm);
        current.setFuel(currentFuel);
    }

    /**
     * Hands the EventVector to the SensorPlatformService
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
    private int getSensorTypeFromDataType(DataType t) {
        switch (t) {
            case ACCELERATION_EVENT:
            case ACCELERATION_RAW:
                return Sensor.TYPE_ACCELEROMETER;
            case ROTATION_EVENT:
            case ROTATION_RAW:
                return Sensor.TYPE_ROTATION_VECTOR;
            case LIGHT:
                return Sensor.TYPE_LIGHT;
            case LOCATION_RAW:
            case LOCATION_EVENT:
                return GPS_IDENTIFIER;
            case WEATHER:
                return WEATHER_IDENTIFIER;
            case OBD:
                return OBD_IDENTIFIER;
            case HEART_RATE:
                return Sensor.TYPE_HEART_RATE;
            default:
                return -1;
        }
    }

    public void setStudyParameters() {
        SharedPreferences studyPrefs = app.getSharedPreferences(app.getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        study_id = studyPrefs.getString("study_ID", "");
        study_name = studyPrefs.getString("study_name", "");
        participant_id = studyPrefs.getString("p_ID", "");
        participant_age = studyPrefs.getInt("p_age", -1);
        participant_gender = studyPrefs.getBoolean("p_gender", true) ? "male" : "female";
    }
}
