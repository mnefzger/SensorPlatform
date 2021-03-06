package au.carrsq.sensorplatform.UI;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import au.carrsq.sensorplatform.Core.MainActivity;
import au.carrsq.sensorplatform.R;

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

        MainActivity app = (MainActivity)getActivity();
        if(app.started) {
            currentStudy.setEnabled(true);
            Log.d("START_FRAGMENT", "serviceRunning = true");
        }

        return v;
    }

    View.OnClickListener newStudyButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity app = (MainActivity) getActivity();
            app.goToNewStudyFragment(true);
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
