package mnefzger.de.sensorplatform;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by matthias on 22/07/16.
 */
public class Preferences {

    public static final String FREQUENCY_ACCELEROMETER = "frequency_accelerometer";

    public static final String ACCELEROMETER_THRESHOLD = "accelerometer_threshold";

    public static final String FREQUENCY_ROTATION = "frequency_rotation";

    public static final String TURN_THRESHOLD_NORMAL = "rotation_threshold_normal";

    public static final String TURN_THRESHOLD_SHARP = "rotation_threshold_risky";

    public static final String FREQUENCY_RAWDATA = "frequency_rawData";

}
