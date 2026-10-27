package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.data.task.IDchaTask;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class DchaServiceUtil {

    final Context mContext;

    public DchaServiceUtil(Context context) {
        mContext = context;
    }

    public interface Listener {
        void onResult(Object object);
    }

    private void execute(@NonNull DchaCall call, @NonNull Listener listener) {
        new IDchaTask().execute(mContext, mDchaService -> {
            Object result;

            if (mDchaService == null) {
                result = false;
            } else {
                try {
                    result = call.run(mDchaService);
                } catch (RemoteException e) {
                    result = e.getMessage();
                }
            }

            Object finalResult = result;
            AppExecutors.main(() -> listener.onResult(finalResult));
        });
    }

    private interface DchaCall {
        Object run(@NonNull IDchaService mDchaService) throws RemoteException;
    }

    public void hideNavigationBar(boolean hide, Listener listener) {
        if (Common.isFlagEnabled(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA) &&
                !Common.isFlagEnabled(mContext, Constants.KEY_FLAG_NORMAL_ENV)) {
            // Dchaを使う設定かつ通常環境モードではない
            execute(mDchaService -> {
                mDchaService.hideNavigationBar(hide);
                return true;
            }, listener);
        } else {
            // 設定で変更
            try {
                Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, hide ? Constants.SETTING_ON : Constants.SETTING_OFF);
                listener.onResult(true);
            } catch (RuntimeException e) {
                listener.onResult(e.getMessage());
            }
        }
    }

    public void installApp(String path, Listener listener) {
        execute(mDchaService -> mDchaService.installApp(path, 2), listener);
    }

    public void rebootPad(Listener listener) {
        execute(mDchaService -> {
            mDchaService.rebootPad(0, null);
            return true;
        }, listener);
    }

    public void removeTask(String packageName, Listener listener) {
        execute(mDchaService -> {
            mDchaService.removeTask(packageName);
            return true;
        }, listener);
    }

    public void setSetupStatus(int status, Listener listener) {
        if ((Common.isCTX() || Common.isCTZ()) &&
                !Common.isFlagEnabled(mContext, Constants.KEY_FLAG_NORMAL_ENV)) {
            // Neo/Next でかつ通常環境モードではない
            BenesseExtension.setDchaState(status);
            listener.onResult(true);
            return;
        }

        if (Common.isFlagEnabled(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA) &&
                !Common.isFlagEnabled(mContext, Constants.KEY_FLAG_NORMAL_ENV)) {
            // Dchaを使う設定かつ通常環境モードではない
            execute(mDchaService -> {
                mDchaService.setSetupStatus(status);
                return true;
            }, listener);
        } else {
            // 設定で変更
            Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, status);
            listener.onResult(true);
        }
    }

    public void uninstallApp(String packageName, Listener listener) {
        execute(mDchaService -> mDchaService.uninstallApp(packageName, 0), listener);
    }

    public void setPreferredHomeApp(String clearPackage, String setPackage, Listener listener) {
        execute(mDchaService -> {
            mDchaService.clearDefaultPreferredApp(clearPackage);
            mDchaService.setDefaultPreferredHomeApp(setPackage);
            return true;
        }, listener);
    }
}
