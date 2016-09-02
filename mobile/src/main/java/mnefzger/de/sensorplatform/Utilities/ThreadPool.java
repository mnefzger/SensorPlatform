package mnefzger.de.sensorplatform.Utilities;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private static ThreadPool mInstance;
    private ThreadPoolExecutor mThreadPoolExec;
    private static int MAX_POOL_SIZE;
    private static final int KEEP_ALIVE = 20;
    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();

    public static void post(Runnable runnable) {
        if (mInstance == null) {
            mInstance = new ThreadPool();
        }
        try{
            mInstance.mThreadPoolExec.execute(runnable);
        } catch (RejectedExecutionException e) {
            Log.e("THREADPOOL", "Execution rejected: " + e);
        }
    }

    private ThreadPool() {
        int coreNum = Runtime.getRuntime().availableProcessors();
        MAX_POOL_SIZE = coreNum * 2;
        mThreadPoolExec = new ThreadPoolExecutor(
                coreNum,
                MAX_POOL_SIZE,
                KEEP_ALIVE,
                TimeUnit.SECONDS,
                workQueue);
    }

    public static void finish() {
        if(mInstance != null)
            mInstance.mThreadPoolExec.shutdown();
    }
}