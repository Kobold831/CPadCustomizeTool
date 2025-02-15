package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
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
            if (!Common.isCfmDialog(mContext)) {
                return false;
            }

            try {
                BenesseExtension.setDchaState(i);
                return true;
            } catch (Exception ignored) {
                if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                    mDchaService.setSetupStatus(i);
                } else {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, i);
                }
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean hideNavigationBar(boolean b) {
        try {
            if (!Common.isCfmDialog(mContext)) {
                return false;
            }

            if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                mDchaService.hideNavigationBar(b);
            } else {
                if (b) {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 1);
                } else {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

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
