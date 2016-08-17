package mnefzger.de.sensorplatform.External;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;


import java.util.List;

import mnefzger.de.sensorplatform.ISensorCallback;

public class FitnessSensorManager {

    private static FitnessSensorManager instance;
    private ISensorCallback callback;

    static final String TAG = "SENSORPLATFORM_FITNESS";
    private GoogleApiClient googleApiClient;

    private FitnessSensorManager(Context c) {
        this.googleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();

        connect();
    }

    public void setCallback(ISensorCallback callback) {
        this.callback = callback;
    }

    public static synchronized FitnessSensorManager getInstance(Context context) {
        if (instance == null) {
            instance = new FitnessSensorManager(context.getApplicationContext());
        }

        return instance;
    }

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectionResult result = googleApiClient.blockingConnect();
                Log.d("WEAR CONNECT", result.isSuccess()+"");
                startMeasurement();
            }
        }).start();
    }

    public void handleIncomingData(float heartbeat) {
        callback.onHeartData(heartbeat);
    }

    public void startMeasurement() {
        controlMeasurementInBackground("/start_heart");
    }

    public void stopMeasurement() {
        controlMeasurementInBackground("/stop_heart");
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
}

