package mnefzger.de.sensorplatform;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;


public class ImageModule implements IEventCallback{
    private IDataCallback callback;
    private Context context;

    private CameraManager cameraManager;
    private CameraDevice camera_front;
    private CameraDevice camera_back;
    private CameraCaptureSession session_front;
    private CameraCaptureSession session_back;
    private ImageReader imageReader_front;
    private ImageReader imageReader_back;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private final String TAG = "CAMERA";

    public ImageModule(SensorPlatformController controller, Activity app) {
        verifyCameraPermissions(app);
        context = app;
        callback = (IDataCallback) controller;
        cameraManager = (CameraManager) app.getSystemService(Activity.CAMERA_SERVICE);

    }

    public void startCapture() {
        startBackgroundThread();
        open("0");
        open("1");
    }

    public void stopCapture() {
        stopBackgroundThread();
        camera_front.close();
        camera_back.close();
    }

    private void open(String id) {
        try {
            int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            if(permission == PackageManager.PERMISSION_GRANTED) {
                if(id == "0") {
                    cameraManager.openCamera(id, backCameraStateCallback, null );
                    imageReader_front = ImageReader.newInstance(240, 320, ImageFormat.JPEG, 5);
                    imageReader_front.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
                }
                if(id == "1") {
                    cameraManager.openCamera(id, frontCameraStateCallback, null );
                    imageReader_back = ImageReader.newInstance(240, 320, ImageFormat.JPEG, 5);
                    imageReader_back.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
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
        public void onDisconnected(CameraDevice camera) {}

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
        public void onDisconnected(CameraDevice camera) {}

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

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader){
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
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

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "IMG-" + System.nanoTime() + ".jpg";
        String filePath = baseDir + "/SensorPlatform/images";

        public ImageSaver(Image image) {
            mImage = image;
            mFile = new File(filePath + File.separator + fileName);
            Log.i("IO_IMAGE", "writing to:" + filePath);
            try {
                File folder = new File(filePath);
                if (!folder.exists()) {
                    folder.mkdir();
                }
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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

    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA,
    };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     * @param activity
     */
    public static void verifyCameraPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,
                    PERMISSIONS_CAMERA, REQUEST_CAMERA);
        }
    }
}
