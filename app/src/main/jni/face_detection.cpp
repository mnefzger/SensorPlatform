#include <string>
#include <jni.h>
#include <math.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#define  LOG_TAG    "Face_Detection_Native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C" {
    JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address, jlong returnadress, jint height, jint width);
}

using namespace cv;
using namespace std;

CascadeClassifier faceCascade;

int initAsm();
int initialized = initAsm();

int initAsm() {
    string haarFaceCascadePath = "/storage/emulated/0/SensorPlatform/haarcascade_frontalface_alt.xml";

    if (initialized == 0) {
		faceCascade.load(haarFaceCascadePath);
		initialized = 1;
    }

	LOGI("ASM init complete.");

	return 1;
}

JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address, jlong returnadress, jint height, jint width) {
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

    Point2f center( gray.rows/2, gray.cols/2 );
    Mat rotationMatrix = getRotationMatrix2D(center, -180, 1);
    /*
    Rect bbox = RotatedRect(center, image.size(), 90).boundingRect();
    int one = ceil(bbox.width/2);
    int two = ceil(bbox.height/2);
    if( fmod( (one - center.x), 2 ) != 0) one += 1;
    if( fmod( (two - center.y), 2 ) != 0) two += 1;
    rotationMatrix.at<double>(0,2) += one - center.x;
    rotationMatrix.at<double>(1,2) += two - center.y;
    */
    Mat rotated;
    warpAffine(gray, rotated, rotationMatrix, gray.size());

    vector< Rect > faces;
    faceCascade.detectMultiScale(rotated, faces, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE, Size(30, 30));

    if (faces.size()) {
    	result = env->NewIntArray(4);
    	jint tmp_array[4];

    	tmp_array[0] = faces[0].x;
    	tmp_array[1] = faces[0].y;
    	tmp_array[2] = faces[0].width;
    	tmp_array[3] = faces[0].height;

		env->SetIntArrayRegion(result, 0, 4, tmp_array);

        rectangle(rotated, faces[0], CV_RGB(255, 255, 255), 1);

    } else {
        result = env->NewIntArray(0);
    }


    Mat bgr(rotated.rows, rotated.cols, CV_8UC3);
    cvtColor(rotated, bgr, COLOR_GRAY2BGR);

    Mat yuv;
    cvtColor(bgr, yuv, COLOR_BGR2YUV_I420);

    Mat* mat = (Mat*) returnadress;
    mat->create(yuv.rows, yuv.cols, CV_8UC1);
    memcpy(mat->data, yuv.data, mat->rows * mat->cols );

    return result;
}





