package mnefzger.de.sensorplatform.UI;


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

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SecondPhoneSetupFragment extends Fragment {

    AppCompatCheckBox phoneActive;
    TextView hint;
    LinearLayout phone_setup_details;

    TextView waiting;
    TextView connecting;
    TextView ready;

    ImageView done_wait, done_phone_connect;

    FrameLayout setup_next;

    boolean receiverRegistered = false;

    public SecondPhoneSetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_second_phone_setup, container, false);

        phoneActive = (AppCompatCheckBox) v.findViewById(R.id.phone_checkbox);
        hint = (TextView) v.findViewById(R.id.phone_hint);
        phone_setup_details = (LinearLayout) v.findViewById(R.id.phone_setup_details);

        waiting = (TextView) v.findViewById(R.id.waitingPhoneText);
        connecting = (TextView) v.findViewById(R.id.connectingPhoneText);
        ready = (TextView) v.findViewById(R.id.readyPhoneText);

        done_wait = (ImageView)v.findViewById(R.id.done_wait);
        done_phone_connect = (ImageView) v.findViewById(R.id.done_phone_connect);

        phoneActive.setOnClickListener(listener);

        setup_next = (FrameLayout) v.findViewById(R.id.next_button);
        setup_next.setOnClickListener(nextStepButtonListener);

        return v;
    }

    View.OnClickListener nextStepButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToSettingsFragment(true);
        }
    };

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = phoneActive.isChecked();
            MainActivity app = (MainActivity) getActivity();
            SharedPreferences sensor_prefs = getActivity().getSharedPreferences(getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sensor_prefs.edit();

            if(checked) {
                hint.setVisibility(View.INVISIBLE);
                phone_setup_details.setVisibility(View.VISIBLE);

                app.getService().initiatePhoneConnection();

                IntentFilter filter = new IntentFilter();
                filter.addAction("PHONE_FOUND");
                filter.addAction("PHONE_CONNECTED");
                filter.addAction("PHONE_NOT_FOUND");
                app.registerReceiver(mReceiver, filter);
                receiverRegistered = true;
            } else {
                app.getService().cancelPhoneConnection();

                phone_setup_details.setVisibility(View.INVISIBLE);
                hint.setVisibility(View.VISIBLE);
                //resetText();

                if(receiverRegistered)
                    app.unregisterReceiver(mReceiver);
            }
        }
    };

    private void searchComplete() {
        done_wait.setVisibility(View.VISIBLE);
        connecting.setText("Trying to connect …");
    }

    private void connectionComplete() {
        done_phone_connect.setVisibility(View.VISIBLE);
        connecting.setText("Trying to connect …");
        ready.setText("Ready.");
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals("PHONE_FOUND")) {
                searchComplete();

            } else if(action.equals("PHONE_CONNECTED")) {
                connectionComplete();
                MainActivity app = (MainActivity) getActivity();
                app.unregisterReceiver(mReceiver);
                receiverRegistered = false;

            } else if(action.equals("PHONE_NOT_FOUND")) {
                //notFound();
            }
        }
    };

}
