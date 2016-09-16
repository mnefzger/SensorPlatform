package mnefzger.de.sensorplatform.UI;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.R;


public class SettingsFragment extends PreferenceFragment
        implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    TextView topbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getActivity().getString(R.string.settings_preferences_key));
        addPreferencesFromResource(R.xml.settings_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout fl = (FrameLayout) v.findViewById(R.id.start_button);
        fl.setOnClickListener(this);

        SharedPreferences setting_prefs = getActivity().getSharedPreferences(getActivity().getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        setting_prefs.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sensor_prefs = getActivity().getSharedPreferences(getActivity().getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        if(sensor_prefs.getBoolean("front_active", true) == false && sensor_prefs.getBoolean("back_active", true) == false ) {
            CheckBoxPreference box = (CheckBoxPreference) findPreference("image_saving");
            box.setChecked(false);
            box.setEnabled(false);
            box.setSelectable(false);
        }

        /*topbar = (TextView) v.findViewById(R.id.settings_topbar_text);
        SharedPreferences studyPrefs = getActivity().getSharedPreferences(getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        String studyName = studyPrefs.getString("study_name", "");
        topbar.setText("New Study: " + studyName);*/

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
                CheckBoxPreference box = (CheckBoxPreference) findPreference("image_saving");
                box.setChecked(false);
            } else {
                findPreference("image_saving").setEnabled(true);
                findPreference("image_saving").setSelectable(true);
            }
        } else if(key.equals("obd_raw")) {
            Log.d("OBD PREF", "Changed.");
            boolean obd = sharedPreferences.getBoolean("obd_raw", false);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("obd_raw", obd);
            editor.commit();
        }
    }
}
