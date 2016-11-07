package au.carrsq.sensorplatform.Utilities;

import android.app.Activity;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class IO {
    final static String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/SensorPlatform/";

    public static void writeFile(File file, String content) {
        try {
            if(!isExternalStorageWritable())  {
                throw new IOException("External storage not writable!");
            }
            OutputStream stream = new FileOutputStream(file, false);
            stream.write(content.getBytes());
            stream.close();
            Log.d("IO", "Wrote to file: " + file.getPath());
        } catch (IOException e) {
            System.err.println("Caught IOException: " +  e.getMessage());
        }
    }

    public static void writeCSV(File file, String[] line) {
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

    public static void copyAssetFile(BufferedReader br, File toFile) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(toFile));

            int in;
            while ((in = br.read()) != -1) {
                bw.write(in);
            }
        } finally {
            Log.d("CASCADES", "Writing finished");
            try{
                if (bw != null) {
                    bw.close();
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static long getAvailableInternalMemorySize() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / 1048576;
        System.out.println("Megs :"+megAvailable);

        return megAvailable;
    }

    public static String loadJSONFromAsset(Activity app, String filename) {
        String json = null;
        try {
            InputStream is = app.getAssets().open(filename);
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

    public static String loadJSONFromFile(String filename) {
        String json = "";

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = baseDir + "/SensorPlatform/";

        BufferedReader buffered_reader=null;
        try
        {
            buffered_reader = new BufferedReader(new FileReader(filePath+filename));
            String line;

            while ((line = buffered_reader.readLine()) != null)
            {
                json += line;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (buffered_reader != null)
                    buffered_reader.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }

        return json;
    }
}
