package mnefzger.de.sensorplatform;

public class Subscription {
    private DataType type;
    private boolean log;

    public Subscription(DataType type, boolean log) {
        this.type = type;
        this.log = log;
    }

    public DataType getType() {
        return this.type;
    }

    public boolean includesLogging() {
        return log;
    }
}
