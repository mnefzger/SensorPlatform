package mnefzger.de.sensorplatform.Processors;

import android.content.Context;
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

import mnefzger.de.sensorplatform.Core.EventVector;
import mnefzger.de.sensorplatform.Core.IEventCallback;
import mnefzger.de.sensorplatform.Core.ImageModule;

/**
 * This class includes all image analysis functions for
 * a. face detection
 * b. car detection and following distance calculation (TODO)
 */

public class ImageProcessor{
    private IEventCallback callback;

    private boolean faceProcRunning = false;

    static{
        System.loadLibrary("opencv_java3");
        System.loadLibrary("imgProc");
    }

    private native int nInitCascades();

    private native int[] nAsmFindFace(long adress1, long adress2);

    private native int[] nAsmFindCars(long adress1, long adress2);

    public ImageProcessor(ImageModule im, Context c) {
        callback = im;
        writeCascadeToFileSystem(c, "lbpcascade_frontalface.xml");
        writeCascadeToFileSystem(c, "haarcascade_vehicles.xml");
        nInitCascades();
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
        if(!faceProcRunning) {
            // the native code writes the processing results to the output adress
            int[] faces = findFaceInImage(imgMat.getNativeObjAddr(), output.getNativeObjAddr());
        }
    }

    private void detectCars(Mat imgMat, Mat output) {
        // the native code writes the processing results to the output adress
        int[] cars = findCarsInImage(imgMat.getNativeObjAddr(), output.getNativeObjAddr());
    }


    private int[] findFaceInImage(long adress1, long adress2) {
        faceProcRunning = true;
        double time = System.currentTimeMillis();
        int[] faces = nAsmFindFace(adress1, adress2);
        Log.d("FACE_DETECTION_FRAME", System.currentTimeMillis()-time + "");

        if(faces.length == 4) {
            Log.d("FACE_DETECTION", "Detected at (" + faces[0] + "," + faces[1]+")");
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "Face detected", 0));
        } else {
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "No Face detected", 0));
        }
        faceProcRunning = false;

        return faces;
    }

    private int[] findCarsInImage(long adress1, long adress2) {
        double time = System.currentTimeMillis();
        int[] cars = nAsmFindCars(adress1, adress2);
        Log.d("CAR_DETECTION_FRAME", System.currentTimeMillis()-time + "");

        if(cars.length > 0) {
            Log.d("CAR_DETECTION", "Detected " +  cars.length/4 + " cars");
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "Cars detected", cars.length/4));

            int max = 0;
            for(int c=0; c<cars.length; c+=4) {
                int pixel_width = cars[c + 2];
                if(pixel_width > max)
                    max = pixel_width;
            }
            calculateDist(max);


        } else {
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "No Car detected", 0));
        }

        return cars;
    }

    private void calculateDist(int pixel_width) {
        double sensor_width = 6.17; //mm
        double f = 4.67; //mm
        int img_width = 320; //pixel
        int real_width = 1847; //mm

        double distance_to_car = (f * real_width * img_width) / (pixel_width * sensor_width); //mm
        distance_to_car /= 1000; //m

       // Log.d("CAR_DETECTION_DISTANCE", distance_to_car + "m");
        callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "Distance to front car", distance_to_car));

        if(distance_to_car < 8) callback.onEventDetected(new EventVector(false, System.currentTimeMillis(), "Tailgating", distance_to_car));
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
            if (bw != null) {
                bw.close();
            }
            br.close();
        }
    }

}
