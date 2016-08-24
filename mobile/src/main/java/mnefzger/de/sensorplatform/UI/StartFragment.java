package mnefzger.de.sensorplatform.UI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import mnefzger.de.sensorplatform.MainActivity;
import mnefzger.de.sensorplatform.R;
import mnefzger.de.sensorplatform.SensorPlatformService;

public class StartFragment extends Fragment {


    Button newStudy;
    Button currentStudy;

    public StartFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_start, container, false);

        newStudy = (Button) v.findViewById(R.id.newStudyButton);
        currentStudy = (Button) v.findViewById(R.id.currentStudyButton);

        newStudy.setOnClickListener(newStudyButtonListener);
        currentStudy.setOnClickListener(currentStudyButtonListener);

        Log.d("START_FRAGMENT", "serviceRunning = " + SensorPlatformService.serviceRunning);
        if(SensorPlatformService.serviceRunning == false) {
            currentStudy.setEnabled(false);
        }

        return v;
    }

    View.OnClickListener newStudyButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToNewStudyFragment();
        }
    };

    View.OnClickListener currentStudyButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToAppFragment();
        }
    };

}
