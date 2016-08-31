package mnefzger.de.sensorplatform.Core;


public enum DataType {
    /**
     *
     */
    ACCELERATION_RAW,
    /**
     *
     */
    ACCELERATION_EVENT,
    /**
     *
     */
    ROTATION_RAW,
    /**
     *
     */
    ROTATION_EVENT,
    /**
     *
     */
    LIGHT,
    /**
     * includes lat, lon, current speed
     */
    LOCATION_RAW,
    /**
     * includes events of speeding
     */
    LOCATION_EVENT,
    /**
     *
     */
    CAMERA_RAW,
    /**
     *
     */
    WEATHER,
    /**
     *
     */
    OBD,
    /**
     *
     */
    HEART_RATE
}
