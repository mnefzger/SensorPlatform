package mnefzger.de.sensorplatform;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    public static final String FREQUENCY_ACCELEROMETER = "frequency_accelerometer";

    public static final String ACCELEROMETER_THRESHOLD = "accelerometer_threshold";

    public static final String FREQUENCY_ROTATION = "frequency_rotation";

    public static final String TURN_THRESHOLD_NORMAL = "rotation_threshold_normal";

    public static final String TURN_THRESHOLD_SHARP = "rotation_threshold_risky";

    public static final String FREQUENCY_RAWDATA = "frequency_rawData";

    public static final String OSM_REQUEST_RATE = "osmRequest_frequency";

    public static Context context;
    public static void setContext(Context c) {
        context = c;
    }


    /**
     * ACCELEROMETER
     */
    public static int getAccelerometerDelay(SharedPreferences prefs) {
        String valueString = prefs.getString(FREQUENCY_ACCELEROMETER, "60000");

        int value = context.getResources().getInteger(R.integer.accelerometer_delay_default); // by default 60000 microseconds

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.accelerometer_delay_default);;
        }

        return value;
    }

    public static double getAccelerometerThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(ACCELEROMETER_THRESHOLD, "3.924");

        double value = context.getResources().getInteger(R.integer.acceleration_threshold_default);

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.acceleration_threshold_default);;
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
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rotation_delay_default);
        }

        return value;
    }

    public static double getTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_NORMAL, "0.3");

        double value = context.getResources().getInteger(R.integer.rotation_threshold_normal_default);

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rotation_threshold_normal_default);;
        }

        return value;
    }

    public static double getSharpTurnThreshold(SharedPreferences prefs) {
        String valueString = prefs.getString(TURN_THRESHOLD_SHARP, "0.5");

        double value = context.getResources().getInteger(R.integer.rotation_threshold_risky_default);

        try {
            value = Double.valueOf(valueString);
        } catch (Exception e) {
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.rotation_threshold_risky_default);;
        }

        return value;
    }


    /**
     * OpenStreetMap
     */
    public static int getOSMRequestRate(SharedPreferences prefs) {
        String valueString = prefs.getString(OSM_REQUEST_RATE, "5000");

        int value = context.getResources().getInteger(R.integer.osmRequest_delay_default);

        try {
            value = Integer.valueOf(valueString);
        } catch (Exception e) {
        }

        if (value <= 0) {
            value = context.getResources().getInteger(R.integer.osmRequest_delay_default);;
        }

        return value;
    }


}
