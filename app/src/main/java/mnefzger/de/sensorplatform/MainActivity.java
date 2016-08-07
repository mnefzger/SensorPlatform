package mnefzger.de.sensorplatform;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jakewharton.threetenabp.AndroidThreeTen;

import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.External.OBD2Connector;
import mnefzger.de.sensorplatform.UI.AppFragment;
import mnefzger.de.sensorplatform.UI.SettingsFragment;


public class MainActivity extends AppCompatActivity implements IDataCallback{

    static {
        try {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("imgProc");
        } catch (UnsatisfiedLinkError e) {
            Log.d("APPLICATION INIT", "Unsatisfied Link error: " + e.toString());
        }
    }

    SettingsFragment settings;
    AppFragment appFragment;
    SharedPreferences prefs;
    SensorPlatformController sPC;
    boolean mBound = false;
    boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().clear();
        if(prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        }

        settings = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, settings).commit();

        // Backport of the new java8 time
        AndroidThreeTen.init(getApplication());

        // start OBD connection setup
        if( Preferences.OBDActivated(prefs) && OBD2Connection.connected == false )
            OBD2Connection.connector = new OBD2Connector(getApplicationContext());


        if(savedInstanceState == null) {
            // bind and start service running in the background
            Intent intent = new Intent(this, SensorPlatformController.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            startService(intent);
        } else {
            // if the data collection was already started, set reference to the right UI
            started = savedInstanceState.getBoolean("started");
            if(started) {
                String frag = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
                appFragment = (AppFragment) getSupportFragmentManager().findFragmentByTag(frag);
                Log.d("FRAGMENT", appFragment + "");
            }
        }
    }

    public void startMeasuring() {
        started = true;

        appFragment = new AppFragment();
        changeFragment(appFragment, true, true);

        sPC.subscribe();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @Override
    public void onRawData(DataVector v) {
        Log.d("RawData @ App  ", v.toString());
        if( appFragment != null && appFragment.isVisible())
            appFragment.updateUI(v);
    }

    @Override
    public void onEventData(EventVector v) {
        Log.d("EventData @ App  ", v.toString());
        if( appFragment != null && appFragment.isVisible())
            appFragment.updateUI(v);
    }

    IDataCallback getActivity() {
        return this;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get SensorPlatformController instance
            SensorPlatformController.LocalBinder binder = (SensorPlatformController.LocalBinder) service;
            sPC = binder.getService();
            mBound = true;
            sPC.setAppCallback(getActivity());
            Log.d("SERVICE", "is connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d("SAVE", "saving state...");
        savedInstanceState.putBoolean("started", started);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(OBD2Connection.connector != null)
            OBD2Connection.connector.unregisterReceiver();
        if(mBound) {
            try {
                unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }


    }

    private void changeFragment(Fragment frag, boolean saveInBackstack, boolean animate) {
        String backStateName = ((Object) frag).getClass().getName();

        try {
            FragmentManager manager = getSupportFragmentManager();
            boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

            if (!fragmentPopped && manager.findFragmentByTag(backStateName) == null) {
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
                // custom effect if fragment is already instanciated
            }
        } catch (IllegalStateException exception) {
            Log.w("FRAGMENT", "Unable to commit fragment, could be activity as been killed in background. " + exception.toString());
        }
    }

}
