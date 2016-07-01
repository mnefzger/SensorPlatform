package mnefzger.de.sensorplatform;

public interface IDataCallback {

    void onRawData(DataVector dv);

    void onEventData(EventVector ev);

    void onImageData();

}
