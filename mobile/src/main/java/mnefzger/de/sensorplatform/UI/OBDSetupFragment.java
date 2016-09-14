package mnefzger.de.sensorplatform.UI;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.Core.Preferences;
import mnefzger.de.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OBDSetupFragment extends Fragment {

    AppCompatCheckBox obdActive;
    TextView hint;
    LinearLayout obd_setup_details;

    TextView searching;
    TextView connecting;
    TextView setup;
    TextView ready;

    FrameLayout setup_next;

    boolean receiverRegistered = false;

    public OBDSetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_obd_setup, container, false);

        obdActive = (AppCompatCheckBox) v.findViewById(R.id.obd_checkbox);
        hint = (TextView) v.findViewById(R.id.obd_hint);
        obd_setup_details = (LinearLayout) v.findViewById(R.id.obd_setup_details);

        searching = (TextView) v.findViewById(R.id.searchingOBDText);
        connecting = (TextView) v.findViewById(R.id.connectingOBDText);
        setup = (TextView) v.findViewById(R.id.setupOBDText);
        ready = (TextView) v.findViewById(R.id.readyOBDText);

        obdActive.setOnClickListener(listener);


        setup_next = (FrameLayout) v.findViewById(R.id.next_button);
        setup_next.setOnClickListener(nextStepButtonListener);

        return v;
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = obdActive.isChecked();
            MainActivity app = (MainActivity) getActivity();
            SharedPreferences sensor_prefs = getActivity().getSharedPreferences(getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sensor_prefs.edit();

            if(checked) {
                editor.putBoolean("obd_raw", true);
                editor.apply();
                hint.setVisibility(View.INVISIBLE);
                obd_setup_details.setVisibility(View.VISIBLE);

                Log.d("OBD setup", Preferences.OBDActivated(sensor_prefs) +", "+sensor_prefs.getBoolean("obd_raw", false));

                app.getService().initiateOBDConnection();

                IntentFilter filter = new IntentFilter();
                filter.addAction("OBD_FOUND");
                filter.addAction("OBD_CONNECTED");
                filter.addAction("OBD_SETUP_COMPLETE");
                filter.addAction("OBD_NOT_FOUND");
                app.registerReceiver(mReceiver, filter);
                receiverRegistered = true;
            } else {
                editor.putBoolean("obd_raw", false);
                editor.apply();
                obd_setup_details.setVisibility(View.INVISIBLE);
                hint.setVisibility(View.VISIBLE);
                resetText();

                if(receiverRegistered)
                    app.unregisterReceiver(mReceiver);
            }
        }
    };

    View.OnClickListener nextStepButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToPhoneSetupFragment();
        }
    };

    public void searchComplete() {
        searching.setText("Looking for nearby OBD-II … Done.");
        connecting.setText("Connecting to OBD-II …");
    }

    public void connectionComplete() {
        connecting.setText("Connecting to OBD-II … Done.");
        setup.setText("Setting up …");
    }

    public void setupComplete() {
        setup.setText("Setting up … Done.");
        readyComplete();
    }

    public void notFound() {
        searching.setText("Seems like there is no OBD-II adapter in the car, are you sure it is plugged in and running?\nTo try again, deactivate this setting and activate it again.");
    }



    public void readyComplete() {
        ready.setText("Ready.");
    }

    private void resetText() {
        searching.setText("Looking for nearby OBD-II …");
        connecting.setText("");
        setup.setText("");
        ready.setText("");
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals("OBD_FOUND")) {
                searchComplete();

            } else if(action.equals("OBD_CONNECTED")) {
                connectionComplete();

            } else if(action.equals("OBD_SETUP_COMPLETE")) {
                setupComplete();
                // we are finished here, unregister receiver
                MainActivity app = (MainActivity) getActivity();
                app.unregisterReceiver(mReceiver);
            } else if(action.equals("OBD_NOT_FOUND")) {
                notFound();
            }
        }
    };
}
