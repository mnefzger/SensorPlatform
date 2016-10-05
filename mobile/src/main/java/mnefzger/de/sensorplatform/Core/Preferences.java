package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import mnefzger.de.sensorplatform.R;

/**
 * This class provides an interface to the res/xml/settings_preferences.xmlences.xml file
 */

public class Preferences {

    public static final String ACCELEROMETER_RAW = "accelerometer_raw";

    public static final String ROTATION_RAW = "rotation_raw";

    public static final String LOCATION_RAW = "location_raw";

    public static final String LIGHT = "light_raw";

    public static final String OBD_RAW = "obd_raw";

    public static final String HEART_RATE = "heartRate_raw";

    public static final String FRONT_CAMERA = "front_active";

    public static final String BACK_CAMERA = "back_active";

    public static final String WEATHER = "weather_active";


    public static final String FREQUENCY_RAWDATA = "frequency_rawData";

    public static final String FREQUENCY_ACCELEROMETER = "frequency_accelerometer";

    public static final String ACCELEROMETER_THRESHOLD_NORMAL = "acceleration_threshold_normal";

    public static final String ACCELEROMETER_THRESHOLD_RISKY = "accelerometer_threshold_risky";

    public static final String ACCELEROMETER_THRESHOLD_DANGEROUS = "accelerometer_threshold_dangerous";

    public static final String FREQUENCY_ROTATION = "frequency_rotation";

    public static final String TURN_THRESHOLD_NORMAL = "rotation_threshold_normal";

    public static final String TURN_THRESHOLD_RISKY = "rotation_threshold_risky";

    public static final String TURN_THRESHOLD_DANGEROUS = "rotation_threshold_dangerous";

    public static final String FREQUENCY_LIGHT = "frequency_light";

    public static final String OSM_REQUEST_RATE = "osmRequest_frequency";

    public static final String GPS_REQUEST_RATE = "gpsRequest_frequency";

    public static final String OBD_REQUEST_RATE = "obdRequest_frequency";


    public static final String FRONT_PROCESSING = "image_front_processing";

    public static final String BACK_PROCESSING = "image_back_processing";

    public static final String VIDEO_SAVING = "image_saving";

    public static final String VIDEO_RESOLUTION = "video_resolution";

    public static final String FRONT_PROCESSING_FPS = "front_processing_fps";

    public static final String BACK_PROCESSING_FPS = "back_processing_fps";


    public static final String LOGGING_RAW = "logging_raw";

    public static final String LOGGING_EVENT = "logging_event";

    public static final String LOG_LEVEL = "log_level";

    public static final String SURVEY = "survey_active";

    public static final String ORIENTATION = "reverse_orientation";



    private static Context context = null;

    public static void setContext(Context app) {
        context = app.getApplicationContext();
    }


    public static boolean accelerometerActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(ACCELEROMETER_RAW, true);

