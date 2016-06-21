package mnefzger.de.sensorplatform;


import java.util.List;

public abstract class EventProcessor {

    protected List<DataVector> data;
    protected IEventCallback callback;

    public EventProcessor(SensorModule m) {
        callback = m;
    }

    public void processData(List<DataVector> data) {
        this.data = data;
    }

}
