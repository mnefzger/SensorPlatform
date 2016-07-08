package mnefzger.de.sensorplatform;


import android.os.SystemClock;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;

import mnefzger.de.sensorplatform.Utilities.MathFunctions;


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

    private native int[] nAsmFindFace(long adress);


    public byte[] processImage(byte[] image, int width, int height) {
        Mat imgMat = new Mat(height + height/2, width, CvType.CV_8UC1);
        imgMat.put(0,0,image);

        Mat gray = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(imgMat, gray, Imgproc.COLOR_YUV2GRAY_I420);

        if(!faceProcRunning) {
            int[] faces = findFaceInImage(gray);
            if(faces.length == 4) {
                Imgproc.circle(gray, new Point(faces[1]*2 + faces[2] + faces[3], faces[0]*2 + faces[2] + faces[3]), 100, new Scalar(255,0,0), 5);
                //Imgproc.rectangle(dst, new Point(faces[0]-faces[2]/2,faces[1]-faces[3]/2), new Point(faces[0]+faces[2]/2,faces[1]+faces[3]/2), new Scalar(0,255,0), 5);
            } else {
                Imgproc.rectangle(gray, new Point(0,0), new Point(80,80), new Scalar(255,0,0), 5);
            }
        } else {
                Imgproc.circle(gray, new Point(0,0), 25, new Scalar(255,0,0), 5);
        }

        Point center = new Point(gray.width()/2, gray.height()/2);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, -90, 1);

        Mat dst = gray.clone();
        Imgproc.warpAffine(gray, dst, rotationMatrix, gray.size());

        Mat rgb = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor( dst, rgb, Imgproc.COLOR_GRAY2RGB);

        Mat yuvout = new Mat(height + height/2, width, CvType.CV_8UC1);
        Imgproc.cvtColor( rgb, yuvout, Imgproc.COLOR_RGB2YUV_I420);

        byte[] return_buff = new byte[(int) (yuvout.total() * yuvout.elemSize())];
        yuvout.get(0, 0, return_buff);

        return return_buff;
    }


    private int[] findFaceInImage(Mat image) {
        faceProcRunning = true;
        double time = System.currentTimeMillis();
        int[] faces = nAsmFindFace(image.getNativeObjAddr());

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
