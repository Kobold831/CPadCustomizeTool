package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
import android.provider.Settings;

import jp.co.benesse.dcha.dchaservice.IDchaService;

/** @noinspection unused*/
public class DchaServiceUtil {

    Context mContext;
    IDchaService mDchaService;

    public DchaServiceUtil(Context context, IDchaService iDchaService) {
        mContext = context;
        mDchaService = iDchaService;
    }

    public void cancelSetup() {
        try {
            mDchaService.cancelSetup();
        } catch (Exception ignored) {
        }
    }

    public boolean checkPadRooted() {
        try {
            return mDchaService.checkPadRooted();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void clearDefaultPreferredApp(String packageName) {
        try {
            mDchaService.clearDefaultPreferredApp(packageName);
        } catch (Exception ignored) {
        }
    }

    public boolean copyFile(String srcFilePath, String dstFilePath) {
        try {
            return mDchaService.copyFile(srcFilePath, dstFilePath);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean copyUpdateImage(String srcFilePath, String dstFilePath) {
        try {
            return mDchaService.copyFile(srcFilePath, dstFilePath.startsWith("/cache") ? dstFilePath : "/cache/../" + dstFilePath);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean deleteFile(String path) {
        try {
            return mDchaService.deleteFile(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    public void disableADB() {
        try {
            mDchaService.disableADB();
        } catch (Exception ignored) {
            Settings.Secure.putInt(mContext.getContentResolver(), "adb_enabled", 0);
        }
    }

    public String getForegroundPackageName() {
        try {
            return mDchaService.getForegroundPackageName();
        } catch (Exception ignored) {
            return null;
        }
    }

    public int getSetupStatus() {
        try {
            return BenesseExtension.getDchaState();
        } catch (NoSuchMethodError | NoClassDefFoundError | Exception ignored) {
            try {
                return mDchaService.getSetupStatus();
            } catch (Exception unused) {
                return -1;
            }
        }
    }

    public int getUserCount() {
        try {
            return mDchaService.getUserCount();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public void hideNavigationBar(boolean hide) {
        try {
            if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                mDchaService.hideNavigationBar(hide);
            } else {
                Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, hide ? 1 : 0);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean installApp(String path, int installFlag) {
        try {
            return mDchaService.installApp(path, installFlag);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isDeviceEncryptionEnabled() {
        try {
            return mDchaService.isDeviceEncryptionEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void rebootPad(int rebootMode, String srcFile) {
        try {
            mDchaService.rebootPad(rebootMode, srcFile);
        } catch (Exception ignored) {
        }
    }

    public void removeTask(String packageName) {
        try {
            mDchaService.removeTask(packageName);
        } catch (Exception ignored) {
        }
    }

    public void sdUnmount() {
        try {
            mDchaService.sdUnmount();
        } catch (Exception ignored) {
        }
    }

    public void setDefaultParam() {
        try {
            mDchaService.setDefaultParam();
        } catch (Exception ignored) {
        }
    }

    public void setDefaultPreferredApp(String packageName) {
        try {
            mDchaService.setDefaultPreferredHomeApp(packageName);
        } catch (Exception ignored) {
        }
    }

    public void setPermissionEnforced(boolean enforced) {
        try {
            mDchaService.setPermissionEnforced(enforced);
        } catch (Exception ignored) {
        }
    }

    public void setSetupStatus(int status) {
        try {
            try {
                BenesseExtension.setDchaState(status);
            } catch (NoSuchMethodError | NoClassDefFoundError | Exception ignored) {
                if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                    mDchaService.setSetupStatus(status);
                } else {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, status);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void setSystemTime(String time, String timeFormat) {
        try {
            mDchaService.setSystemTime(time, timeFormat);
        } catch (Exception ignored) {
        }
    }

    public boolean uninstallApp(String packageName, int uninstallFlag) {
        try {
            return mDchaService.uninstallApp(packageName, uninstallFlag);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean verifyUpdateImage(String updateFile) {
        try {
            return mDchaService.verifyUpdateImage(updateFile);
        } catch (Exception ignored) {
            return true;
        }
    }

    /// ---

    public boolean setPreferredHomeApp(String s, String s1) {
        try {
            mDchaService.clearDefaultPreferredApp(s);
            mDchaService.setDefaultPreferredHomeApp(s1);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean execSystemUpdate(String s, int i) {
        try {
            if (mDchaService.copyUpdateImage(s, "/cache/update.zip")) {
                mDchaService.rebootPad(i, "/cache/update.zip");
                return true;
            } else {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
