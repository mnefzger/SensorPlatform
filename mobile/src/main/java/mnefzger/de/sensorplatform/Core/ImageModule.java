package mnefzger.de.sensorplatform.Core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import mnefzger.de.sensorplatform.Processors.ImageProcessor;
import mnefzger.de.sensorplatform.Utilities.SequenceEncoderWrapper;

/**
 * This class provides the connection to front and back camera.
 * It also includes functions to save in the background:
 * a. single images
 * b. a number of single images as video
 */

public class ImageModule implements IEventCallback{
    private Context context;
    protected ImageProcessor imgProc;
    private IDataCallback callback;
    private SharedPreferences prefs;

    private CameraManager cameraManager;
    private CameraDevice camera_front;
    private CameraDevice camera_back;
    private CameraCaptureSession session_front;
    private CameraCaptureSession session_back;
    private ImageReader imageReader_front;
    private ImageReader imageReader_back;

    private SparseArray<YuvImage> backImages = new SparseArray<>();
    private SparseArray<YuvImage> frontImages = new SparseArray<>();
    private SparseArray<byte[]> frontImagesCV = new SparseArray<>();
    private SparseArray<byte[]> backImagesCV = new SparseArray<>();

    private int FRONT_MAX_FPS;
    private double FRONT_AVG_FPS;
    private int FRONT_PROCESSING_FPS;

    private int BACK_MAX_FPS;
    private double BACK_AVG_FPS;
    private int BACK_PROCESSING_FPS;

    private int RES_W;
    private int RES_H;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private final String TAG = "CAMERA_SENSORPLATFORM";

    static boolean backSaving = false;
    static boolean frontSaving = false;


    public ImageModule(IDataCallback caller, Context app) {
        context = app;
        callback = caller;
        cameraManager = (CameraManager) app.getSystemService(Activity.CAMERA_SERVICE);
        imgProc = new ImageProcessor(this, app);

        prefs = PreferenceManager.getDefaultSharedPreferences(app);
        setPrefs();
    }

    private void setPrefs() {
        FRONT_MAX_FPS = Preferences.getFrontFPS(prefs);
        FRONT_PROCESSING_FPS = Preferences.getFrontProcessingFPS(prefs);
        FRONT_AVG_FPS = FRONT_MAX_FPS;

        BACK_MAX_FPS = Preferences.getBackFPS(prefs);
        BACK_PROCESSING_FPS = Preferences.getBackProcessingFPS(prefs);
        BACK_AVG_FPS = BACK_MAX_FPS;
    }

    public void startCapture() {
        startBackgroundThread();
        Log.d("CAMERA", ""+Preferences.backCameraActivated(prefs) );
        if( Preferences.backCameraActivated(prefs) ) open("0");
        if( Preferences.frontCameraActivated(prefs) ) open("1");
    }

    public void stopCapture() {
        if(mBackgroundThread != null)
            stopBackgroundThread();
        if(camera_front != null)
            camera_front.close();
        if(camera_back != null)
            camera_back.close();
    }

