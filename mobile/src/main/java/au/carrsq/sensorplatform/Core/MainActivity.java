package au.carrsq.sensorplatform.Core;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import au.carrsq.sensorplatform.External.OBD2Connection;
import au.carrsq.sensorplatform.R;
import au.carrsq.sensorplatform.UI.AppFragment;
import au.carrsq.sensorplatform.UI.CameraPreviewFragment;
import au.carrsq.sensorplatform.UI.OBDSetupFragment;
import au.carrsq.sensorplatform.UI.SecondPhoneSetupFragment;
import au.carrsq.sensorplatform.UI.SensorSetupFragment;
import au.carrsq.sensorplatform.UI.SettingsFragment;
import au.carrsq.sensorplatform.UI.SetupFirstFragment;
import au.carrsq.sensorplatform.UI.StartFragment;
import au.carrsq.sensorplatform.UI.SurveyFragment;
import au.carrsq.sensorplatform.Utilities.PermissionManager;


public class MainActivity extends AppCompatActivity {

    /**
     * Load native libraries for OpenCV and image processing
     */
    static {
        try {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("imgProc");
        } catch (UnsatisfiedLinkError e) {
            Log.d("APPLICATION INIT", "Unsatisfied Link error: " + e.toString());
        }
    }

    /**
     * Fragments
     */
    StartFragment startFragment;
    SetupFirstFragment setupFragment;
    SensorSetupFragment sensorsFragment;
    OBDSetupFragment obdFragment;
    SecondPhoneSetupFragment phoneFragment;
    SettingsFragment settings;
    AppFragment appFragment;
    SurveyFragment surveyFragment;
    CameraPreviewFragment cameraFragment;
    private boolean inAppFragment, inSensorFragment, inSettingsFragment,
            inOBDFragment, inCameraFragment, inPhoneFragment = false;

    SensorPlatformService sPS;
    public static boolean mBound = false;
    public static boolean started = false;

    private boolean studySetupComplete = false;
    private boolean setupStarted = false;

    SharedPreferences sensor_prefs, setting_prefs, study_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // make sure all the required permissions are granted
        PermissionManager.verifyPermissions(this);

        // populate the preference files
        setupPreferences();

        // Check if this is the first start or just a re-instantiation
        if(savedInstanceState == null) {
            Log.d("CREATE", "New activity");

            // bind and start service running in the background
            if(!checkIfServiceRunning() || (!studySetupComplete && setupStarted) ) {
                Log.d("CREATE", "Service not running, go to start");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                startService(intent);

                goToStartFragment(0, true);

            } else if(checkIfServiceRunning() && (studySetupComplete || !setupStarted) ){ // new activity but service is already running
                started = true;

                Log.d("CREATE", "Service running, binding it and changing fragment");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                startService(intent);

                goToAppFragment();

            }

        } else { // activity was destroyed (e.g. for orientation change)
            started = savedInstanceState.getBoolean("started");

            if(!mBound) {
                Log.d("BINDING", "Rebinding service");
                Intent intent = new Intent(this, SensorPlatformService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }

            Log.d("RECREATE", "started:"+started+", bound:"+mBound);

            // if the data collection was already started, switch to fragment that shows live data
            if(started)
                goToAppFragment();
        }

    }

    // make sure the preference files are filled with the right values
    public void setupPreferences() {
        sensor_prefs = this.getSharedPreferences(getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
        sensor_prefs.edit().clear();
        if(sensor_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE,  R.xml.sensor_preferences, true);
            sensor_prefs.edit().apply();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE,  R.xml.sensor_preferences, false);
        }

