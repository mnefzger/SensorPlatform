package mnefzger.de.sensorplatform.UI;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.R;

public class SensorSetupFragment extends PreferenceFragment implements View.OnClickListener{

    private Preference hiddenOBD;

    public SensorSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getActivity().getString(R.string.sensor_preferences_key));
        addPreferencesFromResource(R.xml.sensor_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sensor_setup, container, false);

        FrameLayout fl = (FrameLayout) v.findViewById(R.id.next_button);
        fl.setOnClickListener(this);

        // do not show obd option since obd comes in the next step of setup, but is still in the same preference file
        Preference obd = findPreference("obd_raw");
        hiddenOBD = obd;
        PreferenceScreen screen = getPreferenceScreen();
        screen.removePreference(obd);

        return v;
    }

    @Override
    public void onClick(View v) {
        PreferenceScreen screen = getPreferenceScreen();
        screen.addPreference(hiddenOBD);

        MainActivity app = (MainActivity) getActivity();
        app.goToOBDSetupFragment();
    }

}
