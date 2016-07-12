package mnefzger.de.sensorplatform.Utilities;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;


public class OSMQueryAdapter {
    Context context;

    public OSMQueryAdapter(Context context) {
        this.context = context;
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

    }

    public void startSearch(Location location) {
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED) {
            double lat_w = location.getLatitude() - 0.001;
            double lon_s = location.getLongitude() - 0.001;
            double lat_e = location.getLatitude() + 0.001;
            double lon_n = location.getLongitude() + 0.001;

            search(lat_w, lon_s, lat_e, lon_n);
        }
    }

    private void search(double lat_w, double lon_s, double lat_e, double lon_n) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url ="http://overpass-api.de/api/interpreter?data=[out:json][timeout:15];(";
        url += "way[\"maxspeed\"]";
        url += "("+ lat_w +"," + lon_s + "," + lat_e + "," + lon_n + ");";
        url += " <;);out body center qt 100;";

        Log.d("VOLLEY", url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new Gson();
                        OSMRespone osmR = gson.fromJson(response, OSMRespone.class);
                        for(OSMRespone.Element e : osmR.elements) {
                            Log.d("OSMResponse", e.tags.name + ", maxspeed: " + e.tags.maxspeed);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY","That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
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
