package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.task.IDchaTask;

/** @noinspection unused*/
public class DchaServiceUtil {

    final Context mContext;

    public DchaServiceUtil(Context context) {
        mContext = context;
    }

    public interface Listener {
        void onResult(Object object);
    }

    public void clearDefaultPreferredApp(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.clearDefaultPreferredApp(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void copyUpdateImage(String srcFilePath, String dstFilePath, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.copyUpdateImage(srcFilePath, dstFilePath.startsWith("/cache") ? dstFilePath : "/cache/../" + dstFilePath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void hideNavigationBar(boolean hide, Listener listener) {
        if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false) &&
                !Preferences.load(mContext, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            // Dchaを使う設定かつ通常環境モードではない
            new IDchaTask().execute(mContext, iDchaService -> {
                if (iDchaService == null) {
                    listener.onResult(false);
                    return;
                }

                try {
                    iDchaService.hideNavigationBar(hide);
                    listener.onResult(true);
                } catch (RemoteException e) {
                    listener.onResult(e.getMessage());
                }
            });
        } else {
            // 設定で変更
            try {
                Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, hide ? 1 : 0);
                listener.onResult(true);
            } catch (RuntimeException e) {
                listener.onResult(e.getMessage());
            }
        }
    }

    public void installApp(String path, int installFlag, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.installApp(path, installFlag));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void rebootPad(int rebootMode, String srcFile, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.rebootPad(rebootMode, srcFile);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void removeTask(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.removeTask(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setDefaultPreferredHomeApp(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.setDefaultPreferredHomeApp(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setSetupStatus(int status, Listener listener) {
        if (Common.isBenesseExtensionExist("setDchaState") && !Common.isCT3() &&
                !Preferences.load(mContext, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            // BenesseExtensionが存在かつCT3ではないかつ通常環境モードではない
            BenesseExtension.setDchaState(status);
            listener.onResult(true);
            return;
        }

        if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false) &&
                !Preferences.load(mContext, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            // Dchaを使う設定かつ通常環境モードではない
            new IDchaTask().execute(mContext, iDchaService -> {
                if (iDchaService == null) {
                    listener.onResult(false);
                    return;
                }

                try {
                    iDchaService.setSetupStatus(status);
                    listener.onResult(true);
                } catch (RemoteException e) {
                    listener.onResult(e.getMessage());
                }
            });
        } else {
            // 設定で変更
            Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, status);
            listener.onResult(true);
        }
    }

    public void uninstallApp(String packageName, int uninstallFlag, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.uninstallApp(packageName, uninstallFlag));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setPreferredHomeApp(String s, String s1, Listener listener) {
        clearDefaultPreferredApp(s, object -> {
            if (object.equals(true)) {
                setDefaultPreferredHomeApp(s1, object1 -> {
                    if (object1.equals(true)) {
                        listener.onResult(true);
                    } else {
                        listener.onResult(false);
                    }
                });
            } else {
                listener.onResult(false);
            }
        });
    }

    public void execSystemUpdate(String s, int i, Listener listener) {
        String updateFile = "/cache/update.zip";

        copyUpdateImage(s, updateFile, object -> {
            if (object.equals(true)) {
                rebootPad(i, updateFile, object1 -> {
                    if (object1.equals(true)) {
                        listener.onResult(true);
                    } else {
                        listener.onResult(false);
                    }
                });
            } else {
                listener.onResult(false);
            }
        });
    }
}
