package mnefzger.de.sensorplatform.UI;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mnefzger.de.sensorplatform.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class OBDSetupFragmen extends Fragment {


    public OBDSetupFragmen() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_obdsetup, container, false);
    }

}
