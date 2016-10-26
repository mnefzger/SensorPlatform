package au.carrsq.sensorplatform.Core;


public interface IEventCallback {
    void onEventDetected(EventVector v);

    void onEventDetectedWithoutTimestamp(EventVector v);
}
