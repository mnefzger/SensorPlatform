package mnefzger.de.sensorplatform.Core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

/**
 * An instance of this class monitors two sources to detect the start of a trip:
 * a. The "Significant Motion Sensor": >> A significant motion is a motion that might lead to a change in the user's location; for example walking, biking, or sitting in a moving car. <<
 * b. GPS location, queried every 5 seconds, checks if there was a significant change in location (> 25m since last location)
 */
public class TripStartDetector implements LocationListener {
    private ITripDetectionCallback callback;
    private Context context;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private LocationManager locationManager;
    final int LOCATION_REFRESH_TIME = 5000;
    final int LOCATION_REFRESH_DISTANCE = 10;

    private Location previous = null;
    private int checkCount = 0;

    public TripStartDetector(Context context, ITripDetectionCallback callback) {
        this.callback = callback;
        this.context = context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, this, Looper.getMainLooper());
        }

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        mSensorManager.requestTriggerSensor(significantMotionListener, mSensor);

        Log.d("TRIP START DETECTION", "Fired up");
    }

    public void cancel() {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            if(locationManager != null)
                locationManager.removeUpdates(this);
        }

        mSensorManager.cancelTriggerSensor(significantMotionListener, mSensor);
    }

    TriggerEventListener significantMotionListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            callback.onTripStart();
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        checkCount += 1;

        if(previous == null) {
            previous = location;
        } else {
            double distance = MathFunctions.calculateDistance(location.getLatitude(), location.getLongitude(), previous.getLatitude(), previous.getLongitude());
            Log.d("TRIP DETECTOR", "checking location ..." + distance);
            if(checkCount >= 3 && distance > 25) {
                callback.onTripStart();
            } else {
                previous = location;
            }
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
