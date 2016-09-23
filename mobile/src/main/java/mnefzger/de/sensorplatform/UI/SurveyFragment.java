package mnefzger.de.sensorplatform.UI;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.Core.SurveyModel;
import mnefzger.de.sensorplatform.Logger.LoggingModule;
import mnefzger.de.sensorplatform.R;

public class SurveyFragment extends Fragment {

    SurveyModel survey;

    private FrameLayout next;
    private TextView question;
    private int currentQuestion = 0;

    private RadioButton stronglyDisagree, disagree, undecided, agree, stronglyAgree;
    private RadioGroup group;

    private String answers = "";

    public SurveyFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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

        loadSurvey();
        showQuestion(currentQuestion);
        //getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return v;
    }

    View.OnClickListener next_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            answers += getCheckedLickertButton() + ";";


            if(++currentQuestion < survey.questions.size()) {
                Log.d("NEXT QUESTION", getCheckedLickertButton());
                Log.d("ANSWERS", "--> " + answers);
                showQuestion(currentQuestion);

            } else {
                // write to file
                LoggingModule lm = LoggingModule.getInstance();
                lm.writeSurveyToFile(answers, survey.questions.size());

                // reset answer string
                answers = "";

                // go back to main data collection screen
                MainActivity app = (MainActivity) getActivity();
                app.goToAppFragment();
                app.setIntent(new Intent("mnefzger.de.sensorplatform"));
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    };

    private String getCheckedLickertButton() {
        if(stronglyAgree.isChecked())
            return "Strongly Agree";
        if(agree.isChecked())
            return "Agree";
        if(undecided.isChecked())
            return "Undecided";
        if(disagree.isChecked())
            return "Disagree";
        if(stronglyDisagree.isChecked())
            return "Strongly Disagree";

        return "invalid";
    }

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
