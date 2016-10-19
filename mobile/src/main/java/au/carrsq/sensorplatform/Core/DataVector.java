package au.carrsq.sensorplatform.Core;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

public class DataVector {
    /**
     * Study parameters
     */
    private String study_id;
    private String study_name;
    private String participant_id;
    private int participant_age;
    private String participant_gender;

    /**
     * Data about time
     */
    public long timestamp;
    public String dateTime;

    /**
     * Three axis acceleration
     */
    public double accX;
    public double accY;
    public double accZ;

    /**
     * Three axis rotation in euler, matrix, rad/s
     */
    public double rotX;
    public double rotY;
    public double rotZ;
    public float[] rotMatrix;
    public double rotX_rad;
    public double rotY_rad;
    public double rotZ_rad;

    /**
     * Light level in cabin in lux
     */
    public double light;

    /**
     * vehicle location in lat, lon
     */
    public double lat;
    public double lon;

    /**
     * vehicle speed in km/h
     */
    public Double speed = -1.0;

    public String weather;

    /**
     * OBD data values
     */
    public Double obdSpeed = -1.0;
    public Double rpm = -1.0;
    public Double fuel = -1.0;

    /**
     * Heart rate coming from Android Wear device
     */
    public Double heartRate = -1.0;


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

    public void setStudyParams(String s_id, String s_name, String p_id, int p_age, String p_gender) {
        this.study_id = s_id;
        this.study_name = s_name;
        this.participant_id = p_id;
        this.participant_age = p_age;
        this.participant_gender = p_gender;
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

    public void setRotRad(double x, double y, double z) {
        this.rotX_rad = x;
        this.rotY_rad = y;
        this.rotZ_rad = z;
    }

    public void setLight(double light) {
        this.light = light;
    }

    public void setLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public void setSpeed(Double s) {
        this.speed = s;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public void setOBDSpeed(Double s) {
        this.obdSpeed = s;
    }

    public void setRPM(Double r) {
        this.rpm = r;
    }

    public void setFuel(Double f) {
        this.fuel = f;
    }

    public void setHeartRate(Double heartRate) {
        this.heartRate = heartRate;
    }

    @Override
    public String toString() {
        return "time: " + timestamp + ", date: " + dateTime + ", accX: " + accX + ", accY: " + accY + ", accZ: " + accZ +
                ", rotX: " + rotX_rad + ", rotY: " + rotY_rad + ", rotZ: " + rotZ_rad + ", light: " + light + ", speed: " + speed +
                ", OBD speed: " + obdSpeed + ", rpm: " + rpm + ", fuel: " + fuel + ", heart rate: " + heartRate + ", weather: " + weather;
    }

    public String toCSVString() {
        return study_id + ";" + study_name + ";" + participant_id + ";" + participant_age + ";" + participant_gender + ";" +
                timestamp + ";" + dateTime + ";" + accX + ";" + accY + ";" + accZ + ";" +
                rotX_rad + ";" + rotY_rad + ";" + rotZ_rad + ";" + light + ";" +
                lat + ";" + lon + ";" + speed + ";" + obdSpeed + ";" + rpm + ";" + fuel + ";" + heartRate + ";" + weather;
    }

}
