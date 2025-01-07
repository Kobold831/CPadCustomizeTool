package com.saradabar.cpadcustomizetool.util;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class DchaUtilServiceUtil {

    IDchaUtilService mDchaUtilService;

    public DchaUtilServiceUtil(IDchaUtilService iDchaUtilService) {
        mDchaUtilService = iDchaUtilService;
    }

    public boolean setForcedDisplaySize(int i, int i1) {
        try {
            if (mDchaUtilService == null) {
                return false;
            }
            mDchaUtilService.setForcedDisplaySize(i, i1);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
