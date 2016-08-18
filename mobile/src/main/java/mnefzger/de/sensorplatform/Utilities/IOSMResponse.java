package mnefzger.de.sensorplatform.Utilities;

/**
 * Created by matthias on 14/07/16.
 */
public interface IOSMResponse {
    void onOSMRoadResponseReceived(OSMRespone response);
    void onOSMSpeedResponseReceived(OSMRespone response);
}
