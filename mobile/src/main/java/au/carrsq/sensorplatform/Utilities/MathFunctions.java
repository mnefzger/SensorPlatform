package au.carrsq.sensorplatform.Utilities;

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
     * Calculates the Exponential Moving Average for the newest acceleration values based on the previous values
     * with dynamic alpha value
     * @param buffer
     * @return
     */
    public static double getAccEMASingle(List<Double> buffer, double alpha) {
        ExponentialMovingAverage ema = new ExponentialMovingAverage( alpha );
        Iterator<Double> it = buffer.iterator();
        double result = 0;
        while(it.hasNext()) {
            double v = it.next();
            result = ema.average(v);
        }
        return result;
    }

    public static double[] getEMA(double[] current, double[] prev, double alpha) {
        double[] result = new double[current.length];

        for(int dim = 0; dim < current.length; dim++) {
            result[dim] = alpha*current[dim] + (1-alpha)*prev[dim];
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

    public static double dotProduct(double[] v1, double[] v2) {
        return v1[0]*v2[0] + v1[1]*v2[1];
    }

    public static double crossProduct(double[] v1, double[] v2) {
        return v1[0]*v2[1] - v1[1]*v2[0];
    }

    public static double cosVectors(double[] v1, double[] v2) {
        double cos = dotProduct(v1, v2) / ( (Math.sqrt(v1[0]*v1[0] + v1[1]*v1[1])) *
                (Math.sqrt(v2[0]*v2[0] + v2[1]*v2[1])) );
        return cos;
    }

    /**
     * Class representing the Exponential Moving Average
     */
    static class ExponentialMovingAverage {
        private double alpha;
        private double[] oldValue = new double[3];
        private double oldValueSingle = 0.0;

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
                newValue[dimension] = alpha * values[dimension] + (1-alpha) * oldValue[dimension];
                oldValue[dimension] = newValue[dimension];
            }

            return newValue;
        }

        public double average(double value) {
            double newValue;
            if (oldValueSingle == 0.0) {
                oldValueSingle = value;
                return oldValueSingle;
            }
            newValue = alpha * value + (1-alpha) * oldValueSingle;
            oldValueSingle = newValue;

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


    public static double calculateDistanceToLine(double lat1, double lon1, double lat2, double lon2, double lat3, double lon3) {
        double XX = lat2-lat1;
        double YY = lon2-lon1;

        double shortest = ((XX * (lat3-lat1)) + (YY * (lon3-lon1))) / ((XX * XX) + (YY * YY));
        double lat4 = lat1 + XX * shortest;
        double lon4 = lon1 + YY * shortest;

        double minimumDistance = calculateDistance(lat3, lon3, lat4, lon4);
        if(lat4 < lat2 && lat4 > lat1 && lon4 < lon2 && lon4 > lon1) {
            return minimumDistance;
        } else {
            double min1 = Math.min(calculateDistance(lat3, lon3, lat1, lon1), calculateDistance(lat3, lon3, lat2, lon2));
            return Math.min(min1, minimumDistance);
        }

    }

}