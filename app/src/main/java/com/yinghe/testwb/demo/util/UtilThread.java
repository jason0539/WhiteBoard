package com.yinghe.testwb.demo.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuzhenhui on 2018/3/13.
 */

public class UtilThread {

    //非UI线程池
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    //UI线程
    private static Handler mUiHandler = new Handler(Looper.getMainLooper());

    /**
     * 执行一个非UI线程的任务
     */
    public static final void execute(final Runnable runnable) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
//                MLog.d(MLog.TAG_THREAD, "UtilThread->run 线程：" + Thread.currentThread().getName());
                runnable.run();
            }
        });
    }

    /**
     * 执行一个UI线程任务
     */
    public static final void runOnUiThread(Runnable runnable) {
        mUiHandler.post(runnable);
    }

    public static final void postOnUiThreadDelay(Runnable runnable, long delayMills) {
        mUiHandler.postDelayed(runnable, delayMills);
    }
}
