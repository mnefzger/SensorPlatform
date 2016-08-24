package mnefzger.de.sensorplatform.UI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import org.w3c.dom.Text;

import mnefzger.de.sensorplatform.MainActivity;
import mnefzger.de.sensorplatform.R;


public class SetupFirstFragment extends Fragment {

    private Button setup_next;
    private TextInputLayout input_name, input_id, input_pid, input_age;
    private TextInputEditText study_name;
    private TextInputEditText study_ID;
    private TextInputEditText participant_ID;
    private TextInputEditText participant_age;
    private RadioButton female;
    private RadioButton male;

    String s_name;
    String s_id;
    String p_id;
    int p_age;

    public SetupFirstFragment() {
        // Required empty public constructor
    }

    public static SetupFirstFragment newInstance(String param1, String param2) {
        SetupFirstFragment fragment = new SetupFirstFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setup_first, container, false);

        input_name = (TextInputLayout) v.findViewById(R.id.input_layout_name);
        study_name = (TextInputEditText) v.findViewById(R.id.study_name);
        input_id = (TextInputLayout) v.findViewById(R.id.input_layout_sID);
        study_ID = (TextInputEditText) v.findViewById(R.id.study_id);
        input_pid = (TextInputLayout) v.findViewById(R.id.input_layout_pID);
        participant_ID = (TextInputEditText) v.findViewById(R.id.participant_id);
        input_age = (TextInputLayout) v.findViewById(R.id.input_layout_age);
        participant_age = (TextInputEditText) v.findViewById(R.id.participant_age);

        setup_next = (Button) v.findViewById(R.id.next_button);
        setup_next.setOnClickListener(nextStepButtonListener);

        return v;
    }

    View.OnClickListener nextStepButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean valid = validateInputs();
            if(valid) {
                writePreferences();
                MainActivity app = (MainActivity) getActivity();
                app.goToSettingsFragment();
            }
        }
    };

    private boolean validateInputs() {
        s_name = study_name.getText().toString();
        s_id = study_ID.getText().toString();
        p_id = participant_ID.getText().toString();
        String p_age_string = participant_age.getText().toString();

        if(s_name.trim().isEmpty()){
            input_name.setError("Please fill in a study name.");
            return false;
        } else {
            input_name.setErrorEnabled(false);
        }

        if(s_id.trim().isEmpty()){
            input_id.setError("Please fill in a study ID.");
            return false;
        } else {
            input_id.setErrorEnabled(false);
        }

        if(p_id.trim().isEmpty()){
            input_pid.setError("Please fill in a participant ID.");
            return false;
        } else {
            input_pid.setErrorEnabled(false);
        }

        if(p_age_string.trim().isEmpty()){
            input_age.setError("Please fill in the participant's age.");
            return false;
        } else {
            p_age = Integer.valueOf(p_age_string);
            input_age.setErrorEnabled(false);
        }

        return true;
    }

    private void writePreferences() {
        SharedPreferences studyPrefs = getActivity().getSharedPreferences(getString(R.string.study_preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = studyPrefs.edit();



        editor.putString("study_name", s_name);
        editor.putString("study_ID", s_id);
        editor.putString("p_ID", p_id);
        editor.putInt("p_age", p_age);

        editor.commit();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
