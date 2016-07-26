package mnefzger.de.sensorplatform;


public class EventVector {

    public long timestamp;
    public String eventDescription;
    public double value;
    public String camera;

    public EventVector(long time, String event, double value) {
        this.timestamp = time;
        this.eventDescription = event;
        this.value = value;
    }

    public String toString() {
        return "Timestamp: " + timestamp + " -> " + eventDescription + " @ " + value;
    }

    public String toCSVString() {
        return timestamp + ";" + eventDescription + ";" + value;
    }
}
