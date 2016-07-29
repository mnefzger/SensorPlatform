package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Iterator;

import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.External.OBD2Connector;
import mnefzger.de.sensorplatform.Logger.LoggingModule;

public class SensorPlatformController implements IDataCallback{
    private SharedPreferences prefs;
    private SensorModule sm;
    private LoggingModule lm;
    private ImageModule im;
    private IDataCallback appCallback;

    public SensorPlatformController(Activity app) {
        // needed for initialization of preference context
        Preferences.setContext(app);
        prefs = PreferenceManager.getDefaultSharedPreferences(app);

        this.sm = new SensorModule(this, app);
        this.lm = new LoggingModule(app);

        this.im = new ImageModule(this, app);
        this.appCallback = (IDataCallback) app;

        // start OBD connection setup
        if(Preferences.OBDActivated(prefs) && OBD2Connection.connected == false)
            new OBD2Connector(app);
    }

    public boolean subscribeTo(DataType type) {
        /**
         * If a subscription of the same type already exists, return
         */
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            if(it.next().getType() == type) {
                return false;
            }
        }

        Subscription s = new Subscription(type);

        if(type == DataType.CAMERA_RAW) {
            im.startCapture();
        } else {
            sm.startSensing(type);
        }

        ActiveSubscriptions.add(s);

        return true;
    }

    public boolean unsubscribe(DataType type) {
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            if(sub.getType() == type) {
                it.remove();
                ActiveSubscriptions.remove(sub);
                sm.StopSensing(type);
                return true;
            }
        }
        return false;
    }

    public void logRawData(boolean log) {
        ActiveSubscriptions.setLogRaw(log);
    }

    public void logEventData(boolean log) {
        ActiveSubscriptions.setLogEvent(log);
    }

    @Override
    public void onRawData(DataVector dv) {
        appCallback.onRawData(dv);
        if(ActiveSubscriptions.rawLoggingActive()) {
            lm.writeRawToCSV(dv);
        }
    }


    @Override
    public void onEventData(EventVector ev) {
        appCallback.onEventData(ev);
        if(ActiveSubscriptions.eventLoggingActive()) {
            lm.writeEventToCSV(ev);
        }

        if(Preferences.videoSavingActivated(prefs) && !ev.eventDescription.contains("Face")) {
            im.saveVideoAfterEvent(ev);
        }
    }

}
