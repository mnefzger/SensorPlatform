package mnefzger.de.sensorplatform;

public interface IDataCallback {

    public void onRawData(DataVector dv);

    public void onEventData(EventVector ev);

}
