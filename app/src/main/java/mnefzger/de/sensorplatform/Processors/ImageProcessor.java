package mnefzger.de.sensorplatform.Processors;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import mnefzger.de.sensorplatform.EventVector;
import mnefzger.de.sensorplatform.IEventCallback;
import mnefzger.de.sensorplatform.ImageModule;


public class ImageProcessor{
    private IEventCallback callback;

    private boolean faceProcRunning = false;

    static{
        System.loadLibrary("opencv_java3");
        System.loadLibrary("imgProc");
    }

    public ImageProcessor(ImageModule im) {
        callback = im;
    }

    private native int[] nAsmFindFace(long adress1, long adress2);


    public byte[] processImage(byte[] image, int width, int height) {
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

    private void detectFace(Mat imgMat, Mat output) {
        if(!faceProcRunning) {
            // the native code writes the processing results to the output adress
            int[] faces = findFaceInImage(imgMat.getNativeObjAddr(), output.getNativeObjAddr());
        }
    }


    private int[] findFaceInImage(long adress1, long adress2) {
        faceProcRunning = true;
        double time = System.currentTimeMillis();
        int[] faces = nAsmFindFace(adress1, adress2);

        if(faces.length == 4) {
            Log.d("FACE_DETECTION", "Detected at (" + faces[0] + "," + faces[1]+")");
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "Face detected", 0));
        } else {
            Log.d("FACE_DETECTION", "Nothing detected");
            callback.onEventDetected(new EventVector(System.currentTimeMillis(), "No Face detected", 0));
        }
        faceProcRunning = false;
        Log.d("FACE_DETECTION_FRAME", System.currentTimeMillis()-time + "");
        return faces;
    }




}
