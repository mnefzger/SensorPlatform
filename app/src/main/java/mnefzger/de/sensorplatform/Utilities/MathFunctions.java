package mnefzger.de.sensorplatform.Utilities;

import android.util.Log;

import java.util.Iterator;
import java.util.List;

import mnefzger.de.sensorplatform.DataVector;

public class MathFunctions {

    /**
     * Calculates the Exponential Moving Average for the newest acceleration values based on the previous values
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

    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceInMeters = Math.round(6371000 * c);
        return distanceInMeters;
    }

}