    private void open(String id) {
        RES_W = Preferences.getVideoResolution(prefs);
        if(RES_W  == 1280) RES_H = 720;
        else if(RES_W == 640) RES_H = 480;
        else if(RES_W == 320) RES_H = 240;

        Log.d("RESOLUTION", RES_W + "x" + RES_H);
        try {
            int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            if(permission == PackageManager.PERMISSION_GRANTED) {
                if(id == "0") {
                    imageReader_back = ImageReader.newInstance(RES_H, RES_W, ImageFormat.YUV_420_888, 15);
                    imageReader_back.setOnImageAvailableListener(onBackImageAvailableListener, mBackgroundHandler);
                    cameraManager.openCamera(id, backCameraStateCallback, null );
                }
                if(id == "1") {
                    imageReader_front = ImageReader.newInstance(RES_H, RES_W, ImageFormat.YUV_420_888, 15);
                    imageReader_front.setOnImageAvailableListener(onFrontImageAvailableListener, mBackgroundHandler);
                    cameraManager.openCamera(id, frontCameraStateCallback, null );
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback backCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, camera.getId().toString());
            ImageModule.this.camera_back = camera;
            try {
                if(camera.getId() == "0") {
                    ImageModule.this.camera_back.createCaptureSession(Arrays.asList(imageReader_back.getSurface()), backSessionStateCallback, null);
                }
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            stopCapture();
        }

        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    private CameraDevice.StateCallback frontCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, camera.getId().toString());
            ImageModule.this.camera_front = camera;
            try {
                if(camera.getId() == "1") {
                    ImageModule.this.camera_front.createCaptureSession(Arrays.asList(imageReader_front.getSurface()), frontSessionStateCallback, null);
                }
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            stopCapture();
        }

        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    private CameraCaptureSession.StateCallback frontSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            ImageModule.this.session_front = session;
            try {
                session_front.setRepeatingRequest(createCaptureRequest(camera_front, imageReader_front), null, null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    };

    private CameraCaptureSession.StateCallback backSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            ImageModule.this.session_back= session;
            try {
                session_back.setRepeatingRequest(createCaptureRequest(camera_back, imageReader_back), null, null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    };

    private ImageReader.OnImageAvailableListener onFrontImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader){
            handleImageFront(reader.acquireNextImage());
        }
    };

    private ImageReader.OnImageAvailableListener onBackImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader){
            handleImageBack(reader.acquireNextImage());
        }
    };

    private CaptureRequest createCaptureRequest(CameraDevice camera, ImageReader reader) {
        try {
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(reader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public void onEventDetected(EventVector v) {
        callback.onEventData(v);
    }

    public void saveVideoAfterEvent(EventVector ev) {
        final EventVector v = ev;
        if(!frontSaving && !backSaving) {
            mBackgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(Preferences.frontCameraActivated(prefs)) {
                        new VideoSaver2(frontImagesCV, (int)FRONT_AVG_FPS, RES_W, RES_H, "front", v.getTimestamp());
                    }
                    if(Preferences.backCameraActivated(prefs)) {
                        //new VideoSaver(backImages, (int)BACK_AVG_FPS, 640, 480, "back", v.getTimestamp());
                        new VideoSaver2(backImagesCV, (int)BACK_AVG_FPS, RES_W, RES_H, "back", v.getTimestamp());
                    }
                }
            }, 4000);
        }
    }

