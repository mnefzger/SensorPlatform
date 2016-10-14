package mnefzger.de.sensorplatform.UI;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import mnefzger.de.sensorplatform.Core.MainActivity;
import mnefzger.de.sensorplatform.Core.Preferences;
import mnefzger.de.sensorplatform.R;


public class SettingsFragment extends PreferenceFragment
        implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sensor_prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(getActivity().getString(R.string.settings_preferences_key));
        addPreferencesFromResource(R.xml.settings_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout fl = (FrameLayout) v.findViewById(R.id.start_button);
        fl.setOnClickListener(this);

        SharedPreferences setting_prefs = getActivity().getSharedPreferences(getActivity().getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        setting_prefs.registerOnSharedPreferenceChangeListener(this);

        sensor_prefs = getActivity().getSharedPreferences(getActivity().getString(R.string.sensor_preferences_key), Context.MODE_PRIVATE);

        if(!Preferences.frontCameraActivated(sensor_prefs) && !Preferences.backCameraActivated(sensor_prefs) ) {
            CheckBoxPreference saving = (CheckBoxPreference) findPreference("image_saving");
            saving.setChecked(false);
            saving.setEnabled(false);
            saving.setSelectable(false);

            ListPreference res = (ListPreference) findPreference("video_resolution");
            res.setEnabled(false);

        }

        if(!Preferences.frontCameraActivated(sensor_prefs)) {
            CheckBoxPreference front_proc = (CheckBoxPreference) findPreference("image_front_processing");
            front_proc.setChecked(false);
            front_proc.setEnabled(false);
            front_proc.setSelectable(false);

        }

        if(!Preferences.backCameraActivated(sensor_prefs) ) {
            CheckBoxPreference back_proc = (CheckBoxPreference) findPreference("image_back_processing");
            back_proc.setChecked(false);
            back_proc.setEnabled(false);
            back_proc.setSelectable(false);

        }

        /**
         * Not fully integrated yet.
         */
        /*Preference filepath = (Preference) findPreference("log_file_path");
        filepath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                loadFileList();
                onCreateDialog();
                return false;
            }
        });
        */

        return v;
    }



    public void startApplication() {
        ((MainActivity) getActivity()).startMeasuring();
    }


    @Override
    public void onClick(View v) {
        if(Preferences.frontCameraActivated(sensor_prefs) || Preferences.backCameraActivated(sensor_prefs))
            ((MainActivity)getActivity()).goToCameraPreviewFragment(true);
        else
            startApplication();
    }

    private String[] mFileList;
    private File mPath = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
    private String mChosenDir;
    private void loadFileList() {
        try {
            mPath.mkdirs();
        }
        catch(SecurityException e) {
            Log.e("FILEPATH", "unable to write on the sd card " + e.toString());
        }
        if(mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory();
                }

            };
            mFileList = mPath.list(filter);
        }
        else {
            mFileList= new String[0];
        }
    }

    private Dialog onCreateDialog() {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Choose your filepath");
        if(mFileList == null) {
            Log.e("FILEPATH", "Showing file picker before loading the file list");
            dialog = builder.create();
            return dialog;
        }
        builder.setItems(mFileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mChosenDir = mFileList[which];
                Log.d("FilePATH", mPath + "/" + mChosenDir);
            }
        });
        dialog = builder.show();
        return dialog;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("front_active") || key.equals("back_active")) {
            if(!sharedPreferences.getBoolean("front_active", true) &&
                    !sharedPreferences.getBoolean("back_active", true)) {
                findPreference("image_saving").setEnabled(false);
                CheckBoxPreference box = (CheckBoxPreference) findPreference("image_saving");
                box.setChecked(false);
            } else {
                findPreference("image_saving").setEnabled(true);
                findPreference("image_saving").setSelectable(true);
            }
        } else if(key.equals("obd_raw")) {
            boolean obd = sharedPreferences.getBoolean("obd_raw", false);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("obd_raw", obd);
            editor.apply();
        }
    }
}
