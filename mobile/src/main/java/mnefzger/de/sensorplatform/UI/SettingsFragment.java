package mnefzger.de.sensorplatform.UI;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.jcodec.codecs.h264.io.model.Frame;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.R;


public class SettingsFragment extends PreferenceFragment
        implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    TextView topbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (container != null) {
            container.removeAllViews();
        }

        addPreferencesFromResource(R.xml.preferences);
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout fl = (FrameLayout) v.findViewById(R.id.start_button);
        fl.setOnClickListener(this);

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        SharedPreferences prefs = getActivity().getSharedPreferences(getActivity().getString(R.string.preferences_key), Context.MODE_PRIVATE);
        if(prefs.getBoolean("front_active", true) == false && prefs.getBoolean("back_active", true) == false ) {
            CheckBoxPreference box = (CheckBoxPreference) findPreference("image_saving");
            box.setChecked(false);
            box.setEnabled(false);
            box.setSelectable(false);
        }

        topbar = (TextView) v.findViewById(R.id.settings_topbar_text);
        SharedPreferences studyPrefs = getActivity().getSharedPreferences(getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        String studyName = studyPrefs.getString("study_name", "");
        topbar.setText("New Study: " + studyName);

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
        }
    }
}
