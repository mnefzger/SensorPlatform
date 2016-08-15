package mnefzger.de.sensorplatform.External;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class FitnessSensorManager extends WearableListenerService {

    static final String TAG = "SENSORPLATFORM_FITNESS";
    private GoogleApiClient googleApiClient;

    public FitnessSensorManager(Context c) {
        this.googleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();

        connect();
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

    public void startMeasurement() {
        controlMeasurementInBackground("START_HEART");
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        Log.i(TAG, "Connected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.i(TAG, "Disconnected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged()");

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();

                Log.d(TAG, "Data changed: " + uri + ", " + path + ", " + dataItem.getData());
            }
        }
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

