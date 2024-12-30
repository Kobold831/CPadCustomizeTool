package com.saradabar.cpadcustomizetool.util;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class DchaServiceUtil {

    IDchaService mDchaService;

    public DchaServiceUtil(IDchaService iDchaService) {
        mDchaService = iDchaService;
    }

    public void setSetupStatus(int i) {
        try {
            mDchaService.setSetupStatus(i);
        } catch (Exception ignored) {
        }
    }

    public void hideNavigationBar(boolean b) {
        try {
            mDchaService.hideNavigationBar(b);
        } catch (Exception ignored) {
        }
    }

    public void rebootPad(int i, String s) {
        try {
            mDchaService.rebootPad(i, s);
        } catch (Exception ignored) {
        }
    }

    public void setPreferredHomeApp(String s, String s1) {
        try {
            mDchaService.clearDefaultPreferredApp(s);
            mDchaService.setDefaultPreferredHomeApp(s1);
        } catch (Exception ignored) {
        }
    }

    public boolean execSystemUpdate(String s, int i) {
        try {
            if (!copyUpdateImage(s, "/cache/update.zip")) {
                return false;
            }
            mDchaService.rebootPad(i, "/cache/update.zip");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean installApp(String s, int i) {
        try {
            return mDchaService.installApp(s, i);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean copyUpdateImage(String s, String s1) {
        try {
            return mDchaService.copyUpdateImage(s, s1);
        } catch (Exception ignored) {
            return false;
        }
    }
}
