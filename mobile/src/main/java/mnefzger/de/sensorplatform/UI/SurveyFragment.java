package mnefzger.de.sensorplatform.UI;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.Core.SurveyModel;
import mnefzger.de.sensorplatform.R;

public class SurveyFragment extends Fragment {

    SurveyModel survey;

    private FrameLayout next;
    private TextView question;
    private int currentQuestion = 0;

    private RadioButton stronglyDisagree, disagree, undecided, agree, stronglyAgree;
    private RadioGroup group;

    public SurveyFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_survey, container, false);
        question = (TextView) v.findViewById(R.id.question);
        next = (FrameLayout) v.findViewById(R.id.next_question_button);
        next.setOnClickListener(next_listener);

        stronglyDisagree = (RadioButton) v.findViewById(R.id.stronglyDisagree);
        disagree = (RadioButton) v.findViewById(R.id.disagree);
        undecided = (RadioButton) v.findViewById(R.id.undecided);
        agree = (RadioButton) v.findViewById(R.id.agree);
        stronglyAgree = (RadioButton) v.findViewById(R.id.stronglyAgree);

        group = (RadioGroup) v.findViewById(R.id.lickertGroup);

      /*  group.addView(stronglyDisagree);
        group.addView(disagree);
        group.addView(undecided);
        group.addView(agree);
        group.addView(stronglyAgree);*/

        /*stronglyDisagree.setOnClickListener(button_listener);
        disagree.setOnClickListener(button_listener);
        undecided.setOnClickListener(button_listener);
        agree.setOnClickListener(button_listener);
        stronglyAgree.setOnClickListener(button_listener);*/

        loadSurvey();
        showQuestion(currentQuestion);

        return v;
    }

    View.OnClickListener button_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioButton btn = (RadioButton) view;
            boolean current = btn.isChecked();
            resetButtons();
            btn.setChecked(current);
        }
    };

    View.OnClickListener next_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(++currentQuestion < survey.questions.size())
                showQuestion(currentQuestion);
            else {
                MainActivity app = (MainActivity) getActivity();
                app.goToAppFragment();
            }
        }
    };

    private void loadSurvey() {
        String surveyJson = loadJSONFromAsset("survey.json");
        this.survey = new Gson().fromJson(surveyJson, SurveyModel.class);
    }

    private void showQuestion(int index) {
        resetButtons();
        question.setText("\"" + survey.questions.get(index).q + "\"");
    }

    private void resetButtons() {
        stronglyDisagree.setChecked(false);
        disagree.setChecked(false);
        undecided.setChecked(false);
        agree.setChecked(false);
        stronglyAgree.setChecked(false);
    }

    private String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = getActivity().getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
