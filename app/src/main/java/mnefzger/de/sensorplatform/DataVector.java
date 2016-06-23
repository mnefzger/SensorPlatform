package mnefzger.de.sensorplatform;

/**
 * Created by matthias on 20/06/16.
 */
public class DataVector {
    public long timestamp;
    public double accX;
    public double accY;
    public double accZ;

    public DataVector() {

    }

    public void setTimestamp(long time) {
        this.timestamp = time;
    }

    public void setAcc(double x, double y, double z) {
        this.accX = x;
        this.accY = y;
        this.accZ = z;
    }

    @Override
    public String toString() {
        return "time: " + timestamp + ", accX: " + accX + ", accY: " + accY + ", accZ: " + accZ;
    }

    public String toCSVString() {
        return timestamp + ";" + accX + ";" + accY + ";" + accZ;
    }
}
