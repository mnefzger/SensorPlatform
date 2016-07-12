package mnefzger.de.sensorplatform.Utilities;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import hu.supercluster.overpasser.adapter.OverpassQueryResult;
import hu.supercluster.overpasser.adapter.OverpassServiceProvider;
import hu.supercluster.overpasser.library.output.OutputFormat;
import hu.supercluster.overpasser.library.query.OverpassQuery;

public class OSMQueryAdapter {
    Context context;

    public OSMQueryAdapter(Context context) {
        this.context = context;
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

    }

    public OverpassQueryResult startSearch(Location location) {
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            double lat_w = location.getLatitude() - 0.01;
            double lon_s = location.getLongitude() - 0.01;
            double lat_e = location.getLatitude() + 0.01;
            double lon_n = location.getLongitude() + 0.01;

            OverpassQueryResult s = search(lat_w, lon_s, lat_e, lon_n);
            Log.d("RESULT", "" + s);
            return  s;
        }

        return null;
    }

    private OverpassQueryResult search(double lat_w, double lon_s, double lat_e, double lon_n) {
        //Log.d("OVERPASS", "Starting query ... " + lat_w + ","+ lon_s);
        OverpassQuery query = new OverpassQuery()
                .format(OutputFormat.JSON)
                .timeout(3)
                .filterQuery()
                .node()
                .amenity("parking")
                .notEquals("access", "private")
                .boundingBox(
                        lat_w, lon_s,
                        lat_e, lon_n
                )
                .end()
                .output(100)
                ;

        return interpret(query.build());
    }

    private OverpassQueryResult interpret(String query) {
        try {
            return OverpassServiceProvider.get().interpreter(query).execute().body();

        } catch (Exception e) {
            e.printStackTrace();
            return new OverpassQueryResult();
        }
    }

    private static final int REQUEST_INTERNET = 2;
    private static String[] PERMISSIONS_INTERNET = {
            Manifest.permission.INTERNET
    };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     * @param activity
     */
    public static void verifyInternetPermission(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,
                    PERMISSIONS_INTERNET, REQUEST_INTERNET);
        }
    }
}
