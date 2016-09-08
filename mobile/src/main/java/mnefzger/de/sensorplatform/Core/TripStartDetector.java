package mnefzger.de.sensorplatform.Core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;


public class TripStartDetector implements LocationListener {
    private ITripDetectionCallback callback;
    private LocationManager locationManager;
    private Context context;

    final int LOCATION_REFRESH_TIME = 5000;
    final int LOCATION_REFRESH_DISTANCE = 10;

    private Location previous = null;

    public TripStartDetector(Context context, ITripDetectionCallback callback) {
        this.callback = callback;
        this.context = context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, this);
        }

    }

    public void cancel() {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            if(locationManager != null)
                locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(previous == null) {
            previous = location;
        } else {
            double distance = MathFunctions.calculateDistance(location.getLatitude(), location.getLongitude(), previous.getLatitude(), previous.getLongitude());
            if(distance > 15) {
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
