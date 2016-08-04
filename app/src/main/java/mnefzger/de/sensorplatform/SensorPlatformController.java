package mnefzger.de.sensorplatform;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Iterator;

import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.External.OBD2Connector;
import mnefzger.de.sensorplatform.Logger.LoggingModule;

public class SensorPlatformController extends Service implements IDataCallback{
    private SharedPreferences prefs;
    private SensorModule sm;
    private LoggingModule lm;
    private ImageModule im;
    private IDataCallback appCallback;

    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SensorPlatformController getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorPlatformController.this;
        }
    }

    public SensorPlatformController() {}

    public void setAppCallback(IDataCallback app) {
        this.appCallback = app;
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
        if(appCallback != null)
            appCallback.onRawData(dv);
        //Log.d("RawData @ Service", dv.toString());
        if(ActiveSubscriptions.rawLoggingActive()) {
            lm.writeRawToCSV(dv);
        }
    }

    @Override
    public void onEventData(EventVector ev) {
        if(appCallback != null)
            appCallback.onEventData(ev);

        if(ActiveSubscriptions.eventLoggingActive()) {
            lm.writeEventToCSV(ev);
        }

        if(Preferences.videoSavingActivated(prefs) && !ev.eventDescription.contains("Face")) {
            im.saveVideoAfterEvent(ev);
        }
    }

    public void subscribe() {
        if(Preferences.accelerometerActivated(prefs)) {
            subscribeTo(DataType.ACCELERATION_RAW);
            subscribeTo(DataType.ACCELERATION_EVENT);
        }

        if(Preferences.rotationActivated(prefs)) {
            subscribeTo(DataType.ROTATION_RAW);
            subscribeTo(DataType.ROTATION_EVENT);
        }

        if(Preferences.locationActivated(prefs)) {
            subscribeTo(DataType.LOCATION_RAW);
            subscribeTo(DataType.LOCATION_EVENT);
        }

        if(Preferences.frontCameraActivated(prefs) || Preferences.backCameraActivated(prefs)) {
            subscribeTo(DataType.CAMERA_RAW);
        }

        if(Preferences.OBDActivated(prefs)) {
            subscribeTo(DataType.OBD);
        }

        if(Preferences.rawLoggingActivated(prefs)) {
            logRawData(true);
        }

        if(Preferences.eventLoggingActivated(prefs)) {
            logEventData(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification note = new Notification(R.drawable.inertial,
                "Data collection running.",
                System.currentTimeMillis());
        Intent i = new Intent(this, MainActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        note.flags|=Notification.FLAG_NO_CLEAR;

        startForeground(1337, note);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        Preferences.setContext(getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        this.sm = new SensorModule(this, getApplicationContext());
        this.lm = new LoggingModule(getApplicationContext());

        this.im = new ImageModule(this, getApplicationContext());
    }

    @Override
    public void onDestroy() {

    }
}
