package mnefzger.de.sensorplatform;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class PositionProvider extends DataProvider implements LocationListener{
    private LocationManager locationManager;
    private Context context;
    private ISensorCallback callback;
    private Location lastLocation;
    private double lastTimestamp = System.currentTimeMillis();
    private List<Double> lastSpeedValues = new ArrayList<>();

    private final int LOCATION_REFRESH_TIME = 0;
    private final int LOCATION_REFRESH_DISTANCE = 0;

    public PositionProvider(Activity app, SensorModule m) {
        verifyLocationPermissions(app);
        context = app;
        callback = (ISensorCallback) m;
    }

    public void start() {
        super.start();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, this);
        }
    }

    public void stop() {
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
        super.stop();
    }

    @Override
    public void onLocationChanged(Location location) {
        double speed = 0;
        double currentTime = System.currentTimeMillis();

        if(lastLocation != null) {
            double distance = MathFunctions.calculateDistance(location.getLatitude(), location.getLongitude(), lastLocation.getLatitude(), lastLocation.getLongitude());
            // time between updates in seconds
            double timeDelta = (currentTime-lastTimestamp) / 1000.0;
            // speed in m/s
            speed = distance / timeDelta;
            // speed in km/h
            speed = speed * 3.6;

            lastSpeedValues.add(speed);
            if(lastSpeedValues.size() == 3) {
                speed = MathFunctions.getAccEMASingle(lastSpeedValues, 0.8);
                lastSpeedValues.set(lastSpeedValues.size()-1, speed);
            }


            if(lastSpeedValues.size() > 3) lastSpeedValues.remove(0);
        }
        lastLocation = location;
        lastTimestamp = currentTime;

        callback.onLocationData(location, speed);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private static final int REQUEST_LOCATION = 1;
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     * @param activity
     */
    public static void verifyLocationPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,
                    PERMISSIONS_LOCATION, REQUEST_LOCATION);
        }
    }


}
