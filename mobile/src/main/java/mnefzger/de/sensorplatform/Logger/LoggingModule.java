package mnefzger.de.sensorplatform.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import mnefzger.de.sensorplatform.Core.DataVector;
import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.R;

/**
 * This class writes Raw Data and Event Data to csv files on the phone
 */

public class LoggingModule {

    private String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    private String filePath = baseDir + "/SensorPlatform/logs";

    private String fileNameRaw = "RawData_";
    private String fileNameEvent = "EventData_";
    private String fileNameSurvey = "SurveyAnswers";

    private File rawFile;
    private File eventFile;
    private File surveyFile;

    private String tripID;
    private int counter;

    private static LoggingModule instance;

    public static LoggingModule getInstance() {
        if(instance == null) {
            instance = new LoggingModule();
        }

        return instance;
    }

    private LoggingModule() {
        File folder = new File(filePath);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public void generateNewLoggingID(String participantID) {
        tripID = System.currentTimeMillis() +"_"+ participantID;
        Log.d("LOGGING", "New Trip ID " +tripID);
    }

    public void createNewTripFileSet() {
        createNewRawFile(fileNameRaw + tripID + ".csv");
        createNewEventFile(fileNameEvent + tripID + ".csv");

        long space = getAvailableInternalMemorySize();
        if(space < 1000) {
            // TODO, less than 1 gb of free space, create warning
        }
    }

    private void createNewRawFile(String name) {
        rawFile = new File(filePath + File.separator + name);

        if(!rawFile.exists()) {
            try {
                rawFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            createHeadersRaw();
        }
    }

    private void createNewEventFile(String name) {
        eventFile = new File(filePath + File.separator + name);

        if(!eventFile.exists()) {
            try {
                eventFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            createHeadersEvent();
        }
    }

    private void createNewSurveyFile(String name) {
        surveyFile = new File(filePath + File.separator + name);

        if(!surveyFile.exists()) {
            try {
                surveyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeRawToCSV(DataVector v) {
        String[] line = { tripID + ";" + v.toCSVString() };

        write(rawFile, line);
    }

    public void writeEventToCSV(EventVector v) {
        String[] line = { tripID + ";" + v.toCSVString() };

        write(eventFile, line);
    }

    public void writeSurveyToFile(String answers, int items) {
        //createHeadersSurvey(items);
        createNewSurveyFile(fileNameSurvey + ".csv");

        String[] line = { tripID + ";" + answers };

        write(surveyFile, line);
    }

    private void createHeadersRaw() {
        String[] line = { "tripID;s_id;s_name;p_id;p_age;p_gender;timestamp;dateTime;accelerationX;accelerationY;accelerationZ;rotationX;rotationY;rotationZ;light;latitude;longitude;gps_speed;obd_speed;obd_rpm;obd_fuel;heart_rate;weather" };

        write(rawFile, line);
    }

    private void createHeadersEvent() {
        String[] line = { "tripID;timestamp;level;description;value;extra;videoFront;videoBack" };

        write(eventFile, line);
    }

    public void createHeadersSurvey(int noOfQuestions) {
        String[] line = new String[noOfQuestions+1];
        line[0] = "tripID;";

        for(int i=0; i<noOfQuestions; i++) {
            line[i+1] = "q"+(i+1)+";";
        }

        write(eventFile, line);
    }

    private void write(File file, String[] line) {
        try {
            if(!isExternalStorageWritable())  {
                throw new IOException("External storage not writable!");
            }
            CSVWriter writer = new CSVWriter(new FileWriter(file, true), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,CSVWriter.DEFAULT_LINE_END);
            writer.writeNext(line);
            writer.close();
        } catch (IOException e) {
            System.err.println("Caught IOException: " +  e.getMessage());
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private static long getAvailableInternalMemorySize() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / 1048576;
        System.out.println("Megs :"+megAvailable);

        return megAvailable;
    }


}
