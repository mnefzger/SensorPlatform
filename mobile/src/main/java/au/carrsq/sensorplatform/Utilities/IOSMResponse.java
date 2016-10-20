package au.carrsq.sensorplatform.Utilities;

public interface IOSMResponse {
    void onOSMRoadResponseReceived(OSMResponse response);
    void onOSMSpeedResponseReceived(OSMResponse response);
}
