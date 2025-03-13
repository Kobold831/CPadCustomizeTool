package com.saradabar.cpadcustomizetool.util;

import android.os.BenesseExtension;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

/** @noinspection unused*/
public class DchaUtilServiceUtil {

    IDchaUtilService mUtilService;

    public DchaUtilServiceUtil(IDchaUtilService mDchaUtilService) {
        mUtilService = mDchaUtilService;
    }

    public void clearDefaultPreferredApp(String packageName) {
        try {
            mUtilService.clearDefaultPreferredApp(packageName);
        } catch (Exception ignored) {
        }
    }

    public boolean copyFile(String srcFilePath, String dstFilePath) {
        try {
            return mUtilService.copyFile(srcFilePath, dstFilePath);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean copyDirectory(String srcFilePath, String dstFilePath, boolean makeTopDir) {
        try {
            return mUtilService.copyDirectory(srcFilePath, dstFilePath, makeTopDir);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean deleteFile(String path) {
        try {
            return mUtilService.deleteFile(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean existsFile(String path) {
        try {
            return mUtilService.existsFile(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    public String getCanonicalExternalPath(String linkPath) {
        try {
            return mUtilService.getCanonicalExternalPath(linkPath);
        } catch (Exception ignored) {
            return null;
        }
    }

    public int[] getDisplaySize() {
        try {
            return mUtilService.getDisplaySize();
        } catch (Exception ignored) {
            return new int[] {1280, 800};
        }
    }

    public int[] getLcdSize() {
        try {
            return mUtilService.getLcdSize();
        } catch (Exception ignored) {
            return new int[] {1280, 800};
        }
    }

    public int getUserCount() {
        try {
            return mUtilService.getUserCount();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public void hideNavigationBar(boolean hide) {
        try {
            mUtilService.hideNavigationBar(hide);
        } catch (Exception ignored) {
        }
    }

    public String[] listFiles(String path) {
        try {
            return mUtilService.listFiles(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean makeDir(String path, String dirname) {
        try {
            return mUtilService.makeDir(path, dirname);
        } catch (Exception ignored) {
            return false;
        }
    }

    public void sdUnmount() {
        try {
            mUtilService.sdUnmount();
        } catch (Exception ignored) {
        }
    }

    public boolean setForcedDisplaySize(int width, int height) {
        try {
            if (mUtilService == null) {
                try {
                    // WRITE_SECURE_SETTINGS が必要
                    //noinspection ResultOfMethodCallIgnored
                    BenesseExtension.setForcedDisplaySize(width, height);
                    return true;
                } catch (NoSuchMethodError | NoClassDefFoundError | Exception ignored) {
                    return false;
                }
            }
            mUtilService.setForcedDisplaySize(width, height);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
