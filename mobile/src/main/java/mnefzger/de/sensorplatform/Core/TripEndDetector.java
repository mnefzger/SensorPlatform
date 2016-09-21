package mnefzger.de.sensorplatform.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.External.OBD2Connector;
import mnefzger.de.sensorplatform.R;


public class TripEndDetector {
    private SharedPreferences setting_prefs;
    private SharedPreferences sensor_prefs;

    private boolean counting = false;
    private Long firstStop = null;
    private int count = 0;

    private ITripDetectionCallback callback;

    public TripEndDetector(ITripDetectionCallback callback, Context c) {
        setting_prefs = c.getSharedPreferences(c.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = c.getSharedPreferences(c.getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        this.callback = callback;
    }


    public void checkForTripEnd(DataVector dv) {
        if(++count < 10)
            return;

        boolean obd_active = Preferences.OBDActivated(sensor_prefs);
        if(obd_active && OBD2Connection.sock.isConnected())
            checkInOBD(dv);

        boolean location_active = Preferences.locationActivated(sensor_prefs);
        if(location_active && dv.lat != 0 && dv.lon != 0)
            checkInSpeed(dv);

    }

    private void checkInOBD(DataVector dv) {
        if(dv.rpm == 0) {
            Log.d("TRIP END","Trip end in OBD");
            callback.onTripEnd();
        }
    }

    public void reset() {
        counting = false;
        firstStop = null;
        count = 0;
    }

    private void checkInSpeed(DataVector dv) {
        if(dv.speed < 1) {
            if(!counting) {
                firstStop = dv.timestamp;
                counting = true;
            } else {
                long duration = dv.timestamp - firstStop;

                if(duration > 30000) {
                    Log.d("TRIP END","Trip end in Speed");
                    callback.onTripEnd();
                }
            }
        } else {
            reset();
        }
    }
}
