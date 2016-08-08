package mnefzger.de.sensorplatform;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.util.Iterator;

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

    public SensorPlatformController(Context c, IDataCallback app) {
        setAppCallback(app);
        setup();
    }

    public void setAppCallback(IDataCallback app) {
        this.appCallback = app;
    }

    private void setup() {
        // Backport of the new java8 time
        AndroidThreeTen.init(getApplication());

        Preferences.setContext(getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        this.sm = new SensorModule(this, getApplication());
        this.lm = new LoggingModule(getApplication());

        this.im = new ImageModule(this, getApplication());
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

        if(Preferences.lightActivated(prefs)) {
            subscribeTo(DataType.LIGHT);
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
                sm.stopSensing(type);
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

        if(Preferences.videoSavingActivated(prefs) && /**TODO: remove this ->*/ !ev.eventDescription.contains("Face")) {
            im.saveVideoAfterEvent(ev);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            Log.d("INTENT", intent.getAction());
            if(intent.getAction().equals("SERVICE_STOP")) {
                stopService();
            }
        }

        Notification note = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Sensor Platform")
                .setContentText("Data collection running")
                .setSmallIcon(R.drawable.data_collection)
                .setWhen(System.currentTimeMillis())
                .addAction(getStopAction())
                .build();

        Intent i = new Intent(this, MainActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        note.flags|=Notification.FLAG_NO_CLEAR;

        startForeground(1337, note);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        setup();
    }

    @Override
    public void onDestroy() {
        Log.d("SENSOR PLATFORM", "Destroy Service");
        super.onDestroy();
    }

    public void stopService() {
        Log.d("SENSOR PLATFORM", "Stop Service");
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            sm.stopSensing(sub.getType());
        }

        ActiveSubscriptions.removeAll();
        stopForeground(true);
        stopSelf();
    }

    private NotificationCompat.Action getStopAction() {
        Intent stopIntent = new Intent(this, SensorPlatformController.class);
        stopIntent.setAction("SERVICE_STOP");

        PendingIntent p = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Action stop = new NotificationCompat.Action.Builder(R.drawable.data_collection, "Stop", p).build();

        return stop;
    }

}
