package mnefzger.de.sensorplatform;

public class Subscription {
    private DataType type;

    public Subscription(DataType type) {
        this.type = type;
    }

    public DataType getType() {
        return this.type;
    }

}