        setting_prefs = this.getSharedPreferences(getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        setting_prefs.edit().clear();
        if(setting_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.settings_preferences_key), Context.MODE_PRIVATE,  R.xml.settings_preferences, true);
            setting_prefs.edit().apply();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.settings_preferences_key), Context.MODE_PRIVATE,  R.xml.settings_preferences, false);
        }

        study_prefs = this.getSharedPreferences(getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        study_prefs.edit().clear();
        if(study_prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, getString(R.string.study_preferences_key), Context.MODE_PRIVATE,  R.xml.study_preferences, true);
            study_prefs.edit().apply();
        } else {
            PreferenceManager.setDefaultValues(this, getString(R.string.study_preferences_key), Context.MODE_PRIVATE,  R.xml.study_preferences, false);
        }
    }

    /**
     *  changes to the live data fragment and notifies service to start data collection
     */
    public void startMeasuring() {
        goToAppFragment();

        Intent startIntent = new Intent(this, SensorPlatformService.class);
        startIntent.setAction("SERVICE_DATA_START");
        startService(startIntent);
        started = true;

        // trip start detection
        //sPS.startWaitBehaviour();
        // start data collection immediately
        sPS.subscribe();
    }

    private void doUnbindService() {
        try {
            unbindService(mConnection);
            mBound = false;
        } catch (Exception e) {
            Log.e("MAIN", e.toString());
        }
    }

    /**
     * The connection to the background service
     * Even if this connection disconnects, the service continues running
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get SensorPlatformService instance
            SensorPlatformService.LocalBinder binder = (SensorPlatformService.LocalBinder) service;
            sPS = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public ServiceConnection getConnection() {
        return mConnection;
    }

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
        // save whether the data collection was already started
        savedInstanceState.putBoolean("started", started);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d("OnNewIntent", intent.getAction());
    }

    /**
     * Allows to wake up phone and start activity at the end of a trip to show the survey.
     * The intent is triggered in the background service
     */
    private void handleSurveyIntent() {
        Intent intent = getIntent();
        if(intent.getAction() != null && intent.getAction().equals("au.carrsq.sensorplatform.survey")) {
            Log.d("INTENT", "Going to survey");
            goToSurveyFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /**
         * It's important that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, NfcAdapter.getDefaultAdapter(this));

        Intent intent = new Intent(this, SensorPlatformService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        handleSurveyIntent();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(checkIfServiceRunning()) {
            doUnbindService();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Only kill the service if data collection was not yet started
        if(!studySetupComplete && checkIfServiceRunning() && !started && !SensorPlatformService.serviceRunning) {
            Intent intent = new Intent(this, SensorPlatformService.class);
            intent.setAction("SERVICE_STOP");
            stopService(intent);
            Log.d("On Destroy", "Killed Service");
        }
    }

    @Override
    public void onBackPressed() {
        if(inAppFragment) {
            goToStartFragment(0, false);
            return;
        } else if(inSensorFragment) {
            goToNewStudyFragment(false);
            return;
        } else if(inSettingsFragment) {
            goToPhoneSetupFragment(false);
            return;
        } else if(inOBDFragment) {
            goToSensorSetupFragment(false);
            return;
        } else if (inPhoneFragment) {
            goToOBDSetupFragment(false);
            return;
        } else if(inCameraFragment) {
            goToSettingsFragment(false);
            return;
        }
        super.onBackPressed();
    }


    public void goToAppFragment() {
        this.appFragment = new AppFragment();
        changeFragment(this.appFragment, true, true, true);

        boolean reversed = Preferences.isReverseOrientation(setting_prefs);
        if(reversed)
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        else
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        inAppFragment = true;
        studySetupComplete = true;
    }

    public void goToNewStudyFragment(boolean forward) {
        setupFragment = new SetupFirstFragment();
        changeFragment(setupFragment, true, true, forward);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupStarted = true;
    }

    public void goToSensorSetupFragment(boolean forward) {
        sensorsFragment = new SensorSetupFragment();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(forward)
            transaction.setCustomAnimations(R.animator.slide_in_right_animator, R.animator.slide_out_left_animator);
        else
            transaction.setCustomAnimations(R.animator.slide_in_left_animator, R.animator.slide_out_right_animator);
        transaction.addToBackStack(sensorsFragment.getClass().getName());
        transaction.replace(R.id.fragment_container, sensorsFragment).commit();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        inSensorFragment = true;
    }

    public void goToOBDSetupFragment(boolean forward) {
        obdFragment = new OBDSetupFragment();
        changeFragment(obdFragment, true, true, forward);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        inOBDFragment = true;
    }

    public void goToPhoneSetupFragment(boolean forward) {
        phoneFragment = new SecondPhoneSetupFragment();
        changeFragment(phoneFragment, true, true, forward);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        inPhoneFragment = true;
    }

    public void goToSettingsFragment(boolean forward) {
        settings = new SettingsFragment();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(forward)
            transaction.setCustomAnimations(R.animator.slide_in_right_animator, R.animator.slide_out_left_animator);
        else
            transaction.setCustomAnimations(R.animator.slide_in_left_animator, R.animator.slide_out_right_animator);
        transaction.addToBackStack(settings.getClass().getName());
        transaction.replace(R.id.fragment_container, settings).commit();
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        inSettingsFragment = true;
    }

    public void goToCameraPreviewFragment(boolean forward) {
        cameraFragment = new CameraPreviewFragment();
        changeFragment(cameraFragment, true, true, forward);
        boolean reversed = Preferences.isReverseOrientation(setting_prefs);
        if(reversed)
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        else
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        inCameraFragment = true;
        studySetupComplete = true;
    }


    public void goToSurveyFragment() {
        surveyFragment = new SurveyFragment();
        changeFragment(surveyFragment, true, true, true);
        boolean reversed = Preferences.isReverseOrientation(setting_prefs);
        if(reversed)
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        else
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    MainActivity getActivity() {
        return this;
    }

    /**
     * Changing to the StartFragment allows for a short delay to allow the service to shut down completely
     */
    public void goToStartFragment(int ms, final boolean forward) {
        startFragment = new StartFragment();

        Handler wait = new Handler();
        wait.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeFragment(startFragment, true, true, forward);
                MainActivity that = getActivity();
                that.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }, ms);

    }

    private void changeFragment(Fragment frag, boolean saveInBackstack, boolean animate, boolean forward) {
        // reset any fragment booleans
        inAppFragment = false;
        inSensorFragment = false;
        inOBDFragment = false;
        inPhoneFragment = false;
        inSettingsFragment = false;
        inCameraFragment = false;

        String backStateName = ((Object) frag).getClass().getName();

        try {
            FragmentManager manager = getSupportFragmentManager();
            boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

            if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) {
                Log.w("FRAGMENT", "Create new Fragment: " + backStateName + ", " + frag);
                //fragment not in back stack, create it.
                FragmentTransaction transaction = manager.beginTransaction();

                if (animate) {
                    if(forward)
                        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                    else
                        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                }

                transaction.replace(R.id.fragment_container, frag, backStateName);

                if (saveInBackstack) {
                    transaction.addToBackStack(backStateName);
                }

                transaction.commit();

            } else {
                Log.w("FRAGMENT", "Already instantiated, popped back: " + backStateName);
                Fragment toShow = manager.findFragmentByTag(backStateName);
                /*FragmentTransaction transaction = manager.beginTransaction();
                if(forward)
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                else
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                transaction.show(toShow).commit();*/
                if(toShow.getView() != null)
                    toShow.getView().bringToFront();

            }
        } catch (IllegalStateException exception) {
            Log.w("FRAGMENT", "Unable to commit fragment, could be activity has been killed in background. " + exception.toString());
        }
    }

    /**
     * Start activity when the OS forwards an intent triggered by a NFC tag.
     * Prevents second activity to start if application is already running
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }



}
