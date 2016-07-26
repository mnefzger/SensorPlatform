package mnefzger.de.sensorplatform;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class SettingsFragment extends PreferenceFragment
        implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        addPreferencesFromResource(R.xml.preferences);
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        FloatingActionButton b = (FloatingActionButton) v.findViewById(R.id.start_button);
        b.setOnClickListener(this);

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if(prefs.getBoolean("front_active", true) == false && prefs.getBoolean("back_active", true) == false ) {
            findPreference("image_saving").setEnabled(false);
            findPreference("image_saving").setSelectable(false);
        }

        return v;
    }



    public void startApplication() {

        ((MainActivity) getActivity()).startMeasuring();
    }


    @Override
    public void onClick(View v) {
        startApplication();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("front_active") || key.equals("back_active")) {
            if(sharedPreferences.getBoolean("front_active", true) == false &&
                    sharedPreferences.getBoolean("back_active", true) == false ) {
                findPreference("image_saving").setEnabled(false);
            } else {
                findPreference("image_saving").setEnabled(true);
            }


        }
    }
}
