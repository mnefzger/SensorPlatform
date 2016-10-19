package au.carrsq.sensorplatform.Core;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import au.carrsq.sensorplatform.Utilities.VolleyQueue;
import au.carrsq.sensorplatform.Utilities.WeatherResponse;

/**
 * Queries the current weather and reports back through the callback
 */
public class WeatherProvider extends DataProvider{
    private Context context;
    private ISensorCallback callback;
    private final int WEATHER_QUERY_DELAY = 300000;
    private final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(SELECT%20woeid%20FROM%20geo.places%20WHERE%20text%3D%22(LATITUDE%2CLONGITUDE)%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
    private boolean running = false;

    private Double currentLat = null;
    private Double currentLon = null;

    private boolean first = true;

    public WeatherProvider(Context c, SensorModule m) {
        this.context = c;
        this.callback = m;
        /*Location mock = new Location("mock");
        mock.setLongitude(153.015858);
        mock.setLatitude(-27.447945);
        current = mock;*/
    }

    @Override
    public void start() {
        Log.d("WEATHER", "Started Weather");
        running = true;
        // start periodic updates
        getWeatherPeriodically();
    }

    @Override
    public void stop() {
        running = false;
    }

    public void updateLocation(double lat, double lon) {
        this.currentLat = lat;
        this.currentLon = lon;
        if(first){
            //queryWeather();
            first = false;
        }
    }

    private void getWeatherPeriodically() {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if(currentLat != null) {
                            queryWeather();
                        }

                        if(running)
                           getWeatherPeriodically();
                    }
                },
                WEATHER_QUERY_DELAY
        );
    }

    private void queryWeather() {
        Log.d("WEATHER", "Queried Weather");

        String url = BASE_URL.replace("LATITUDE", currentLat + "");
        url = url.replace("LONGITUDE", currentLon + "");
        Log.d("VOLLEY", url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("WEATHER", "Trying to parse..." + response);
                        Gson gson = new Gson();
                        WeatherResponse weatherResponse = gson.fromJson(response, WeatherResponse.class);
                        Log.d("WEATHER", "Successfully parsed weather :((( ");
                        processResponse(weatherResponse);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY","Getting weather info failed.");
            }
        });

        VolleyQueue.getInstance(context).add(stringRequest);
    }

    private void processResponse(WeatherResponse res) {
        double tempC = (res.query.results.channel.item.condition.temp - 32) / 1.8; // in celsius
        String description = res.query.results.channel.item.condition.text;
        double windSpeed = res.query.results.channel.wind.speed * 1.609344; // in km/h
        if(running) callback.onWeatherData(tempC, description, windSpeed);
    }
}
