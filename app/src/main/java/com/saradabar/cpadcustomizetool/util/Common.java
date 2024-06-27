/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.util;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ServiceManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.Display;
import android.view.IWindowManager;

import com.rosan.dhizuku.api.Dhizuku;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class Common {

    public static boolean tryBindDchaService(Context context, IDchaService iDchaService, IDchaUtilService iDchaUtilService, ServiceConnection serviceConnection, boolean isDchaService, int reqCode, int i, int i1, String s, String s1) {
        try {
            if (isDchaService) {
                switch (reqCode) {
                    case Constants.FLAG_SET_DCHA_STATE_0:
                        if (isCfmDialog(context)) {
                            iDchaService.setSetupStatus(0);
                            return true;
                        } else {
                            return false;
                        }
                    case Constants.FLAG_SET_DCHA_STATE_3:
                        if (isCfmDialog(context)) {
                            iDchaService.setSetupStatus(3);
                            return true;
                        } else {
                            return false;
                        }
                    case Constants.FLAG_HIDE_NAVIGATION_BAR:
                        iDchaService.hideNavigationBar(true);
                        return true;
                    case Constants.FLAG_VIEW_NAVIGATION_BAR:
                        iDchaService.hideNavigationBar(false);
                        return true;
                    case Constants.FLAG_REBOOT:
                        iDchaService.rebootPad(i, s);
                        return true;
                    case Constants.FLAG_SET_LAUNCHER:
                        iDchaService.clearDefaultPreferredApp(s);
                        iDchaService.setDefaultPreferredHomeApp(s1);
                        return true;
                    case Constants.FLAG_SYSTEM_UPDATE:
                        if (iDchaService.copyUpdateImage(s, "/cache/update.zip")) {
                            iDchaService.rebootPad(i, "/cache/update.zip");
                            return true;
                        } else {
                            return false;
                        }
                    case Constants.FLAG_INSTALL_PACKAGE:
                        return iDchaService.installApp(s, i);
                    case Constants.FLAG_COPY_UPDATE_IMAGE:
                        return iDchaService.copyUpdateImage(s, s1);
                    case Constants.FLAG_CHECK:
                        return context.getApplicationContext().bindService(Constants.DCHA_SERVICE, serviceConnection, Context.BIND_AUTO_CREATE);
                    case Constants.FLAG_TEST:
                        return true;
                    default:
                        return false;
                }
            } else {
                switch (reqCode) {
                    case Constants.FLAG_CHECK:
                        return context.getApplicationContext().bindService(Constants.DCHA_UTIL_SERVICE, serviceConnection, Context.BIND_AUTO_CREATE);
                    case Constants.FLAG_RESOLUTION:
                        if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                            String method = "setForcedDisplaySize";
                            Class.forName("android.view.IWindowManager").getMethod(method, int.class, int.class, int.class).invoke(IWindowManager.Stub.asInterface(ServiceManager.getService("window")), Display.DEFAULT_DISPLAY, i, i1);
                            return true;
                        } else {
                            return iDchaUtilService.setForcedDisplaySize(i, i1);
                        }
                    default:
                        return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getNowDate() {
        DateFormat df = new SimpleDateFormat("MMM dd HH:mm:ss.SSS z yyyy", Locale.ENGLISH);
        return df.format(System.currentTimeMillis());
    }

    public static void LogOverWrite(Context context, Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        String message = "- ログ開始 -\n" +
                getNowDate() + ":\n" +
                "- デバイス情報:\n" +
                Build.FINGERPRINT + "\n" +
                "- 例外原因:\n" +
                throwable.getCause() + "\n" +
                "- スタックトレース:\n" +
                stringWriter +
                "- ログ終了 -\n\n";

        if (!Preferences.load(context, Constants.KEY_CRASH_LOG, "").isEmpty()) {
            Preferences.save(context, Constants.KEY_CRASH_LOG, String.join(",", Preferences.load(context, Constants.KEY_CRASH_LOG, "")).replace("    ", "") + message);
        } else {
            Preferences.save(context, Constants.KEY_CRASH_LOG, message);
        }
    }

    /* 選択したファイルデータを取得 */
    public static String getFilePath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            switch (Objects.requireNonNull(uri.getAuthority())) {
                /* 内部ストレージ */
                case "com.android.externalstorage.documents":
                    String[] str = DocumentsContract.getDocumentId(uri).split(":");
                    return Environment.getExternalStorageDirectory() + "/" + str[1];
                /* ダウンロード */
                case "com.android.providers.downloads.documents":
                    try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            return Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                        }
                    }
                default:
                    return null;
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static JSONObject parseJson(Context context) throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(context.getExternalCacheDir(), "Check.json").getPath()));
        JSONObject json;
        StringBuilder data = new StringBuilder();
        String str = bufferedReader.readLine();

        while (str != null) {
            data.append(str);
            str = bufferedReader.readLine();
        }

        json = new JSONObject(data.toString());

        bufferedReader.close();
        return json;
    }

    public static boolean isDhizukuActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isDeviceOwnerApp("com.rosan.dhizuku")) {
            if (Dhizuku.init(context)) {
                return Dhizuku.isPermissionGranted();
            }
        }
        return false;
    }

    public static boolean isCfmDialog(Context context) {
        if (Preferences.load(context, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTZ) {
            /* チャレパNEO・NEXTは対象 */
            if (!Constants.COUNT_DCHA_COMPLETED_FILE.exists() && Constants.IGNORE_DCHA_COMPLETED_FILE.exists() || !Constants.COUNT_DCHA_COMPLETED_FILE.exists() || Constants.IGNORE_DCHA_COMPLETED_FILE.exists()) {
                return Preferences.load(context, Constants.KEY_FLAG_CONFIRMATION, false);
            } else {
                return true;
            }
        } else {
            /* チャレパ２・３は対象外 */
            return true;
        }
    }

    public static long getFileSize(final File file) {
        if (file == null || !file.exists()) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        final List<File> dirs = new LinkedList<>();
        dirs.add(file);
        long result = 0;

        while (!dirs.isEmpty()) {
            final File dir = dirs.remove(0);

            if (!dir.exists()) {
                continue;
            }

            final File[] listFiles = dir.listFiles();

            if (listFiles == null) {
                continue;
            }

            for (final File child : listFiles) {
                if (child.isDirectory()) {
                    dirs.add(child);
                } else {
                    result += child.length();
                }
            }
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static double getDirectorySize(File file) {
        double fileSize = 0;

        if (file != null) {
            try {
                File[] list = file.listFiles();

                for (File value : list != null ? list : new File[0]) {
                    if (!value.isDirectory()) {
                        fileSize += Files.size(Paths.get(value.getPath()));
                    } else {
                        File[] obbName = new File(value.getPath() + "/obb").listFiles();
                        File[] obbFile = obbName != null ? obbName[0].listFiles() : new File[0];
                        fileSize += Files.size(Paths.get(obbFile != null ? obbFile[0].getPath() : null));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return fileSize;
    }

    public static boolean isRunningService(Context context, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static String getTemporaryPath(Context context) {
        if (context != null && context.getExternalCacheDir() != null) {
            return context.getExternalCacheDir().getPath() + "/tmp";
        }
        return null;
    }

    public static boolean deleteDirectory(File file) {
        if (file != null && file.isDirectory()) {
            String[] fileList = file.list();
            if (fileList != null) {
                for (String s : fileList) {
                    File f = new File(file, s);
                    if (!f.delete()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
