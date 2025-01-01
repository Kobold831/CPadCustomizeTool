package com.saradabar.cpadcustomizetool.util;

import android.content.Context;

import com.saradabar.cpadcustomizetool.data.task.IDchaUtilTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class DchaUtilServiceUtil {

    final Object objLock = new Object();

    Context mContext;
    IDchaUtilService mDchaUtilService;

    public DchaUtilServiceUtil(Context context, IDchaUtilService iDchaUtilService) {
        mContext = context;
        mDchaUtilService = iDchaUtilService;
    }

    public boolean setForcedDisplaySize(int i, int i1) {
        try {
            if (mDchaUtilService != null) {
                return mDchaUtilService.setForcedDisplaySize(i, i1);
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try {
                    new IDchaUtilTask().execute(mContext, iDchaUtilTaskListener());
                    synchronized (objLock) {
                        objLock.wait();
                    }
                } catch (Exception ignored) {
                }
                executorService.shutdown();
            });

            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                return false;
            }

            if (mDchaUtilService == null) {
                return false;
            }
            return mDchaUtilService.setForcedDisplaySize(i, i1);
        } catch (Exception ignored) {
            return false;
        }
    }

    private IDchaUtilTask.Listener iDchaUtilTaskListener() {
        return new IDchaUtilTask.Listener() {

            @Override
            public void onSuccess(IDchaUtilService iDchaUtilService) {
                mDchaUtilService = iDchaUtilService;
                synchronized (objLock) {
                    objLock.notify();
                }
            }

            @Override
            public void onFailure() {
                synchronized (objLock) {
                    objLock.notify();
                }
            }
        };
    }
}
