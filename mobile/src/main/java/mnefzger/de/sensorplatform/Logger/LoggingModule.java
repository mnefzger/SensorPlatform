package mnefzger.de.sensorplatform.Logger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import mnefzger.de.sensorplatform.DataVector;
import mnefzger.de.sensorplatform.EventVector;


public class LoggingModule {

    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String filePath = baseDir + "/SensorPlatform/logs";

    String fileNameRaw = "RawData_";
    String fileNameEvent = "EventData_";

    File rawFile;
    File eventFile;

    public LoggingModule() {
        File folder = new File(filePath);
        if (!folder.exists()) {
            folder.mkdir();
        }
        // TODO only create if logging is selected
        createNewRawFile(fileNameRaw + System.currentTimeMillis() + ".csv");
        createNewEventFile(fileNameEvent + System.currentTimeMillis() + ".csv");
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

    public void writeRawToCSV(DataVector v) {
        String[] line = { v.toCSVString() };

        write(rawFile, line);
    }

    public void writeEventToCSV(EventVector v) {
        String[] line = { v.toCSVString() };

        write(eventFile, line);
    }

    private void createHeadersRaw() {
        String[] line = { "timestamp;dateTime;accelerationX;accelerationY;accelerationZ;rotationX;rotationY;rotationZ;light;latitude;longitude;GPS speed;OBD speed;OBD RPM;OBD fuel;Heart Rate" };

        write(rawFile, line);
    }

    private void createHeadersEvent() {
        String[] line = { "timestamp;description;value" };

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



}
