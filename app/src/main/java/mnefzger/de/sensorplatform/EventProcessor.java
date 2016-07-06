package mnefzger.de.sensorplatform;


import android.util.Log;

import java.util.List;

public abstract class EventProcessor {

    List<DataVector> data;
    IEventCallback callback;

    public EventProcessor(SensorModule m) {
        callback = m;
    }

    public void processData(List<DataVector> data) {
        this.data = data;
    }

    public List<DataVector> getLastData(int ms) {
        if( !(data.get(0).timestamp == 0) && data.size() > 1 ) {
            // time between last and first entry
            long totalTime = data.get(data.size() - 1).timestamp - data.get(0).timestamp;
            //Log.d("SAMPLING total", totalTime + " ms");

            // average time between each entry
            long deltaTime = totalTime / data.size();
            //Log.d("SAMPLING delta", deltaTime + " ms");

            double samplingRate = 1000 / deltaTime;
            //Log.d("SAMPLING rate", samplingRate + " Hz");

            // in the timespan of ms, how many DataVectors were collected?
            int numberOfDataVectors = (int) Math.ceil(samplingRate * ms/1000);

            int lastSamplingIndex = data.size() - numberOfDataVectors;

            lastSamplingIndex = lastSamplingIndex < 0 ? 0 : lastSamplingIndex;

            return data.subList(lastSamplingIndex, data.size());
        }

        return data;
    }

}
