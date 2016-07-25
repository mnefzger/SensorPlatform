package mnefzger.de.sensorplatform;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class SettingsFragment extends PreferenceFragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        addPreferencesFromResource(R.xml.preferences);
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        FloatingActionButton b = (FloatingActionButton) v.findViewById(R.id.start_button);
        b.setOnClickListener(this);

        return v;
    }



    public void startApplication() {

        ((MainActivity) getActivity()).startMeasuring();
    }


    @Override
    public void onClick(View v) {
        startApplication();
    }
}
