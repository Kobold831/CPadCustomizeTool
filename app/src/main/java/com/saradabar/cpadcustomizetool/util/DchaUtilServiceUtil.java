package com.saradabar.cpadcustomizetool.util;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class DchaUtilServiceUtil {

    IDchaUtilService mDchaUtilService;

    public DchaUtilServiceUtil(IDchaUtilService iDchaUtilService) {
        mDchaUtilService = iDchaUtilService;
    }

    public boolean setForcedDisplaySize(int i, int i1) {
        try {
            return mDchaUtilService.setForcedDisplaySize(i, i1);
        } catch (Exception ignored) {
            return false;
        }
    }
}
