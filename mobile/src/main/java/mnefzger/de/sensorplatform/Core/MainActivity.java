package mnefzger.de.sensorplatform.Core;

import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.UI.AppFragment;
import mnefzger.de.sensorplatform.UI.OBDSetupFragment;
import mnefzger.de.sensorplatform.UI.SensorSetupFragment;
import mnefzger.de.sensorplatform.UI.SettingsFragment;
import mnefzger.de.sensorplatform.UI.SetupFirstFragment;
import mnefzger.de.sensorplatform.UI.StartFragment;
import mnefzger.de.sensorplatform.Utilities.PermissionManager;


public class MainActivity extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("imgProc");
        } catch (UnsatisfiedLinkError e) {
            Log.d("APPLICATION INIT", "Unsatisfied Link error: " + e.toString());
        }
    }

    StartFragment startFragment;
    SetupFirstFragment setupFragment;
    SensorSetupFragment sensorsFragment;
    OBDSetupFragment obdFragment;
    SettingsFragment settings;
    AppFragment appFragment;

    SensorPlatformService sPS;
    public static boolean mBound = false;
    public static boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionManager.verifyPermissions(this);

        setupPreferences();

        if(savedInstanceState == null) {
            Log.d("CREATE", "New activity");

            // bind and start service running in the background
            if(!checkIfServiceRunning()) {
                Log.d("CREATE", "Service not running, go to start");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                startService(intent);

                goToStartFragment(0);

            } else {
                started = true;

                Log.d("CREATE", "Service running, binding it and changing fragment");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                startService(intent);

                goToAppFragment();

            }

        } else {

            // if the data collection was already started, set reference to the UI fragment that shows live data
            started = savedInstanceState.getBoolean("started");

            if(!mBound) {
                Log.d("BINDING", "Rebinding service");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }

            Log.d("RECREATE", "started:"+started+", bound:"+mBound);

            if(started)
                goToAppFragment();
        }

    }

    // make sure the preference files are filled with the right values
    public void setupPreferences() {
        SharedPreferences sensor_prefs = this.getSharedPreferences(getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs.edit().clear();
        if(sensor_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE,  R.xml.sensor_preferences, true);
            sensor_prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE,  R.xml.sensor_preferences, false);
        }

        SharedPreferences setting_prefs = this.getSharedPreferences(getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        setting_prefs.edit().clear();
        if(setting_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.settings_preferences_key), Context.MODE_PRIVATE,  R.xml.settings_preferences, true);
            setting_prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.settings_preferences_key), Context.MODE_PRIVATE,  R.xml.settings_preferences, false);
        }

        SharedPreferences study_prefs = this.getSharedPreferences(getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        study_prefs.edit().clear();
        if(study_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.study_preferences_key), Context.MODE_PRIVATE,  R.xml.study_preferences, true);
            study_prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.study_preferences_key), Context.MODE_PRIVATE,  R.xml.study_preferences, false);
        }
    }

    public void startMeasuring() {
        goToAppFragment();

        Intent startIntent = new Intent(this, SensorPlatformService.class);
        startIntent.setAction("SERVICE_DATA_START");
        startService(startIntent);
        started = true;

        sPS.startWaitBehaviour();
        //sPS.subscribe();
    }

    private void doUnbindService() {
        try {
            unbindService(mConnection);
            mBound = false;
        } catch (Exception e) {
            Log.e("MAIN", e.toString());
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get SensorPlatformService instance
            SensorPlatformService.LocalBinder binder = (SensorPlatformService.LocalBinder) service;
            sPS = binder.getService();
            mBound = true;
            Log.d("SERVICE", "is bound: " + mBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public SensorPlatformService getService() {
        return sPS;
    }

    public boolean checkIfServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SensorPlatformService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d("SAVE", "saving state...");
        savedInstanceState.putBoolean("started", started);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, SensorPlatformService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();

        // don't forget to unregister the receiver
        if(OBD2Connection.connector != null)
            OBD2Connection.connector.unregisterReceiver();


        if(checkIfServiceRunning()) {
            doUnbindService();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(checkIfServiceRunning() && !started && !sPS.serviceRunning) {
            Intent intent = new Intent(this, SensorPlatformService.class);
            intent.setAction("SERVICE_STOP");
            stopService(intent);
            Log.d("On Destroy", "Killed Service");
        }
    }

    public void goToAppFragment() {
        this.appFragment = new AppFragment();
        changeFragment(this.appFragment, true, true);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    public void goToNewStudyFragment() {
        setupFragment = new SetupFirstFragment();
        changeFragment(setupFragment, true, true);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void goToSensorSetupFragment() {
        sensorsFragment = new SensorSetupFragment();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.slide_in_right_animator, R.animator.slide_out_left_animator);
        transaction.replace(R.id.fragment_container, sensorsFragment).commit();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void goToOBDSetupFragment() {
        obdFragment = new OBDSetupFragment();
        changeFragment(obdFragment, true, true);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void goToSettingsFragment() {
        settings = new SettingsFragment();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.slide_in_right_animator, R.animator.slide_out_left_animator);
        transaction.replace(R.id.fragment_container, settings).commit();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    MainActivity getActivity() {
        return this;
    }

    /**
     * Changing to the StartFragment allows for a short delay to allow the service to shut down completely
     */
    public void goToStartFragment(int ms) {
        startFragment = new StartFragment();

        Handler wait = new Handler();
        wait.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeFragment(startFragment, true, true);
                MainActivity that = getActivity();
                that.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }, ms);

    }

    private void changeFragment(Fragment frag, boolean saveInBackstack, boolean animate) {
        String backStateName = ((Object) frag).getClass().getName();

        try {
            FragmentManager manager = getSupportFragmentManager();
            boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

            if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) {
                Log.w("FRAGMENT", "Create new Fragment: " + backStateName + ", " + frag);
                //fragment not in back stack, create it.
                FragmentTransaction transaction = manager.beginTransaction();

                if (animate) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                }

                transaction.replace(R.id.fragment_container, frag, backStateName);

                if (saveInBackstack) {
                    transaction.addToBackStack(backStateName);
                } else {
                }

                transaction.commit();

            } else {
                Log.w("FRAGMENT", "Already instatiated, popped back: " + backStateName);
            }
        } catch (IllegalStateException exception) {
            Log.w("FRAGMENT", "Unable to commit fragment, could be activity has been killed in background. " + exception.toString());
        }
    }



}
