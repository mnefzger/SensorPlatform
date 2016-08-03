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

    String fileNameRaw = "RawData.csv";
    String fileNameEvent = "EventData.csv";

    File rawFile;
    File eventFile;

    public LoggingModule(Context app) {
        verifyStoragePermissions(app);
        try {
            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            rawFile = new File(filePath + File.separator + fileNameRaw);
            if(!rawFile.exists()) {
                rawFile.createNewFile();
                createHeadersRaw();
            }
            eventFile = new File(filePath + File.separator + fileNameEvent);
            if(!rawFile.exists()) {
                eventFile.createNewFile();
                createHeadersEvent();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        String[] line = { "timestamp;dateTime;accelerationX;accelerationY;accelerationZ;rotationX;rotationY;rotationZ;latitude;longitude;GPS speed;OBD speed;OBD RPM" };

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

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param context
     */
    private static void verifyStoragePermissions(Context context) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    (Activity)context,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}
