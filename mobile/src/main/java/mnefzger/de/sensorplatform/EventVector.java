package mnefzger.de.sensorplatform;


public class EventVector {

    private boolean debug;

    private long timestamp;
    private String eventDescription;
    private double value;
    private String videoName;

    public EventVector(boolean debug, long time, String event, double value) {
        this.debug = debug;
        this.timestamp = time;
        this.eventDescription = event;
        this.value = value;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public double getValue() {
        return value;
    }

    public String getVideoName() {
        return videoName;
    }


    public String toString() {
        return "Timestamp: " + timestamp + " -> " + eventDescription + " @ " + value;
    }

    public String toCSVString() {
        return timestamp + ";" + eventDescription + ";" + value;
    }
}
