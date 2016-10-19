package au.carrsq.sensorplatform.Processors;


import android.util.Log;

import java.util.List;

import au.carrsq.sensorplatform.Core.DataVector;
import au.carrsq.sensorplatform.Core.IEventCallback;
import au.carrsq.sensorplatform.Core.SensorModule;

public abstract class EventProcessor {

    List<DataVector> data;
    IEventCallback callback;

    public EventProcessor(SensorModule m) {
        callback = m;
    }

    public void processData(List<DataVector> data) {
        this.data = data;
    }

    // TODO this method needs to be revisited, yields wrong results sometimes
    protected List<DataVector> getLastData(int ms) {
        if( !(data.get(0).timestamp == 0) && data.size() > 1 ) {

            // time between last and first entry
            long totalTime = data.get(data.size() - 1).timestamp - data.get(0).timestamp;

            // average time between each entry
            long deltaTime = totalTime / data.size();

            double samplingRate = 1000 / deltaTime;

            // in the timespan of ms, how many DataVectors were collected?
            int numberOfDataVectors = (int) Math.ceil(samplingRate * ms/1000);
            Log.d("NUMBER DATA VECTORS", numberOfDataVectors + "");

            int lastSamplingIndex = data.size() - numberOfDataVectors;

            lastSamplingIndex = lastSamplingIndex < 0 ? 0 : lastSamplingIndex;

            return data.subList(lastSamplingIndex, data.size());
        }

        return data;
    }

    protected List<DataVector> getLastDataItems(int number) {
        if( data.size() > number ) {
            return data.subList(data.size()-number, data.size());
        }

        return data;
    }

}
