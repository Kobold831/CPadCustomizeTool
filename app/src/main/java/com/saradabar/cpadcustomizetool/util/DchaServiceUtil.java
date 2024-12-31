package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.provider.Settings;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class DchaServiceUtil {

    Context mContext;
    IDchaService mDchaService;

    public DchaServiceUtil(Context context, IDchaService iDchaService) {
        mContext = context;
        mDchaService = iDchaService;
    }

    public boolean setSetupStatus(int i) {
        try {
            if (Preferences.load(mContext, Constants.KEY_FLAG_SETTINGS_DCHA, false)) {
                if (Common.isCfmDialog(mContext)) {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, i);
                } else {
                    return false;
                }
            } else {
                mDchaService.setSetupStatus(i);
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public boolean hideNavigationBar(boolean b) {
        try {
            if (Preferences.load(mContext, Constants.KEY_FLAG_SETTINGS_DCHA, false)) {
                if (Common.isCfmDialog(mContext)) {
                    if (b) {
                        Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 1);
                    } else {
                        Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                    }
                } else {
                    return false;
                }
            } else {
                mDchaService.hideNavigationBar(b);
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public boolean setPreferredHomeApp(String s, String s1) {
        try {
            mDchaService.clearDefaultPreferredApp(s);
            mDchaService.setDefaultPreferredHomeApp(s1);
        } catch (Exception ignored) {
            return false;
        }
        return true;
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
