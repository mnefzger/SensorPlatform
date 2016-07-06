package mnefzger.de.sensorplatform;


public class EventVector {

    private long timestamp;
    private String eventDescription;
    private double value;

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
