package mnefzger.de.sensorplatform;


public class EventVector {

    private boolean debug;

    private long timestamp;
    private String eventDescription;
    private double value;
    private double extraValue;
    private String videoName;

    public EventVector(boolean debug, long time, String event, double value) {
        this.debug = debug;
        this.timestamp = time;
        this.eventDescription = event;
        this.value = value;
    }

    public EventVector(boolean debug, long time, String event, double value, double extra) {
        this.debug = debug;
        this.timestamp = time;
        this.eventDescription = event;
        this.value = value;
        this.extraValue = extra;
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

    public void setExtraValue(double extraValue) {
        this.extraValue = extraValue;
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

    public double getExtraValue() {
        return extraValue;
    }

    public String getVideoName() {
        return videoName;
    }


    public String toString() {
        return "Timestamp: " + timestamp + " -> " + eventDescription + " @ " + value + ", extra: " + extraValue + ", video: " + videoName;
    }

    public String toCSVString() {
        return timestamp + ";" + eventDescription + ";" + value + ";" + extraValue + ";" + videoName;
    }
}
