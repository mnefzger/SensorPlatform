package mnefzger.de.sensorplatform.Utilities;

import android.hardware.SensorManager;

import java.util.Iterator;
import java.util.List;

public class MathFunctions {

    /**
     * Calculates the Exponential Moving Average for the newest acceleration values based on the previous values
     * with dynamic alpha value
     * @param buffer
     * @return
     */
    public static double[] getAccEMA(List<double[]> buffer) {
        ExponentialMovingAverage ema = new ExponentialMovingAverage(2.0 / (buffer.size() + 1) );
        Iterator<double[]> it = buffer.iterator();
        double[] result = new double[3];
        while(it.hasNext()) {
            double[] v = it.next();
            if(v != null) {
                result = ema.average(v);
            }
        }
        return result;
    }

    /**
     * Calculates the Exponential Moving Average for the newest acceleration values based on the previous values
     * with predefined alpha value
     * @param buffer
     * @return
     */
    public static double[] getAccEMA(List<double[]> buffer, double alpha) {
        ExponentialMovingAverage ema = new ExponentialMovingAverage( alpha );
        Iterator<double[]> it = buffer.iterator();
        double[] result = new double[3];
        while(it.hasNext()) {
            double[] v = it.next();
            if(v != null) {
                result = ema.average(v);
            }
        }
        return result;
    }

    /**
     * Returns the Euler representation of a quaternion rotation vector
     * @param values
     * @return
     */
    public static float[] calculateEulerAngles(float[] values) {
        float[] rMatrix = new float[9];
        float[] temp = new float[3];
        float[] result = new float[3];

        //calculate rotation matrix from rotation vector first
        SensorManager.getRotationMatrixFromVector(rMatrix, values);

        //calculate Euler angles now
        SensorManager.getOrientation(rMatrix, temp);

        //The results are in radians, need to convert it to degrees
        for (int i = 0; i < temp.length; i++){
            result[i] = Math.round(Math.toDegrees(temp[i]));
        }

        return result;
    }

    public static float[] calculateRadAngles(float[] values) {
        float[] rMatrix = new float[9];
        float[] result = new float[3];

        //calculate rotation matrix from rotation vector first
        SensorManager.getRotationMatrixFromVector(rMatrix, values);

        //calculate Euler angles now
        SensorManager.getOrientation(rMatrix, result);


        return result;
    }

    /**
     * Class representing the Exponential Moving Average
     */
    static class ExponentialMovingAverage {
        private double alpha;
        private double[] oldValue = new double[3];

        public ExponentialMovingAverage(double alpha) {
            this.alpha = alpha;
        }

        public double[] average(double[] values) {
            double[] newValue = new double[3];
            for(int dimension = 0; dimension < 3; dimension++) {
                if (oldValue[dimension] == 0.0) {
                    oldValue[dimension] = values[dimension];
                    return oldValue;
                }
                newValue[dimension] = alpha * newValue[dimension] + (1-alpha) * oldValue[dimension];
                oldValue[dimension] = newValue[dimension];
            }

            return newValue;
        }
    }

    /**
     * Returns the distance between two geopositions
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceInMeters = 6371000 * c;
        return distanceInMeters;
    }

}