package mnefzger.de.sensorplatform.Core;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.Utilities.MathFunctions;

public class PositionProvider extends DataProvider implements LocationListener{
    private SharedPreferences setting_prefs;
    private LocationManager locationManager;
    private Context context;
    private ISensorCallback callback;
    private Location lastLocation;
    private double lastTimestamp = System.currentTimeMillis();
    private List<Double> lastSpeedValues = new ArrayList<>();

    private final int LOCATION_REFRESH_TIME;
    private final int LOCATION_REFRESH_DISTANCE = 0;

    public PositionProvider(Context app, ISensorCallback m) {
        context = app;
        callback = m;
        setting_prefs = app.getSharedPreferences(app.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);

        LOCATION_REFRESH_TIME = Preferences.getGPSRequestRate(setting_prefs);
    }

    @Override
    public void start() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, this);
        }
    }

    @Override
    public void stop() {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            if(locationManager != null)
                locationManager.removeUpdates(this);
        }
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
            if(lastSpeedValues.size() >= 5) {
                speed = MathFunctions.getAccEMASingle(lastSpeedValues, 0.7);
                lastSpeedValues.set(lastSpeedValues.size()-1, speed);
            }


            if(lastSpeedValues.size() > 5) lastSpeedValues.remove(0);
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


}
