package mnefzger.de.sensorplatform;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    public static final String ACCELEROMETER_RAW = "accelerometer_raw";

    public static final String ROTATION_RAW = "rotation_raw";

    public static final String LOCATION_RAW = "location_raw";

    public static final String LIGHT = "light_raw";

    public static final String OBD_RAW = "obd_raw";

    public static final String FRONT_CAMERA = "front_active";

    public static final String BACK_CAMERA = "back_active";

    public static final String FRONT_PROCESSING = "image_front_processing";

    public static final String BACK_PROCESSING = "image_back_processing";

    public static final String VIDEO_SAVING = "image_saving";


    public static final String FREQUENCY_RAWDATA = "frequency_rawData";

    public static final String FREQUENCY_ACCELEROMETER = "frequency_accelerometer";

    public static final String ACCELEROMETER_THRESHOLD = "accelerometer_threshold";

    public static final String FREQUENCY_ROTATION = "frequency_rotation";

    public static final String TURN_THRESHOLD_NORMAL = "rotation_threshold_normal";

    public static final String TURN_THRESHOLD_SHARP = "rotation_threshold_risky";

    public static final String FREQUENCY_LIGHT = "frequency_light";

    public static final String OSM_REQUEST_RATE = "osmRequest_frequency";

    public static final String GPS_REQUEST_RATE = "gpsRequest_frequency";

    public static final String OBD_REQUEST_RATE = "obdRequest_frequency";


    public static final String FRONT_CAMERA_FPS = "front_max_fps";

    public static final String FRONT_PROCESSING_FPS = "front_processing_fps";

    public static final String BACK_CAMERA_FPS = "back_max_fps";

    public static final String BACK_PROCESSING_FPS = "back_processing_fps";


    public static final String LOGGING_RAW = "logging_raw";

    public static final String LOGGING_EVENT = "logging_event";



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

    public static boolean frontCameraActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(FRONT_CAMERA, true);

        return value;
    }

    public static boolean backCameraActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(BACK_CAMERA, true);

        return value;
    }

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

    public static int getRawDataDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_RAWDATA, "500");

        int value = context.getResources().getInteger(R.integer.rawData_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rawData_delay_default);;
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
            value = context.getResources().getInteger(R.integer.obdRequest_delay_default);;
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

    public static double getAccelerometerThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(ACCELEROMETER_THRESHOLD, "3.924");

        double value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_default) );

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = Double.valueOf( context.getResources().getString(R.string.acceleration_threshold_default) );
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
        String valueString = prefs.getString(TURN_THRESHOLD_NORMAL, "0.3");

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

    public static double getSharpTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_SHARP, "0.5");

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
     * Location
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
    public static int getFrontFPS(SharedPreferences prefs) {
        String valueString = prefs.getString(FRONT_CAMERA_FPS, "15");

        int value = context.getResources().getInteger(R.integer.front_max_fps);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.front_max_fps);
        }

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


    public static int getBackFPS(SharedPreferences prefs) {
        String valueString = prefs.getString(BACK_CAMERA_FPS, "15");

        int value = context.getResources().getInteger(R.integer.back_max_fps);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.back_max_fps);
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

    public static boolean rawLoggingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LOGGING_RAW, false);

        return value;
    }

    public static boolean eventLoggingActivated(SharedPreferences prefs) {
        boolean value = prefs.getBoolean(LOGGING_EVENT, false);

        return value;
    }


}
