package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.util.List;

import mnefzger.de.sensorplatform.R;


public class TripEndDetector {
    private SharedPreferences setting_prefs;
    private SharedPreferences sensor_prefs;

    private boolean counting = false;
    private Long firstStop = null;

    private ITripDetectionCallback callback;

    public TripEndDetector(ITripDetectionCallback callback, Context c) {
        setting_prefs = c.getSharedPreferences(c.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = c.getSharedPreferences(c.getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        this.callback = callback;
    }


    public void checkForTripEnd(DataVector dv) {

        boolean obd_active = Preferences.OBDActivated(sensor_prefs);
        if(obd_active)
            checkInOBD(dv);

        boolean location_active = Preferences.locationActivated(sensor_prefs);
        if(location_active)
            checkInSpeed(dv);

    }

    private void checkInOBD(DataVector dv) {
        if(dv.rpm == 0) {
            callback.onTripEnd();
            return;
        }
    }

    private void checkInSpeed(DataVector dv) {
        if(dv.speed < 1) {
            if(!counting) {
                firstStop = dv.timestamp;
                counting = true;
            } else {
                long duration = dv.timestamp - firstStop;

                if(duration > 30000) {
                    callback.onTripEnd();
                }
            }
        } else {
            counting = false;
            firstStop = null;
        }
    }
}