    private double lastFront = System.currentTimeMillis();
    private double lastFrontProc = System.currentTimeMillis();
    private double lastFrontSaved = System.currentTimeMillis();
    private int frontIt = 0;
    private void handleImageFront(Image i) {
        if(i != null) {
            final byte[] bytes = getBytes(i);
            final int w = i.getWidth();
            final int h = i.getHeight();
            i.close();
            YuvImage yuvimage;

            double now = System.currentTimeMillis();

            /**
             * Decide if frame is to be processed or not
             */
            if(Preferences.frontImagesProcessingActivated(prefs) && now - lastFrontProc >= (1000 / FRONT_PROCESSING_FPS) ) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        imgProc.processImageFront(bytes.clone(), w, h);
                    }
                }).start();
                //byte[] processedImg = imgProc.processImage(bytes, img.getWidth(), img.getHeight());
                //yuvimage = new YuvImage(processedImg, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
                lastFrontProc = now;
            }

            /**
             * Store the received image (either processed or raw) and write it to file
             */
            if(now - lastFront >= (1000/(1+FRONT_MAX_FPS)) ) {
                double latestFPS = 1000 / (now - lastFront);
                FRONT_AVG_FPS = 0.995*FRONT_AVG_FPS + 0.005*latestFPS;
                //Log.d("FPS front", FRONT_AVG_FPS+", " +latestFPS);

                frontImagesCV.append(frontIt, bytes);
                frontIt++;
                lastFront = now;

                //mBackgroundHandler.post( new ImageSaver(yuvimage, "front") );
            }

            /**
             * Only store the last ten seconds in the image buffer
             */
            if(frontImagesCV.size() > (10*FRONT_MAX_FPS) ) {
                int key = frontImagesCV.keyAt(0);
                frontImagesCV.remove(key);
            }

        }
    }

    private double lastBack = System.currentTimeMillis();
    private double lastBackProc = System.currentTimeMillis();
    private double lastBackSaved = System.currentTimeMillis();
    private int backIt = 0;
    private void handleImageBack(Image i) {
        if(i != null) {
            final byte[] bytes = getBytes(i);
            final int w = i.getWidth();
            final int h = i.getHeight();
            i.close();
            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, w, h, null);

            double now = System.currentTimeMillis();

            /**
             * Decide if frame is to be processed or not
             */
            if(Preferences.backImagesProcessingActivated(prefs) && now - lastBackProc >= (1000 / BACK_PROCESSING_FPS) ) {
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        imgProc.processImageBack(bytes.clone(), w, h);
                    }
                }).start();*/
                byte[] processedImg = imgProc.processImageBack(bytes.clone(), w, h);
                yuvimage = new YuvImage(processedImg, ImageFormat.NV21, 320, 240, null);
                mBackgroundHandler.post( new ImageSaver(yuvimage, "back") );
                lastBackProc = now;
            }

            /**
             * Store the received image (either processed or raw) and write it to file
             */
            if(now - lastBack >= (1000/(1+BACK_MAX_FPS)) ) {
                double latestFPS = 1000 / (now - lastBack);
                BACK_AVG_FPS = 0.995*BACK_AVG_FPS + 0.005*latestFPS;
                //Log.d("FPS back", BACK_AVG_FPS+", " +latestFPS);

                backImagesCV.put(backIt, bytes);
                backIt++;
                lastBack = now;


            }

            /**
             * Only store the last ten seconds in the image buffer
             */
            if(backImagesCV.size() > (10*BACK_MAX_FPS) ) {
                int key = backImagesCV.keyAt(0);
                backImagesCV.remove(key);
            }

        }
    }

    private byte[] getBytes(Image img) {
        ByteBuffer buffer0 = img.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = img.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();

        byte[] bytes = new byte[buffer0_size + buffer2_size];
        buffer0.get(bytes, 0, buffer0_size);
        buffer2.get(bytes, buffer0_size, buffer2_size);

        return bytes;
    }

    public static Bitmap getBitmapImageFromYUV(YuvImage img, int width, int height) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compressToJpeg(new Rect(0, 0, width, height), 80, baos);

        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
    }

    private static class VideoSaver {

        private SparseArray<YuvImage> images = new SparseArray<>();
        private int FPS, w, h;
        private String mode;

        SequenceEncoderWrapper encoder;
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName;
        String filePath = baseDir + "/SensorPlatform/videos/";
        File mFile;

        public VideoSaver(SparseArray<YuvImage> list, int FPS, int width, int height, String mode, long timestamp) {
            this.images = copy(list);
            this.FPS = FPS;
            this.w = width;
            this.h = height;
            this.mode = mode;

            fileName = "Video-" + timestamp + ".mp4";

            if(mode == "front" && frontSaving == true) return;
            if(mode == "back" && backSaving == true) return;

            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            // subdirectory /front or /back
            filePath += mode;
            folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            mFile = new File(filePath + File.separator + fileName);

            save();
        }

        private void save() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if(mode == "front") frontSaving = true;
                        if(mode == "back") backSaving = true;
                        Log.d("VIDEO", "Trying to save..." + images.size() + " frames, FPS " + FPS);
                        double start = System.currentTimeMillis();
                        encoder = new SequenceEncoderWrapper(mFile, images.size(), FPS, w, h);
                        for (int i = 0; i < images.size(); i++) {
                            YuvImage image = images.get(i);
                            if (image != null) {
                                Bitmap bi = getBitmapImageFromYUV(image, image.getWidth(), image.getHeight());
                                encoder.encodeImage(bi);
                            }
                        }
                        encoder.finish();

                        double delta = (System.currentTimeMillis() - start)/1000;
                        Log.d("VIDEO", "Saving finished in " + delta + "s!" );
                        if(mode == "front") frontSaving = false;
                        if(mode == "back") backSaving = false;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private SparseArray<YuvImage> copy(SparseArray<YuvImage> list) {
            SparseArray<YuvImage> temp = new SparseArray<>();
            for(int i=0; i<list.size(); i++) {
                temp.put(i, list.valueAt(i));
            }
            return temp;
        }

    }

    private static class VideoSaver2 {
        private SparseArray<byte[]> images;
        private int FPS, w, h;
        private String mode;

        VideoWriter videoWriter;
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName;
        String filePath = baseDir + "/SensorPlatform/videos/";
        File mFile;

        public VideoSaver2(SparseArray<byte[]> list, int FPS, int width, int height, String mode, long timestamp) {
            this.images = copyByteList(list);
            this.FPS = FPS;
            this.w = width;
            this.h = height;
            this.mode = mode;

            fileName = "Video-" + timestamp + ".avi";

            if(mode == "front" && frontSaving == true) return;
            if(mode == "back" && backSaving == true) return;

            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            // subdirectory /front or /back
            filePath += mode;
            folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            mFile = new File(filePath + File.separator + fileName);
            videoWriter = new VideoWriter(filePath + File.separator + fileName, VideoWriter.fourcc('M','J','P','G'), FPS, new Size(w,h));

            save();
        }

        private void save() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if(mode == "front") frontSaving = true;
                        if(mode == "back") backSaving = true;
                        videoWriter.open(filePath + File.separator + fileName, VideoWriter.fourcc('M','J','P','G'), FPS, new Size(w,h));
                        Log.d("VIDEO", "Trying to save..." + images.size() + " frames, FPS " + FPS + ", path: " + filePath + File.separator + fileName + ", "+w+h);
                        double start = System.currentTimeMillis();
                        for (int i = 0; i < images.size(); i++) {
                            byte[] image = images.get(i);
                            Mat imgMat = new Mat(h + h/2, w, CvType.CV_8UC1);
                            imgMat.put(0,0,image);
                            Mat gray = new Mat(imgMat.height(), imgMat.width(), imgMat.depth());
                            Imgproc.cvtColor(imgMat, gray, Imgproc.COLOR_YUV2GRAY_I420);
                            Mat rgbMat = new Mat(h,w,CvType.CV_8UC3);
                            Imgproc.cvtColor(gray, rgbMat, Imgproc.COLOR_GRAY2RGB);

                            videoWriter.write(rgbMat);
                            rgbMat.release();
                            gray.release();
                            imgMat.release();
                            Log.d("VIDEO", "save image " + rgbMat);
                        }
                        videoWriter.release();

                        double delta = (System.currentTimeMillis() - start)/1000;
                        Log.d("VIDEO", "Saving finished in " + delta + "s!" );
                        if(mode == "front") frontSaving = false;
                        if(mode == "back") backSaving = false;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private SparseArray<byte[]> copyByteList(SparseArray<byte[]> list) {
            SparseArray<byte[]> temp = new SparseArray<>();
            for(int i=0; i<list.size(); i++) {
                temp.put(i, list.valueAt(i));
            }
            return temp;
        }

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final YuvImage mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "IMG-" + System.nanoTime() + ".jpg";
        String filePath;

        public ImageSaver(YuvImage image, String path) {
            mImage = image;
            // image directory
            filePath = baseDir + "/SensorPlatform/images/";
            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            // subdirectory /front or /back
            filePath += path;
            folder = new File(filePath);
            if (!folder.exists()) {
                 folder.mkdir();
            }

            mFile = new File(filePath + File.separator + fileName);
        }

        @Override
        public void run() {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            mImage.compressToJpeg(new Rect(0, 0, mImage.getWidth(),mImage.getHeight()), 100, baos);

            byte[] jpgData = baos.toByteArray();

            FileOutputStream output;
            try {
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
                output = new FileOutputStream(mFile);
                output.write(jpgData);
                output.getFD().sync();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }




    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
