package mnefzger.de.sensorplatform.Core;

public interface IDataCallback {

    void onRawData(DataVector dv);

    void onEventData(EventVector ev);
}
