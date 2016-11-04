package au.carrsq.sensorplatform.External;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import au.carrsq.sensorplatform.Core.DataProvider;
import au.carrsq.sensorplatform.Core.ISensorCallback;

/**
 * Handles connection, start, stop of Android Wear device
 */
public class FitnessSensorManager extends DataProvider{

    private static FitnessSensorManager instance;
    private ISensorCallback callback;
    public static boolean wearAvailable = false;

    static final String TAG = "SENSORPLATFORM_FITNESS";
    private GoogleApiClient googleApiClient;

    private FitnessSensorManager(Context c) {
        this.googleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();

        connect();
    }

    public static synchronized FitnessSensorManager getInstance(Context context) {
        if (instance == null) {
            instance = new FitnessSensorManager(context.getApplicationContext());
        }

        return instance;
    }

    public void setCallback(ISensorCallback callback) {
        this.callback = callback;
    }

    private interface Callback {
        public void success(final String nodeId);
        public void failed();
    }

    private void connect() {
        retrieveDeviceNode(new Callback() {
            @Override
            public void success(String nodeId) {
                wearAvailable = true;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionResult result = googleApiClient.blockingConnect();
                        Log.d("WEAR CONNECT", result.isSuccess()+"");
                    }
                }).start();
            }

            @Override
            public void failed() {
               wearAvailable = false;
            }
        });
    }

    // check if a Android Wear watch is there and connected
    private void retrieveDeviceNode(final Callback callback) {
        final GoogleApiClient client = googleApiClient;
        new Thread(new Runnable() {

            @Override
            public void run() {
                client.blockingConnect(1000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    String nodeId = nodes.get(0).getId();
                    callback.success(nodeId);
                } else {
                    callback.failed();
                }
                client.disconnect();
            }
        }).start();
    }

    public void handleIncomingData(float heartbeat) {
        if(callback != null)
            callback.onHeartData(heartbeat);
    }

    public void startMeasurement() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground("/wearable/start_heart");
            }
        }).start();
    }

    public void stopMeasurement() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground("/wearable/stop_heart");
            }
        }).start();
    }

    private void controlMeasurementInBackground(final String path) {
        if (googleApiClient.isConnected()) {
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await().getNodes();

            Log.d(TAG, "Sending to nodes: " + nodes.size());

            for (Node node : nodes) {
                Log.i(TAG, "add node " + node.getDisplayName());
                Wearable.MessageApi.sendMessage(
                        googleApiClient, node.getId(), path, null
                ).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "controlMeasurementInBackground(" + path + "): " + sendMessageResult.getStatus().isSuccess());
                    }
                });
            }
        } else {
            Log.w(TAG, "No connection possible");
        }
    }

    @Override
    public void start() {
        if(googleApiClient.isConnected())
            startMeasurement();
        else {
            connect();
            startMeasurement();
        }

    }

    @Override
    public void stop() {
        stopMeasurement();
    }
}

