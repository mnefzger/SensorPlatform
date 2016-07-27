package mnefzger.de.sensorplatform;


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
    CAMERA_RAW
}
