package mnefzger.de.sensorplatform.Core;


public class EventVector {

    private boolean debug;

    private long timestamp;
    private String eventDescription;
    private double value;
    private double extraValue;
    private String videoFront;
    private String videoBack;

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

    public void setVideoNames(long timestamp) {

        this.videoFront = "front-" + timestamp + ".avi";
        this.videoBack = "back-" + timestamp + ".avi";
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


    public String toString() {
        return "Timestamp: " + timestamp + " -> " + eventDescription + " @ " + value + ", extra: " + extraValue + ", video: " + videoFront + "," + videoBack;
    }

    public String toCSVString() {
        return timestamp + ";" + eventDescription + ";" + value + ";" + extraValue + ";" + videoFront + ";" + videoBack;
    }
}
