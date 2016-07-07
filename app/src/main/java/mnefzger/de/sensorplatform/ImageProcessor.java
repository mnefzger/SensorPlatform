package mnefzger.de.sensorplatform;


import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class ImageProcessor{
    private IEventCallback callback;

    static{
        System.loadLibrary("opencv_java3");
    }

    public ImageProcessor(ImageModule im) {
        callback = im;
    }

    public byte[] processImage(byte[] image, int width, int height) {
        Mat imgMat = new Mat(height + height/2, width, CvType.CV_8UC1);
        imgMat.put(0,0,image);

        Mat mBgr = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor( imgMat, mBgr, Imgproc.COLOR_YUV2BGR_I420);

        Mat gray = new Mat();
        Imgproc.cvtColor(mBgr, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(gray,gray, 150, 280);


        Mat yuvout = new Mat();
        Imgproc.cvtColor(gray, yuvout, Imgproc.COLOR_GRAY2BGR );

        Imgproc.cvtColor( yuvout, yuvout, Imgproc.COLOR_BGR2YUV_I420);


        byte[] return_buff = new byte[(int) (yuvout.total() * yuvout.elemSize())];
        yuvout.get(0, 0, return_buff);

        return return_buff;
    }


}