        return value;
    }

    public static boolean rotationActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(ROTATION_RAW, true);

        return value;
    }

    public static boolean locationActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LOCATION_RAW, true);

        return value;
    }

    public static boolean lightActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LIGHT, true);

        return value;
    }

    public static boolean OBDActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(OBD_RAW, false);

        return value;
    }

    public static boolean heartRateActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(HEART_RATE, false);

        return value;
    }

    public static boolean frontCameraActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(FRONT_CAMERA, true);

        return value;
    }

    public static boolean backCameraActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(BACK_CAMERA, true);

        return value;
    }

    public static boolean weatherActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(WEATHER, true);

        return value;
    }

    public static int getRawDataDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_RAWDATA, "500");

        int value = context.getResources().getInteger(R.integer.rawData_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rawData_delay_default);
        }

        return value;
    }


    public static int getOBDDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(OBD_REQUEST_RATE, "500");

        int value = context.getResources().getInteger(R.integer.obdRequest_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.obdRequest_delay_default);
        }

        return value;
    }

    /**
     * ACCELEROMETER
     */
    public static int getAccelerometerDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_ACCELEROMETER, "60000");

        int value = context.getResources().getInteger(R.integer.accelerometer_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.accelerometer_delay_default);
        }

        return value;
    }

    public static double getNormalAccelerometerThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(ACCELEROMETER_THRESHOLD_NORMAL, "0.2");

        double value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_normal_default) );

        try {
            value = Double.valueOf(valueString) * 9.81; // convert to m/s^2
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_normal_default) ) * 9.81;
        }

        return value;
    }

    public static double getRiskyAccelerometerThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(ACCELEROMETER_THRESHOLD_RISKY, "0.3");

        double value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_risky_default) );

        try {
            value = Double.valueOf(valueString) * 9.81; // convert to m/s^2
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_risky_default) ) * 9.81;
        }

        return value;
    }

    public static double getDangerousAccelerometerThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(ACCELEROMETER_THRESHOLD_DANGEROUS, "0.4");

        double value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_dangerous_default) );

        try {
            value = Double.valueOf(valueString) * 9.81; // convert to m/s^2
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_dangerous_default) ) * 9.81;
        }

        return value;
    }


    /**
     * ROTATION
     */
    public static int getOrientationDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_ROTATION, "60000");

        int value = context.getResources().getInteger(R.integer.rotation_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rotation_delay_default);
        }

        return value;
    }

    public static double getTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_NORMAL, "0.45");

        double value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_normal_default) );

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_normal_default) );
        }

        return value;
    }

    public static double getRiskyTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_RISKY, "0.6");

        double value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_risky_default) );

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_risky_default) );
        }

        return value;
    }

    public static double getDangerousTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_DANGEROUS, "0.7");

        double value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_dangerous_default) );

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.rotation_threshold_dangerous_default) );
        }

        return value;
    }


    public static int getLightDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_LIGHT, "60000");

        int value = context.getResources().getInteger(R.integer.light_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.light_delay_default);
        }

        return value;
    }

    /**
     * LOCATION
     */
    public static int getOSMRequestRate(SharedPreferences prefs) {
        String valueString = prefs.getString(OSM_REQUEST_RATE, "5000");

        int value = context.getResources().getInteger(R.integer.osmRequest_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.osmRequest_delay_default);
        }

        return value;
    }

    public static int getGPSRequestRate(SharedPreferences prefs) {
        String valueString = prefs.getString(GPS_REQUEST_RATE, "1000");

        int value = context.getResources().getInteger(R.integer.gpsRequest_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.gpsRequest_delay_default);
        }

        return value;
    }


    /**
     *  CAMERA
     */
    public static boolean frontImagesProcessingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(FRONT_PROCESSING, true);

        return value;
    }


    public static boolean backImagesProcessingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(BACK_PROCESSING, true);

        return value;
    }

    public static boolean videoSavingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(VIDEO_SAVING, true);

        return value;
    }


    public static int getFrontProcessingFPS(SharedPreferences prefs) {
        String valueString = prefs.getString(FRONT_PROCESSING_FPS, "5");

        int value = context.getResources().getInteger(R.integer.front_processing_fps);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.front_processing_fps);
        }

        return value;
    }


    public static int getBackProcessingFPS(SharedPreferences prefs) {
        String valueString = prefs.getString(BACK_PROCESSING_FPS, "5");

        int value = context.getResources().getInteger(R.integer.back_processing_fps);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.back_processing_fps);
        }

        return value;
    }

    public static int getVideoResolution(SharedPreferences prefs) {
        String valueString = prefs.getString(VIDEO_RESOLUTION, "320");

        int value = context.getResources().getInteger(R.integer.video_resolution);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.video_resolution);
        }

        return value;
    }

    /**
     * LOGGING AND SURVEY
     */

    public static boolean rawLoggingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LOGGING_RAW, false);

        return value;
    }

    public static boolean eventLoggingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LOGGING_EVENT, false);

        return value;
    }

    public static int getLogLevel(SharedPreferences prefs) {
        int value = prefs.getInt(LOG_LEVEL, 1);

        return value;
    }

    public static boolean surveyActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(SURVEY, false);

        return value;
    }

    public static boolean isReverseOrientation(SharedPreferences prefs) {
        Boolean reverse = prefs.getBoolean(ORIENTATION, false);

        return reverse;
    }


}
