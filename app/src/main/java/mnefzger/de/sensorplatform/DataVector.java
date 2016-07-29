package mnefzger.de.sensorplatform;

import android.location.Location;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.Instant;

/**
 * Created by matthias on 20/06/16.
 */
public class DataVector {
    public long timestamp;
    public String dateTime;
    /**
     * Three axis acceleration
     */
    public double accX;
    public double accY;
    public double accZ;
    /**
     * Three axis acceleration
     */
    public double rotX;
    public double rotY;
    public double rotZ;
    public float[] rotMatrix;
    /**
     * vehicle location in lat, lon
     */
    public Location location;
    /**
     * vehicle speed in km/h
     */
    public double speed;

    public double obdSpeed;

    public double rpm;

    public DataVector() {
        setDateTime();
    }

    public void setTimestamp(long time) {
        this.timestamp = time;
    }

    public void setDateTime() {
        this.dateTime = Instant.now().toString();
    }

    public void setAcc(double x, double y, double z) {
        this.accX = x;
        this.accY = y;
        this.accZ = z;
    }

    public void setRot(double x, double y, double z) {
        this.rotX = x;
        this.rotY = y;
        this.rotZ = z;
    }

    public void setRotMatrix(float[] matrix) {
        this.rotMatrix = matrix;
    }

    public void setLocation(Location l) {
        this.location = l;
    }

    public void setSpeed(double s) {
        this.speed = s;
    }

    public void setOBDSpeed(double s) {
        this.obdSpeed = s;
    }

    public void setRPM(double r) {
        this.rpm = r;
    }

    @Override
    public String toString() {
        return "time: " + timestamp + ", date: " + dateTime + ", accX: " + accX + ", accY: " + accY + ", accZ: " + accZ +
                ", rotX: " + rotX + ", rotY: " + rotY + ", rotZ: " + rotZ +", speed: " + speed +
                ", OBD speed: " + obdSpeed + ", rpm: " + rpm;
    }

    public String toCSVString() {
        double lat = (location == null) ? 0 : location.getLatitude();
        double lon = (location == null) ? 0 : location.getLongitude();
        return timestamp + ";" + dateTime + ";" + accX + ";" + accY + ";" + accZ + ";" +
                rotX + ";" + rotY + ";" + rotZ + ";" +
                lat + ";" + lon + ";" + speed + ";" + obdSpeed + ";" + rpm;
    }
}
