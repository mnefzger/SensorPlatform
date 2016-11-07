package au.carrsq.sensorplatform.UI;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import au.carrsq.sensorplatform.Core.MainActivity;
import au.carrsq.sensorplatform.Core.Preferences;
import au.carrsq.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OBDSetupFragment extends Fragment {
    SharedPreferences sensor_prefs;

    AppCompatCheckBox obdActive;
    TextView hint;
    LinearLayout obd_setup_details, connection_layout;

    TextView searching;
    TextView connecting;
    TextView setup;
    TextView ready;

    ImageView done_searching, done_connect, done_setup, done_ready;

    FrameLayout setup_next;

    boolean receiverRegistered = false;

    public OBDSetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_obd_setup, container, false);

        sensor_prefs = getActivity().getSharedPreferences(getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        obdActive = (AppCompatCheckBox) v.findViewById(R.id.obd_checkbox);
        hint = (TextView) v.findViewById(R.id.obd_hint);
        obd_setup_details = (LinearLayout) v.findViewById(R.id.obd_setup_details);

        searching = (TextView) v.findViewById(R.id.searchingOBDText);
        connecting = (TextView) v.findViewById(R.id.connectingOBDText);
        setup = (TextView) v.findViewById(R.id.setupOBDText);
        //ready = (TextView) v.findViewById(R.id.readyOBDText);

        done_searching = (ImageView) v.findViewById(R.id.done_search);
        done_connect = (ImageView) v.findViewById(R.id.done_connect);
        done_setup = (ImageView) v.findViewById(R.id.done_setup);
        //done_ready = (ImageView) v.findViewById(R.id.done_ready);

        connection_layout = (LinearLayout) v.findViewById(R.id.connection_layout_obd);

        obdActive.setOnClickListener(listener);

        if(Preferences.OBDActivated(sensor_prefs))
            obdActive.setChecked(true);
        else
            obdActive.setChecked(false);

        obdActive.callOnClick();

        setup_next = (FrameLayout) v.findViewById(R.id.next_button);
        setup_next.setOnClickListener(nextStepButtonListener);

        return v;
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = obdActive.isChecked();
            MainActivity app = (MainActivity) getActivity();
            SharedPreferences.Editor editor = sensor_prefs.edit();

            if(checked) {
                editor.putBoolean("obd_raw", true);
                editor.apply();
                hint.setVisibility(View.INVISIBLE);
                obd_setup_details.setVisibility(View.VISIBLE);

                IntentFilter filter = new IntentFilter();
                filter.addAction("OBD_FOUND");
                filter.addAction("OBD_CONNECTED");
                filter.addAction("OBD_SETUP_COMPLETE");
                filter.addAction("OBD_NOT_FOUND");
                app.registerReceiver(mReceiver, filter);
                receiverRegistered = true;

                if(app.getService() != null)
                    app.getService().initiateOBDConnection();
            } else {
                editor.putBoolean("obd_raw", false);
                editor.apply();
                obd_setup_details.setVisibility(View.INVISIBLE);
                hint.setVisibility(View.VISIBLE);
                resetText();

                if(app!= null)
                    app.getService().cancelOBDConnection();

                if(receiverRegistered) {
                    app.unregisterReceiver(mReceiver);
                    receiverRegistered = false;
                }

            }
        }
    };

    View.OnClickListener nextStepButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToPhoneSetupFragment(true);
        }
    };

    public void searchComplete() {
        done_searching.setVisibility(View.VISIBLE);
        connecting.setText("Connecting to OBD-II …");
    }

    public void connectionComplete() {
        done_connect.setVisibility(View.VISIBLE);
        setup.setText("Setting up …");
    }

    public void setupComplete() {
        done_setup.setVisibility(View.VISIBLE);
        readyComplete();
    }

    public void notFound() {
        searching.setText("Seems like there is no OBD-II adapter in the car, are you sure it is plugged in and running?\nTo try again, deactivate this setting and activate it again.");
    }

    public void readyComplete() {
        obd_setup_details.animate().alpha(0).setDuration(500);
        obd_setup_details.setVisibility(View.GONE);
        connection_layout.setVisibility(View.VISIBLE);
        connection_layout.animate().alpha(1).setDuration(750);
        //done_ready.setVisibility(View.VISIBLE);
    }

    private void resetText() {
        searching.setText("Looking for nearby OBD-II …");
        connecting.setText("");
        setup.setText("");

        connection_layout.setAlpha(0);
        connection_layout.setVisibility(View.GONE);

        obd_setup_details.setAlpha(1);

        done_searching.setVisibility(View.INVISIBLE);
        done_connect.setVisibility(View.INVISIBLE);
        done_setup.setVisibility(View.INVISIBLE);
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
                receiverRegistered = false;
            } else if(action.equals("OBD_NOT_FOUND")) {
                notFound();
            }
        }
    };
}
