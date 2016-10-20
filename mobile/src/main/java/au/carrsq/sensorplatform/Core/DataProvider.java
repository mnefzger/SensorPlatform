package au.carrsq.sensorplatform.Core;

/**
 * Abstract base class for any provider class that collects data
 */
public abstract class DataProvider {

    public abstract void start();

    public abstract void stop();
}
