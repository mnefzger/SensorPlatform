package mnefzger.de.sensorplatform.Core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.Iterator;

import mnefzger.de.sensorplatform.External.UserPhoneBluetoothServer;
import mnefzger.de.sensorplatform.Logger.LoggingModule;
import mnefzger.de.sensorplatform.R;

/**
 * This class is the main service managing the data collection.
 * It provides functionality to start, stop, pause the service
 * as well as to (un)subscribe to specific data values or events.
 */

public class SensorPlatformService extends Service implements IDataCallback, ITripDetectionCallback{
    private SharedPreferences setting_prefs;
    private SharedPreferences sensor_prefs;

    private SensorModule sm;
    private LoggingModule lm;
    private ImageModule im;
    private UserPhoneBluetoothServer server;
    private TripStartDetector tsDetector;
    private TripEndDetector teDetector;

    private final IBinder mBinder = new LocalBinder();

    public static boolean serviceRunning = false;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SensorPlatformService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorPlatformService.this;
        }
    }

    public SensorPlatformService() {}

    public SensorPlatformService(Context c, IDataCallback app) {
        setup();
    }

    private void setup() {
        // Backport of the new java8 time
        AndroidThreeTen.init(getApplication());

        Preferences.setContext(getApplication());
        setting_prefs = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        this.sm = new SensorModule(this, getApplication());
        this.lm = LoggingModule.getInstance();

        this.im = new ImageModule(this, getApplication());
        this.server = new UserPhoneBluetoothServer(sm, getApplication());

        this.teDetector = new TripEndDetector(this, getApplication());

    }

    public void startWaitBehaviour() {
        tsDetector = new TripStartDetector(getApplication(), this);
        serviceRunning = true;
    }

    @Override
    public void onTripStart() {
        tsDetector.cancel();
        teDetector.reset();
        this.onEventData(new EventVector(true, System.currentTimeMillis(), "Trip Start detected", 0));
        //subscribe();
        prepareTripLogging();
        restartDataCollection();
    }

    @Override
    public void onTripEnd() {
        this.onEventData(new EventVector(true, System.currentTimeMillis(), "Trip End detected", 0));
        pauseDataCollection();
        //ActiveSubscriptions.removeAll();
        startWaitBehaviour();
    }

    private void prepareTripLogging() {
        lm.generateNewLoggingID();

        if(Preferences.rawLoggingActivated(setting_prefs)) {
            logRawData(true);
        }

        if(Preferences.eventLoggingActivated(setting_prefs)) {
            logEventData(true);
        }

        if(Preferences.rawLoggingActivated(setting_prefs) || Preferences.eventLoggingActivated(setting_prefs) ) {
            lm.createNewTripFileSet();
            sm.setStudyParameters();
        }
    }

    public void subscribe() {
        /**
         * Logging
         */
        prepareTripLogging();

        /**
         * Data Collection
         */
        if(Preferences.accelerometerActivated(sensor_prefs)) {
            subscribeTo(DataType.ACCELERATION_RAW);
            subscribeTo(DataType.ACCELERATION_EVENT);
        }

        if(Preferences.rotationActivated(sensor_prefs)) {
            subscribeTo(DataType.ROTATION_RAW);
            subscribeTo(DataType.ROTATION_EVENT);
        }

        if(Preferences.locationActivated(sensor_prefs)) {
            subscribeTo(DataType.LOCATION_RAW);
            subscribeTo(DataType.LOCATION_EVENT);
        }

        if(Preferences.lightActivated(sensor_prefs)) {
            subscribeTo(DataType.LIGHT);
        }

        if(Preferences.frontCameraActivated(sensor_prefs) || Preferences.backCameraActivated(sensor_prefs)) {
            subscribeTo(DataType.CAMERA_RAW);
        }

        if(Preferences.OBDActivated(sensor_prefs)) {
            subscribeTo(DataType.OBD);
        }

        if(Preferences.heartRateActivated(sensor_prefs)) {
            subscribeTo(DataType.HEART_RATE);
        }

        if(Preferences.weatherActivated(sensor_prefs)) {
            subscribeTo(DataType.WEATHER);
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
        teDetector.checkForTripEnd(dv);

        // the time-to-collision calculation needs info about the current speed
        if(Preferences.backCameraActivated(sensor_prefs) &&
                Preferences.backImagesProcessingActivated(setting_prefs))
            im.updateSpeed(dv.speed, dv.obdSpeed);

        Intent raw = new Intent("mnefzger.de.sensorplatform.RawData");
        raw.putExtra("RawData", new Gson().toJson(dv));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(raw);

        if(ActiveSubscriptions.rawLoggingActive()) {
            lm.writeRawToCSV(dv);
        }
    }

    long lastSave = System.currentTimeMillis();
    @Override
    public void onEventData(EventVector ev) {
        if(Preferences.videoSavingActivated(setting_prefs) && !ev.isDebug()) {
            long now = System.currentTimeMillis();

            // Check if a video is currently being saved or the last save was less than 4 second ago
            if(!im.isSaving() && (now - lastSave) > 4000) {
                im.saveVideoAfterEvent(ev);
                ev.setVideoName("Video-" + ev.getTimestamp() + ".avi");
                lastSave = now;
            }
        }

        if(ActiveSubscriptions.eventLoggingActive() && !ev.isDebug()) {
            lm.writeEventToCSV(ev);
        }

        Intent event = new Intent("mnefzger.de.sensorplatform.EventData");
        event.putExtra("EventData", new Gson().toJson(ev));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(event);

    }

    public void initiateOBDConnection() {
        this.sm.initiateOBDSetup();
    }

    public void initiatePhoneConnection() {
        this.server.setupServer();
    }

    public void cancelPhoneConnection() {
        this.server.cancel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String message = "Data Collection running.";
        if(intent != null && intent.getAction() != null) {
            Log.d("INTENT", intent.getAction());
            if(intent.getAction().equals("SERVICE_STOP")) {
                stopService();
                return START_NOT_STICKY;
            }
            if(intent.getAction().equals("SERVICE_PAUSE")) {
                pauseDataCollection();
                message = "Data Collection Paused.";
            }
            if(intent.getAction().equals("SERVICE_RESUME")) {
                restartDataCollection();
            }
            if(intent.getAction().equals("SERVICE_DATA_START")) {
                Notification note = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Sensor Platform")
                        .setContentText(message)
                        .setSmallIcon(R.drawable.data_collection)
                        .setWhen(System.currentTimeMillis())
                        .addAction(getStopAction())
                        .addAction(getPauseAction())
                        .addAction(getResumeAction())
                        .build();

                Intent i = new Intent(this, MainActivity.class);

                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                note.flags|=Notification.FLAG_NO_CLEAR;

                startForeground(1337, note);
            }
        }

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
        Log.d("SENSOR PLATFORM SERVICE", "Destroy Service");
        super.onDestroy();
    }

    public void stopService() {
        Log.d("SENSOR PLATFORM SERVICE", "Stop Service");
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            sm.stopSensing(sub.getType());
        }

        im.stopCapture();

        ActiveSubscriptions.removeAll();

        serviceRunning = false;
        stopForeground(true);
        stopSelf();
        // remove notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();

    }

    public void pauseDataCollection() {
        Log.d("SENSOR PLATFORM", "Pause Service");
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            sm.stopSensing(it.next().getType());
        }

        im.stopCapture();

        sm.clearDataBuffer();
    }

    public void restartDataCollection() {
        Iterator<Subscription> it = ActiveSubscriptions.get().iterator();
        while(it.hasNext()) {
            Subscription sub = it.next();
            sm.startSensing(sub.getType());
        }

        im.startCapture();
    }

    private NotificationCompat.Action getStopAction() {
        Intent stopIntent = new Intent(this, SensorPlatformService.class);
        stopIntent.setAction("SERVICE_STOP");

        PendingIntent p = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action.Builder(R.drawable.data_collection, "Stop", p).build();
    }

    private NotificationCompat.Action getPauseAction() {
        Intent pauseIntent = new Intent(this, SensorPlatformService.class);
        pauseIntent.setAction("SERVICE_PAUSE");

        PendingIntent p = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action.Builder(R.drawable.data_collection, "Pause", p).build();
    }

    private NotificationCompat.Action getResumeAction() {
        Intent resumeIntent = new Intent(this, SensorPlatformService.class);
        resumeIntent.setAction("SERVICE_RESUME");

        PendingIntent p = PendingIntent.getService(this, 0, resumeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action.Builder(R.drawable.data_collection, "Resume", p).build();
    }

}
