package au.carrsq.sensorplatform.UI;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import au.carrsq.sensorplatform.Core.MainActivity;
import au.carrsq.sensorplatform.External.BluetoothManager;
import au.carrsq.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SecondPhoneSetupFragment extends Fragment {

    AppCompatCheckBox phoneActive;
    TextView hint;
    LinearLayout phone_setup_details;
    LinearLayout instruction_layout;
    LinearLayout connection_layout;

    TextView instruction;
    TextView connecting;

    ImageView phones;
    ImageView done;

    FrameLayout setup_next;

    boolean receiverRegistered = false;

    public SecondPhoneSetupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_second_phone_setup, container, false);

        instruction = (TextView) v.findViewById(R.id.instructionPhoneText);

        phoneActive = (AppCompatCheckBox) v.findViewById(R.id.phone_checkbox);
        hint = (TextView) v.findViewById(R.id.phone_hint);
        phone_setup_details = (LinearLayout) v.findViewById(R.id.phone_setup_details);
        connection_layout = (LinearLayout) v.findViewById(R.id.connection_layout);
        instruction_layout = (LinearLayout) v.findViewById(R.id.instruction_layout);

        connecting = (TextView) v.findViewById(R.id.connectingPhoneText);

        phones = (ImageView) v.findViewById(R.id.phone_figure);
        done = (ImageView) v.findViewById(R.id.done_phone);

        phoneActive.setOnClickListener(listener);

        setup_next = (FrameLayout) v.findViewById(R.id.next_button);
        setup_next.setOnClickListener(nextStepButtonListener);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForEstablishedConnection();
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

            if(checked) {
                hint.animate().alpha(0).setDuration(500);
                hint.setVisibility(View.INVISIBLE);
                phone_setup_details.animate().alpha(1).setDuration(500);
                phone_setup_details.setVisibility(View.VISIBLE);
                String name = BluetoothAdapter.getDefaultAdapter().getName();
                if(!instruction.getText().toString().contains(name))
                    instruction.append(name);

                app.getService().initiatePhoneConnection();

                IntentFilter filter = new IntentFilter();
                filter.addAction("PHONE_FOUND");
                filter.addAction("PHONE_CONNECTED");
                filter.addAction("PHONE_NOT_FOUND");
                app.registerReceiver(mReceiver, filter);
                receiverRegistered = true;
            } else {
                app.getService().cancelPhoneConnection();

                phone_setup_details.animate().alpha(0).setDuration(500);
                phone_setup_details.setVisibility(View.INVISIBLE);
                hint.setVisibility(View.VISIBLE);
                hint.animate().alpha(1).setDuration(500);

                resetText();

                if(receiverRegistered)
                    app.unregisterReceiver(mReceiver);
            }
        }
    };

    private void checkForEstablishedConnection() {
        MainActivity app = (MainActivity)getActivity();
        Log.d("Connection check", "...");
        if(app.getService().isPhoneConnected()) {
            phoneActive.setChecked(true);
            connectionComplete();
            Log.d("Connection check", "Connected");
        }
    }

    private void resetText() {
        instruction_layout.setAlpha(1);
        instruction_layout.setVisibility(View.VISIBLE);
        connection_layout.setAlpha(0);
        connection_layout.setVisibility(View.GONE);
        phones.setImageResource(R.drawable.phone_switch);
    }

    private void searchComplete() {
        // intermediate step...
    }

    private void connectionComplete() {
        hint.setVisibility(View.INVISIBLE);
        phone_setup_details.setVisibility(View.VISIBLE);
        phones.animate().alpha(0).setDuration(500);
        phones.setImageResource(R.drawable.phone_switch_complete);
        instruction_layout.animate().alpha(0).setDuration(500);
        instruction_layout.setVisibility(View.GONE);
        connection_layout.setVisibility(View.VISIBLE);
        phones.animate().alpha(1).setDuration(750);
        connection_layout.animate().alpha(1).setDuration(750);


    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals("PHONE_FOUND")) {
                searchComplete();

            } else if(action.equals("PHONE_CONNECTED")) {
                connectionComplete();
                MainActivity app = (MainActivity) getActivity();
                if(app != null) {
                    app.unregisterReceiver(mReceiver);
                    receiverRegistered = false;
                }


            } else if(action.equals("PHONE_NOT_FOUND")) {
                //notFound();
            }
        }
    };

}
