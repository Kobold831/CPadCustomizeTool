package com.saradabar.cpadcustomizetool.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class Common {

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setPermissionGrantState(Context context, String packageName, int grantState) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        for (String permission : getRuntimePermissions(context, packageName)) {
            dpm.setPermissionGrantState(new ComponentName(context, AdministratorReceiver.class), packageName, permission, grantState);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String[] getRuntimePermissions(Context context, String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(context, packageName))).toArray(new String[0]);
    }

    public static String[] getRequiredPermissions(Context context, String packageName) {
        try {
            String[] str = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (str != null && str.length > 0) {
                return str;
            } else {
                return new String[0];
            }
        } catch (Exception ignored) {
            return new String[0];
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
                "- スタックトレース\n" +
                stringWriter +
                "- ログ終了 -\n\n";

        if (!Preferences.load(context, Constants.KEY_CRASH_LOG, "").equals("")) {
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

        while(str != null){
            data.append(str);
            str = bufferedReader.readLine();
        }

        json = new JSONObject(data.toString());

        bufferedReader.close();
        return json;
    }
}