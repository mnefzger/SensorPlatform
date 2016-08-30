package mnefzger.de.sensorplatform.Processors;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import mnefzger.de.sensorplatform.EventVector;
import mnefzger.de.sensorplatform.IEventCallback;
import mnefzger.de.sensorplatform.ImageModule;
import mnefzger.de.sensorplatform.R;

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
        writeCascadeToFileSystem(c);
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

        if(cars.length == 4) {
            Log.d("CAR_DETECTION", "Detected at (" + cars[0] + "," + cars[1]+")");
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "Car detected", 0));
        } else {
            callback.onEventDetected(new EventVector(true, System.currentTimeMillis(), "No Car detected", 0));
        }

        return cars;
    }


    private void writeCascadeToFileSystem(Context c) {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = baseDir + "/SensorPlatform/";
        Log.d("CASCADES", "writing to " + filePath);
        File toPath = new File(filePath);

        if (!toPath.exists()) {
            toPath.mkdir();
        }

        try {
            InputStream inStream = c.getAssets().open("lbpcascade_frontalface.xml");
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
            File toFile = new File(toPath, "lbpcascade_frontalface.xml");
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
