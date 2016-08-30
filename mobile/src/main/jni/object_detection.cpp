#include <string>
#include <jni.h>
#include <math.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#define  LOG_TAG    "Object_Detection_Native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C" {
    JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address, jlong returnadress);
    JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nAsmFindCars(JNIEnv *env, jobject obj, jlong address, jlong returnadress);
    JNIEXPORT jint Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nInitCascades(JNIEnv *env, jobject obj);
}

using namespace cv;
using namespace std;

CascadeClassifier faceCascadeHaar;
CascadeClassifier faceCascadeLBP;
CascadeClassifier faceCascadeLBP2;

JNIEXPORT jint Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nInitCascades(JNIEnv *env, jobject obj) {
    string haarFaceCascadePath = "/storage/emulated/0/SensorPlatform/haarcascade_frontalface_alt.xml";
    string lbpFaceCascadePath = "/storage/emulated/0/SensorPlatform/lbpcascade_frontalface.xml";
    string lbpFaceCascadePath2 = "/storage/emulated/0/SensorPlatform/visionary_FACES_01_LBP.xml";

	faceCascadeHaar.load(haarFaceCascadePath);
	faceCascadeLBP.load(lbpFaceCascadePath);
	faceCascadeLBP2.load(lbpFaceCascadePath2);

	LOGI("Init cascades complete.");

	return 1;
}

JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address, jlong returnadress) {
	jintArray result;
	Mat image = *((Mat*) address);

    /*
    * Resizing leads to weird behaviour
    *
    Mat mini;
    Size small( 200 , 150 );
    resize(image, mini, small);
    */

    Mat gray(image.rows, image.cols, image.depth());
    cvtColor(image, gray, COLOR_YUV2GRAY_I420);

    /**
    *   Rotating the image is only necessary certain device orientations
    *
    Point2f center( gray.rows/2, gray.cols/2 );
    Mat rotationMatrix = getRotationMatrix2D(center, -180, 1);
    Mat rotated;
    warpAffine(gray, rotated, rotationMatrix, gray.size());
    flip(gray, rotated, 0);
    */

    vector< Rect > faces;
    //faceCascadeHaar.detectMultiScale(gray, faces, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE, Size(30, 30));
    faceCascadeLBP.detectMultiScale( gray, faces, 1.1, 2, 0, Size(30, 30) );
    //faceCascadeLBP2.detectMultiScale( gray, faces, 1.1, 2, 0, Size(30, 30) );


    if (faces.size()) {
    	result = env->NewIntArray(4);
    	jint tmp_array[4];

    	tmp_array[0] = faces[0].x;
    	tmp_array[1] = faces[0].y;
    	tmp_array[2] = faces[0].width;
    	tmp_array[3] = faces[0].height;

		env->SetIntArrayRegion(result, 0, 4, tmp_array);

        rectangle(gray, faces[0], CV_RGB(255, 255, 255), 1);

    } else {
        result = env->NewIntArray(0);
    }

    /*
    Mat bgr(gray.rows, gray.cols, CV_8UC3);
    cvtColor(gray, bgr, COLOR_GRAY2BGR);

    Mat yuv;
    cvtColor(bgr, yuv, COLOR_BGR2YUV_I420);

    Mat* mat = (Mat*) returnadress;
    mat->create(yuv.rows, yuv.cols, CV_8UC1);
    memcpy(mat->data, yuv.data, mat->rows * mat->cols );
    */

    return result;
}

JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_Processors_ImageProcessor_nAsmFindCars(JNIEnv *env, jobject obj, jlong address, jlong returnadress) {
	jintArray result;
	Mat image = *((Mat*) address);

    Mat gray(image.rows, image.cols, image.depth());
    cvtColor(image, gray, COLOR_YUV2GRAY_I420);
    flip(gray, gray, -1);

    vector< Rect > cars;
    //faceCascadeLBP.detectMultiScale( gray, cars, 1.1, 2, 0, Size(30, 30) );


    if (cars.size()) {
    	result = env->NewIntArray(4);
    	jint tmp_array[4];

    	tmp_array[0] = cars[0].x;
    	tmp_array[1] = cars[0].y;
    	tmp_array[2] = cars[0].width;
    	tmp_array[3] = cars[0].height;

		env->SetIntArrayRegion(result, 0, 4, tmp_array);

        rectangle(gray, cars[0], CV_RGB(255, 255, 255), 1);

    } else {
        result = env->NewIntArray(0);
    }

    Mat bgr(gray.rows, gray.cols, CV_8UC3);
    cvtColor(gray, bgr, COLOR_GRAY2BGR);

    Mat yuv;
    cvtColor(bgr, yuv, COLOR_BGR2YUV_I420);

    Mat* mat = (Mat*) returnadress;
    mat->create(yuv.rows, yuv.cols, CV_8UC1);
    memcpy(mat->data, yuv.data, mat->rows * mat->cols );


    return result;
}



