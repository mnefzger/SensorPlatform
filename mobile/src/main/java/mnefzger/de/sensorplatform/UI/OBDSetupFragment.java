package mnefzger.de.sensorplatform.UI;


import android.bluetooth.BluetoothDevice;
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
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.Core.Preferences;
import mnefzger.de.sensorplatform.External.OBD2Connection;
import mnefzger.de.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OBDSetupFragment extends Fragment {

    AppCompatCheckBox obdActive;
    LinearLayout obd_setup_details;

    TextView connecting;
    TextView setup;
    TextView ready;

    FrameLayout setup_next;

    public OBDSetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_obdsetup, container, false);

        obdActive = (AppCompatCheckBox) v.findViewById(R.id.obd_checkbox);
        obd_setup_details = (LinearLayout) v.findViewById(R.id.obd_setup_details);

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
            SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            if(checked) {
                editor.putBoolean("obd_raw", true);
                Log.d("PREFS", Preferences.OBDActivated(prefs)+"");
                obd_setup_details.setVisibility(View.VISIBLE);

                app.getService().initiateOBDConnection();

                IntentFilter filter = new IntentFilter();
                filter.addAction("OBD_CONNECTED");
                filter.addAction("OBD_SETUP_COMPLETE");
                app.registerReceiver(mReceiver, filter);
            } else {
                editor.putBoolean("obd_raw", false);
                obd_setup_details.setVisibility(View.INVISIBLE);
            }
            editor.commit();
        }
    };

    View.OnClickListener nextStepButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToSettingsFragment();
        }
    };

    public void connectionComplete() {
        connecting.setText("Connecting to OBD-II … Done.");
        setup.setText("Setting up …");
    }

    public void setupComplete() {
        setup.setText("Setting up … Done.");
        readyComplete();
    }

    public void readyComplete() {
        ready.setText("Ready.");
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals("OBD_CONNECTED")) {
                connectionComplete();
            } else if(action.equals("OBD_SETUP_COMPLETE")) {
                setupComplete();
                // we are finished here, unregister receiver
                MainActivity app = (MainActivity) getActivity();
                app.unregisterReceiver(mReceiver);
            }
        }
    };
}
