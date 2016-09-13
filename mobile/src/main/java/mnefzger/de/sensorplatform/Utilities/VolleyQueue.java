package mnefzger.de.sensorplatform.Utilities;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

public class VolleyQueue {
    com.android.volley.RequestQueue queue;
    static VolleyQueue instance = null;

    public static VolleyQueue getInstance(Context c) {
        if(instance == null)
            return new VolleyQueue(c);
        else
            return instance;
    }

    private VolleyQueue(Context c) {
        // Instantiate the cache
        Cache cache = new DiskBasedCache(c.getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        queue = new RequestQueue(cache, network);
        queue.start();
    }

    public void add(StringRequest r) {
        queue.add(r);
    }
}
