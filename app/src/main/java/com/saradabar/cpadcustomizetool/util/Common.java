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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;

import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Common {

    public static String getNowDate() {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd (E) HH:mm:ss", Locale.US);
        return df.format(System.currentTimeMillis());
    }

    public static void LogOverWrite(Context context, @NonNull Throwable throwable) {
        String message =
                "Date: " + getNowDate() + System.lineSeparator() +
                "Version: v" + BuildConfig.VERSION_NAME + System.lineSeparator() +
                "Device: " + Build.MODEL + System.lineSeparator() +
                "Build: " + Build.ID + System.lineSeparator() +
                "Exception: " + throwable.getMessage();

        ArrayList<String> arrayList = Preferences.load(context, Constants.KEY_LIST_CRASH_LOG);

        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(message);
        Preferences.save(context, Constants.KEY_LIST_CRASH_LOG, arrayList);
    }

    /* 選択したファイルデータを取得 */
    @Nullable
    public static String getFilePath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            switch (Objects.requireNonNull(uri.getAuthority())) {
                /* 内部ストレージ */
                case "com.android.externalstorage.documents":
                    String[] split = DocumentsContract.getDocumentId(uri).split(":");
                    if (Objects.equals(split[0], "primary")) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        return "/storage/" + split[0] + "/" + split[1];
                    }
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

    @NonNull
    public static JSONObject parseJson(@NonNull File json) throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(json.getPath()));
        JSONObject jsonObject;
        StringBuilder data = new StringBuilder();
        String str = bufferedReader.readLine();

        while (str != null) {
            data.append(str);
            str = bufferedReader.readLine();
        }

        jsonObject = new JSONObject(data.toString());

        bufferedReader.close();
        return jsonObject;
    }

    public static boolean isDhizukuActive(@NonNull Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isDeviceOwnerApp(DhizukuVariables.OFFICIAL_PACKAGE_NAME)) {
            if (Dhizuku.init(context)) {
                return Dhizuku.isPermissionGranted();
            }
        }
        return false;
    }

    private static String getProductModelName() {
        return Build.MODEL;
    }

    public static boolean isCT2() {
        return Constants.PRODUCT_CT2.contains(getProductModelName());
    }

    public static boolean isCT3() {
        return getProductModelName().equals(Constants.PRODUCT_CT3);
    }
    
    public static boolean isCTX() {
        return getProductModelName().equals(Constants.PRODUCT_CTX);
    }
    
    public static boolean isCTZ() {
        return getProductModelName().equals(Constants.PRODUCT_CTZ);
    }
    
    public static boolean isBenesseExtensionExist() {
        try {
            int status = BenesseExtension.getDchaState();
            return true;
        } catch (NoClassDefFoundError ignored) {
            return false;
        }
    }
    
    public static boolean getDchaCompletedPast() {
        // BenesseExtension が存在しない場合は配慮不要
        if (!isBenesseExtensionExist()) return true;
        // IGNORE_DCHA_COMPLETED が存在している場合は COUNT_DCHA_COMPLETED を作成しても何ら問題ない
        return BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    public static boolean isCfmDialog(Context context) {
        if (!getDchaCompletedPast()) {
            return Preferences.load(context, Constants.KEY_FLAG_DCHA_FUNCTION_CONFIRMATION, false);
        } else {
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
            //noinspection SequencedCollectionMethodCanBeUsed
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

    @NonNull
    public static String getRandomString() {
        String theAlphaNumericS;
        StringBuilder builder;
        theAlphaNumericS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        builder = new StringBuilder(5);

        for (int m = 0; m < 5; m++) {
            int myindex = (int) (theAlphaNumericS.length() * Math.random());
            builder.append(theAlphaNumericS.charAt(myindex));
        }
        return builder.toString();
    }

    public static void debugLog(String msg) {
        if (BuildConfig.DEBUG) Log.e("DEBUG", msg);
    }
}
