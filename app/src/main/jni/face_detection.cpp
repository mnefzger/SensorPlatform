#include <string>
#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#define  LOG_TAG    "Face_Detection_Native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C" {
    JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address);
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

JNIEXPORT jintArray Java_mnefzger_de_sensorplatform_ImageProcessor_nAsmFindFace(JNIEnv *env, jobject obj, jlong address) {
	jintArray result;
	Mat image = *((Mat*) address);

    /*
    Mat mBgr(image.rows, image.cols, image.depth());
    cvtColor(image, mBgr, CV_YUV2BGR_I420);
    */

    Point2f center(image.cols/2, image.rows/2);
    Mat rotationMatrix = getRotationMatrix2D(center, -90, 1);
    Rect bbox = RotatedRect(center, image.size(), 90).boundingRect();
    rotationMatrix.at<double>(0,2) += bbox.width/2.0 - center.x;
    rotationMatrix.at<double>(1,2) += bbox.height/2.0 - center.y;
    Mat dst;
    warpAffine(image, dst, rotationMatrix, bbox.size());

    Mat mini;
    Size small(dst.cols/4, dst.rows/4);
    resize(dst, mini, small);

    //Mat gray(dst.rows, dst.cols, CV_8UC1);
    //cvtColor(dst, gray, CV_BGR2GRAY);

    vector< Rect > faces;
    faceCascade.detectMultiScale(mini, faces, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE, Size(30, 30));

    if (faces.size()) {
    	result = env->NewIntArray(4);
    	jint tmp_array[4];

    	tmp_array[0] = faces[0].x;
    	tmp_array[1] = faces[0].y;
    	tmp_array[2] = faces[0].width;
    	tmp_array[3] = faces[0].height;

		env->SetIntArrayRegion(result, 0, 4, tmp_array);

    } else {
        result = env->NewIntArray(0);
    }

    return result;
}





