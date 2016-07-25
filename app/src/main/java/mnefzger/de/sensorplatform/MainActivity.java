package mnefzger.de.sensorplatform;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().clear();
        if(prefs.getAll().isEmpty()) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        }

        // Show settings
        settings = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, settings).commit();

    }

    public void startMeasuring() {
        sPC = new SensorPlatformController(this);
        sPC.subscribeTo(DataType.ACCELERATION_EVENT);
        sPC.subscribeTo(DataType.ACCELERATION_RAW);
        sPC.subscribeTo(DataType.LOCATION_RAW);
        sPC.subscribeTo(DataType.LOCATION_EVENT);
        sPC.subscribeTo(DataType.ROTATION_RAW);
        sPC.subscribeTo(DataType.ROTATION_EVENT);
        //sPC.subscribeTo(DataType.CAMERA_RAW);

        sPC.logRawData(false);
        sPC.logEventData(false);

        app = new AppFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, app).commit();
    }


    @Override
    public void onRawData(DataVector v) {
        //Log.d("RawData @ App  ", v.toString());
        if( app != null )
            app.updateUI(v);
    }

    @Override
    public void onEventData(EventVector v) {
        Log.d("EventData @ App  ", v.toString());
        if( app != null )
            app.updateUI(v);
    }

    @Override
    public void onImageData() {

    }

}
