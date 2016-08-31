package mnefzger.de.sensorplatform.Core;

import android.location.Location;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

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
     * Light level in cabin in lux
     */
    public double light;
    /**
     * vehicle location in lat, lon
     */
    public Location location;
    /**
     * vehicle speed in km/h
     */
    public double speed;

    public String weather;

    public double obdSpeed;

    public double rpm;

    public double fuel;

    public double heartRate;

    public DataVector() {
        setDateTime();
    }

    public void setTimestamp(long time) {
        this.timestamp = time;
    }

    public void setDateTime() {
        Instant now = Instant.now();
        this.dateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault()).toString();
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

    public void setLight(double light) {
        this.light = light;
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

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public void setOBDSpeed(double s) {
        this.obdSpeed = s;
    }

    public void setRPM(double r) {
        this.rpm = r;
    }

    public void setFuel(double f) {
        this.fuel = f;
    }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    @Override
    public String toString() {
        return "time: " + timestamp + ", date: " + dateTime + ", accX: " + accX + ", accY: " + accY + ", accZ: " + accZ +
                ", rotX: " + rotX + ", rotY: " + rotY + ", rotZ: " + rotZ + ", light: " + light + ", speed: " + speed +
                ", OBD speed: " + obdSpeed + ", rpm: " + rpm + ", fuel: " + fuel + ", heart rate: " + heartRate + ", weather: " + weather;
    }

    public String toCSVString() {
        double lat = (location == null) ? 0 : location.getLatitude();
        double lon = (location == null) ? 0 : location.getLongitude();
        return timestamp + ";" + dateTime + ";" + accX + ";" + accY + ";" + accZ + ";" +
                rotX + ";" + rotY + ";" + rotZ + ";" + light + ";" +
                lat + ";" + lon + ";" + speed + ";" + obdSpeed + ";" + rpm + ";" + fuel + ";" + heartRate + ";" + weather;
    }



}
