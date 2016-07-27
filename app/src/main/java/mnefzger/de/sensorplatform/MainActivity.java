package mnefzger.de.sensorplatform;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.R.anim;


public class MainActivity extends AppCompatActivity implements IDataCallback{

    static {
        try {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("imgProc");
        } catch (UnsatisfiedLinkError e) {
            Log.d("APPLICATION INIT", "Unsatisfied Link error: " + e.toString());
        }
    }

    SensorPlatformController sPC;
    SettingsFragment settings;
    AppFragment app;
    SharedPreferences prefs;

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

        sPC = new SensorPlatformController(this);

        settings = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, settings).commit();
        //hangeFragment(settings, false, true);
    }

    public void startMeasuring() {

        if(Preferences.accelerometerActivated(prefs)) {
            sPC.subscribeTo(DataType.ACCELERATION_RAW);
            sPC.subscribeTo(DataType.ACCELERATION_EVENT);
        }

        if(Preferences.rotationActivated(prefs)) {
            sPC.subscribeTo(DataType.ROTATION_RAW);
            sPC.subscribeTo(DataType.ROTATION_EVENT);
        }

        if(Preferences.locationActivated(prefs)) {
            sPC.subscribeTo(DataType.LOCATION_RAW);
            sPC.subscribeTo(DataType.LOCATION_EVENT);
        }

        if(Preferences.frontCameraActivated(prefs) || Preferences.backCameraActivated(prefs)) {
            sPC.subscribeTo(DataType.CAMERA_RAW);
        }

        sPC.logRawData(false);
        sPC.logEventData(false);


        app = new AppFragment();
        changeFragment(app, true, true);
    }


    @Override
    public void onRawData(DataVector v) {
        // Log.d("RawData @ App  ", v.toString());
        if( app != null && app.isVisible())
            app.updateUI(v);
    }

    @Override
    public void onEventData(EventVector v) {
        Log.d("EventData @ App  ", v.toString());
        if( app != null && app.isVisible())
            app.updateUI(v);
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
