package mnefzger.de.sensorplatform.Processors;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.Core.IEventCallback;
import mnefzger.de.sensorplatform.Core.ImageModule;
import mnefzger.de.sensorplatform.Core.Preferences;
import mnefzger.de.sensorplatform.R;

/**
 * This class includes all image analysis functions for
 * a. face detection
 * b. car detection and following distance calculation
 */

public class ImageProcessor{
    private IEventCallback callback;

    private Double currentSpeed;

    private double TTC_NORMAL;
    private double TTC_RISKY;
    private double TTC_DANGEROUS;

    private boolean flip_back = false;
    private boolean flip_front = false;

    static{
        System.loadLibrary("opencv_java3");
        System.loadLibrary("imgProc");
    }

    private native int nInitCascades();

    private native int[] nAsmFindFace(long adress1, long adress2, boolean flipped);

    private native int[] nAsmFindCars(long adress1, long adress2, boolean flipped);

    public ImageProcessor(ImageModule im, Context c) {
        callback = im;
        writeCascadeToFileSystem(c, "lbpcascade_frontalface.xml");
        writeCascadeToFileSystem(c, "haarcascade_vehicles.xml");
        //writeCascadeToFileSystem(c, "haarcascade_vehicles_alt.xml");
        nInitCascades();

        SharedPreferences setting_prefs = c.getSharedPreferences(c.getString(R.string.settings_preferences_key), Context.MODE_PRIVATE);
        TTC_NORMAL = Preferences.getNormalTTC(setting_prefs);
        TTC_RISKY = Preferences.getRiskyTTC(setting_prefs);
        TTC_DANGEROUS = Preferences.getDangerousTTC(setting_prefs);

        boolean reversed = Preferences.isReverseOrientation(setting_prefs);
        if(reversed) flip_back = true;
        else flip_front = true;
    }

    public void setCurrentSpeed(Double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public byte[] processImageFront(byte[] image, int width, int height) {
        Mat imgMat = new Mat(height + height/2, width, CvType.CV_8UC1);
        imgMat.put(0,0,image);

        Mat output = new Mat();

        detectFace(imgMat, output);

        if(!output.empty()) {
            byte[] return_buff = new byte[(int) (output.total() * output.elemSize())];
            output.get(0, 0, return_buff);
            return return_buff;
        } else {
            return image;
        }
    }

    public byte[] processImageBack(byte[] image, int width, int height) {
        Mat imgMat = new Mat(height + height/2, width, CvType.CV_8UC1);
        imgMat.put(0,0,image);

        Mat output = new Mat();

        detectCars(imgMat, output);

        if(!output.empty()) {
            byte[] return_buff = new byte[(int) (output.total() * output.elemSize())];
            output.get(0, 0, return_buff);
            return return_buff;
        } else {
            return image;
        }
    }

    private void detectFace(Mat imgMat, Mat output) {
        // the native code writes the processing results to the output adress
        int[] faces = findFaceInImage(imgMat.getNativeObjAddr(), output.getNativeObjAddr());
    }

    private void detectCars(Mat imgMat, Mat output) {
        // the native code writes the processing results to the output adress
        int[] cars = findCarsInImage(imgMat.getNativeObjAddr(), output.getNativeObjAddr());
    }

    long lastFaceDetect = System.currentTimeMillis();
    private int[] findFaceInImage(long adress1, long adress2) {
        double time = System.currentTimeMillis();
        int[] faces = nAsmFindFace(adress1, adress2, flip_front);
        //Log.d("FACE_DETECTION_FRAME", System.currentTimeMillis()-time + "");

        if(faces.length == 4) {
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "Face detected", 0));
            lastFaceDetect = System.currentTimeMillis();
        } else {
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "No Face detected", 0));

            long distractionTime = System.currentTimeMillis()-lastFaceDetect;
            if(distractionTime > 3000)
                callback.onEventDetected(new EventVector(EventVector.LEVEL.DANGEROUS, System.currentTimeMillis(), "Driver is distracted", distractionTime));
        }

        return faces;
    }

    private int[] findCarsInImage(long adress1, long adress2) {
        double time = System.currentTimeMillis();
        int[] cars = nAsmFindCars(adress1, adress2, flip_back);
        //Log.d("CAR_DETECTION_FRAME", System.currentTimeMillis()-time + "");

        if(cars != null && cars.length > 0) {
            //Log.d("CAR_DETECTION", "Detected " +  cars.length/4 + " cars");
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "Cars detected", cars.length/4));

            int max = 0;
            for(int c=0; c<cars.length; c+=4) {
                int pixel_width = cars[c + 2];
                if(pixel_width > max)
                    max = pixel_width;
            }
            detectTailgating( calculateDist(max) );


        } else {
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "No Cars detected", 0));
        }

        return cars;
    }

    /**
     * Calculates the following distance based on the car's pixel width
     * The hardcoded values are for a Google Nexus 6P, replace if another phone is used
     */
    private double calculateDist(int pixel_width) {
        double sensor_width = 6.17; //mm
        double f = 4.67; //mm
        int img_width = 320; //pixel
        int real_width = 1847; //mm

        double distance_to_car = (f * real_width * img_width) / (pixel_width * sensor_width); //mm
        distance_to_car /= 1000; //m

        return distance_to_car;
    }

    private void detectTailgating(double distance) {
        if(currentSpeed != null && currentSpeed > 0) {
            // Time to collision
            double TTC = distance / (currentSpeed / 3.6);

            // tailgating detection starts at >25km/h
            if(currentSpeed >= 25 && currentSpeed <= 60) { // urban
                EventVector.LEVEL l = null;
                if (TTC < TTC_DANGEROUS/2)
                    l = EventVector.LEVEL.DANGEROUS;
                else if(TTC < TTC_RISKY/2)
                    l = EventVector.LEVEL.RISKY;
                else if(TTC < TTC_NORMAL/2)
                    l = EventVector.LEVEL.NORMAL;

                if(l != null)
                    callback.onEventDetected(new EventVector(l, System.currentTimeMillis(), "Tailgating below 60km/h, TTC", TTC));
                return;

            } else if(currentSpeed > 60) { // highway
                EventVector.LEVEL l = null;
                if (TTC < TTC_DANGEROUS)
                    l = EventVector.LEVEL.DANGEROUS;
                else if(TTC < TTC_RISKY)
                    l = EventVector.LEVEL.RISKY;
                else if(TTC < TTC_NORMAL)
                    l = EventVector.LEVEL.NORMAL;

                if(l != null)
                    callback.onEventDetected(new EventVector(l, System.currentTimeMillis(), "Tailgating above 60km/h, TTC", TTC));
                return;
            }
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "TTC", TTC));

        } else {
            callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "Distance to front car", distance));

            if(distance < 6)
                callback.onEventDetected(new EventVector(EventVector.LEVEL.DEBUG, System.currentTimeMillis(), "Tailgating, Distance", distance));
        }
    }


    private void writeCascadeToFileSystem(Context c, String filename) {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = baseDir + "/SensorPlatform/";
        Log.d("CASCADES", "writing to " + filePath);
        File toPath = new File(filePath);

        if (!toPath.exists()) {
            toPath.mkdir();
        }

        try {
            InputStream inStream = c.getAssets().open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
            File toFile = new File(toPath, filename);
            copyAssetFile(br, toFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyAssetFile(BufferedReader br, File toFile) throws IOException {
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
}
