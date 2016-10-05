package mnefzger.de.sensorplatform.Core;


public class EventVector {

    public enum LEVEL {DEBUG, NORMAL, RISKY, DANGEROUS};

    private LEVEL level;

    private long timestamp;
    private String eventDescription;
    private double value;
    private double extraValue;
    private String videoFront;
    private String videoBack;

    public EventVector(LEVEL level, long time, String event, double value) {
        this.level = level;
        this.timestamp = time;
        this.eventDescription = event;
        this.value = value;
    }

    public EventVector(LEVEL level, long time, String event, double value, double extra) {
        this.level = level;
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

    public LEVEL getLevel() {
        return level;
    }

    public String getLevelString() {
        if(this.level == LEVEL.NORMAL)
            return "NORMAL";
        if(this.level == LEVEL.RISKY)
            return "RISKY";
        if(this.level == LEVEL.DANGEROUS)
            return "DANGEROUS";

        return "-";
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
        String level = getLevelString();
        return timestamp + ";" + level + ";" + eventDescription + ";" + value + ";" + extraValue + ";" + videoFront + ";" + videoBack;
    }

    public boolean isIncludedInLevel(int[] int_levels) {
        if(level == LEVEL.DEBUG || int_levels == null)
            return false;

        for(int i=0; i<int_levels.length; i++) {
            LEVEL l = EventVector.LEVEL.values()[int_levels[i]];
            if(level == l)
                return true;
        }

        return false;
    }
}
