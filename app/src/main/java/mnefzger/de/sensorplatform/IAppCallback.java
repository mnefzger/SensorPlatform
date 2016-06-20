package mnefzger.de.sensorplatform;

public interface IAppCallback {

    public void onRawData(DataVector dv);

    public void onEventData(EventVector ev);

}
