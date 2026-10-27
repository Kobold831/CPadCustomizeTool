/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.util;

import android.Manifest;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.rosan.dhizuku.api.Dhizuku;

import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import com.rosan.dhizuku.shared.DhizukuVariables;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class Common {

    private static volatile Field sDevicePolicyManagerServiceField;

    public static boolean isWriteSecureSettingsGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    public static DevicePolicyManager getDevicePolicyManager(Context context) {
        if (isDhizukuAllActive(context)) {
            return binderWrapperDevicePolicyManager(context);
        } else {
            return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
    }

    public static ComponentName getDeviceAdminComponent(Context context) {
        if (isDhizukuAllActive(context)) {
            return DhizukuVariables.COMPONENT_NAME;
        } else {
            return new ComponentName(context, DeviceAdminReceiver.class);
        }
    }

    @NonNull
    public static DevicePolicyManager binderWrapperDevicePolicyManager(@NonNull Context c) {
        try {
            Context context = c.createPackageContext(DhizukuVariables.OFFICIAL_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Field field = sDevicePolicyManagerServiceField;

            if (field == null) {
                field = manager.getClass().getDeclaredField("mService");
                field.setAccessible(true);
                sDevicePolicyManagerServiceField = field;
            }
            IDevicePolicyManager oldInterface = (IDevicePolicyManager) field.get(manager);
            if (oldInterface instanceof DhizukuBinderWrapper) return manager;
            assert oldInterface != null;
            IBinder oldBinder = oldInterface.asBinder();
            IBinder newBinder = Dhizuku.binderWrapper(oldBinder);
            IDevicePolicyManager newInterface = IDevicePolicyManager.Stub.asInterface(newBinder);
            field.set(manager, newInterface);
            return manager;
        } catch (NoSuchFieldException | IllegalAccessException |
                 PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isDchaActive(@NonNull Context context) {
        return DchaServiceConnection.ensureBound(context);
    }

    @NonNull
    public static ArrayList<String> exec(String str) {
        ArrayList<String> stringArrayList = new ArrayList<>();
        Process process = null;

        try {
            ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", "-c", str);
            builder.redirectErrorStream(true);
            process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String data;

                while ((data = reader.readLine()) != null) {
                    stringArrayList.add(data);
                }
            }

            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return stringArrayList;
    }

    public static String getNowDate() {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd (E) HH:mm:ss", Locale.JAPAN);
        return df.format(System.currentTimeMillis());
    }

    public static void LogOverWrite(Context context, @NonNull Throwable throwable) {
        StackTraceElement crashElement = getCrashElement(context, throwable);
        String crashClass = throwable.getClass().getName();
        String crashMethod = "";

        if (crashElement != null) {
            crashClass = crashElement.getClassName();
            crashMethod = crashElement.getMethodName();
        }

        String message =
                "Date: " + getNowDate() + System.lineSeparator() +
                "Version: v" + BuildConfig.VERSION_NAME + System.lineSeparator() +
                "Device: " + Build.MODEL + System.lineSeparator() +
                "Build: " + Build.ID + System.lineSeparator() +
                "Class: " + crashClass + System.lineSeparator() +
                "Method: " + crashMethod + System.lineSeparator() +
                "Exception: " + throwable.getClass().getName() + ": " + throwable.getMessage();

        ArrayList<String> arrayList = Preferences.load(context, Constants.KEY_LIST_CRASH_LOG);

        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(message);
        Preferences.save(context, Constants.KEY_LIST_CRASH_LOG, arrayList);
    }

    @Nullable
    private static StackTraceElement getCrashElement(@NonNull Context context, @NonNull Throwable throwable) {
        String packageName = context.getPackageName() + ".";
        StackTraceElement firstElement = null;
        Throwable cause = throwable;

        for (int depth = 0; cause != null && depth < Constants.MAX_CAUSE_DEPTH; depth++) {
            StackTraceElement[] stackTraceElements = cause.getStackTrace();

            if (stackTraceElements.length > 0 && firstElement == null) {
                firstElement = stackTraceElements[0];
            }

            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (stackTraceElement.getClassName().startsWith(packageName)) {
                    return stackTraceElement;
                }
            }

            Throwable next = cause.getCause();

            if (next == cause) {
                break;
            }
            cause = next;
        }
        return firstElement;
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
        StringBuilder data = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(json), StandardCharsets.UTF_8))) {
            String str;

            while ((str = bufferedReader.readLine()) != null) {
                data.append(str);
            }
        }

        return new JSONObject(data.toString());
    }

    public static boolean isUserApp(@NonNull ApplicationInfo app) {
        return app.sourceDir.startsWith("/data/app/");
    }

    public static boolean isDhizukuActive(@NonNull Context context) {
        try {
            return Dhizuku.init(context);
        } catch (AssertionError ignored) {
        }
        return false;
    }

    public static boolean isDhizukuAllActive(Context context) {
        try {
            if (isDhizukuActive(context)) {
                return Dhizuku.isPermissionGranted();
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    public static int getFlag(String keyName) {
        return Preferences.load(MyApplication.getContext(), keyName, Constants.DEF_INT);
    }

    public static int getFlag(Context context, String keyName) {
        return Preferences.load(context, keyName, Constants.DEF_INT);
    }

    public static int getFlag(String keyName, int def) {
        return Preferences.load(MyApplication.getContext(), keyName, def);
    }

    public static int getFlag(Context context, String keyName, int def) {
        return Preferences.load(context, keyName, def);
    }

    public static void setFlag(String keyName, int flag) {
        Preferences.save(MyApplication.getContext(), keyName, flag);
    }

    public static void setFlag(Context context, String keyName, int flag) {
        Preferences.save(context, keyName, flag);
    }

    public static boolean isFlagEnabled(String keyName) {
        return Preferences.load(MyApplication.getContext(), keyName, Constants.DEF_BOOL);
    }

    public static boolean isFlagEnabled(Context context, String keyName) {
        return Preferences.load(context, keyName, Constants.DEF_BOOL);
    }

    public static boolean isFlagEnabled(String keyName, boolean def) {
        return Preferences.load(MyApplication.getContext(), keyName, def);
    }

    public static boolean isFlagEnabled(Context context, String keyName, boolean def) {
        return Preferences.load(context, keyName, def);
    }

    public static void setFlagEnabled(String keyName, boolean enabled) {
        Preferences.save(MyApplication.getContext(), keyName, enabled);
    }

    public static void setFlagEnabled(Context context, String keyName, boolean enabled) {
        Preferences.save(context, keyName, enabled);
    }

    public static String getName(String keyName) {
        return Preferences.load(MyApplication.getContext(), keyName, Constants.DEF_STR);
    }

    public static String getName(Context context, String keyName) {
        return Preferences.load(context, keyName, Constants.DEF_STR);
    }

    public static String getName(String keyName, String def) {
        return Preferences.load(MyApplication.getContext(), keyName, def);
    }

    public static String getName(Context context, String keyName, String def) {
        return Preferences.load(context, keyName, def);
    }

    public static void setName(String keyName, String val) {
        Preferences.save(MyApplication.getContext(), keyName, val);
    }

    public static void setName(Context context, String keyName, String val) {
        Preferences.save(context, keyName, val);
    }

    public static String getPref(Context context, String keyName) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(keyName, "0");
    }

    private static boolean isDebugDevice() {
        return getDebugDevice() != 0 && BuildConfig.DEBUG;
    }

    private static int getDebugDevice() {
        return getFlag(Constants.KEY_INT_DEBUG_DEVICE);
    }

    public static boolean isCT2() {
        return isDebugDevice()
                ? getDebugDevice() == Constants.DEBUG_DEVICE_CT2
                : Constants.PRODUCT_CT2.contains(Build.MODEL);
    }

    public static boolean isCT3() {
        return isDebugDevice()
                ? getDebugDevice() == Constants.DEBUG_DEVICE_CT3
                : Build.MODEL.equals(Constants.PRODUCT_CT3);
    }

    public static boolean isCTX() {
        return isDebugDevice()
                ? getDebugDevice() == Constants.DEBUG_DEVICE_CTX
                : Build.MODEL.equals(Constants.PRODUCT_CTX);
    }

    public static boolean isCTZ() {
        return isDebugDevice()
                ? getDebugDevice() == Constants.DEBUG_DEVICE_CTZ
                : Build.MODEL.equals(Constants.PRODUCT_CTZ);
    }

    public static boolean isBenesseExtensionExist(String method) {
        try {
            Class<?> c = Class.forName("android.os.BenesseExtension", false, ClassLoader.getSystemClassLoader());
            for (Method m : c.getMethods()) {
                if (m.getName().equals(method)) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException |
                 NoClassDefFoundError |
                 SecurityException |
                 NullPointerException ignored) {
            return false;
        }
    }

    public static boolean isDchaLockExists() {
        return isBenesseExtensionExist("checkPassword");
    }

    public static boolean getDchaCompletedPast() {
        if (!isDchaLockExists()) {
            // BenesseExtension でのパスワード検証が存在しない
            return true;
        }
        return BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
    }

    public static boolean isShowCfmDialog(Context context) {
        if (getDchaCompletedPast()) {
            // すでに保護が働いている場合は確認ダイアログを表示しない
            return false;
        }
        // すでにダイアログで確認している場合は、確認ダイアログを表示しない
        return !isFlagEnabled(context, Constants.KEY_FLAG_DCHA_FUNCTION_CONFIRMATION);
    }

    public static long getFileSize(final File file) {
        if (file == null || !file.exists()) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        final ArrayDeque<File> dirs = new ArrayDeque<>();
        dirs.add(file);
        long result = 0;

        while (!dirs.isEmpty()) {
            final File dir = dirs.poll();

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

    public static void deleteDirectoryAsync(@Nullable File directory) {
        if (directory == null) {
            return;
        }
        new Thread(() -> deleteDirectory(directory)).start();
    }

    public static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return false;
        }
        File[] fs = directory.listFiles();

        if (fs == null) {
            return false;
        }

        try {
            for (File file : fs) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
            return directory.delete();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @NonNull
    public static String getRandomString() {
        final String theAlphaNumericS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder builder = new StringBuilder(Constants.RANDOM_STRING_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int m = 0; m < Constants.RANDOM_STRING_LENGTH; m++) {
            builder.append(theAlphaNumericS.charAt(random.nextInt(theAlphaNumericS.length())));
        }
        return builder.toString();
    }

    public static boolean extractTempApk(@NonNull Context context) {
        File cacheDir = context.getExternalCacheDir();

        if (cacheDir == null) {
            return false;
        }

        try (InputStream inputStream = context.getAssets().open("base.apk");
             FileOutputStream fileOutputStream = new FileOutputStream(new File(cacheDir, "base.apk"), false)) {
            byte[] buffer = new byte[Constants.BUFFER_SIZE_SMALL];
            int length;

            while ((length = inputStream.read(buffer)) >= 0) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    public interface BypassPermissionListener {
        void onResult(boolean success);
    }

    public static void acquireBypassPermission(@NonNull Context context, @NonNull BypassPermissionListener listener) {
        if (!extractTempApk(context)) {
            listener.onResult(false);
            return;
        }

        new DchaServiceUtil(context).setSetupStatus(Constants.DIGICHALIZE_STATUS_DIGICHALIZED, object -> {
            if (!object.equals(true)) {
                listener.onResult(false);
                return;
            }

            new DchaServiceUtil(context).installApp(context.getExternalCacheDir() + "/" + "base.apk", object1 -> {
                if (!object1.equals(true)) {
                    listener.onResult(false);
                    return;
                }

                new DchaServiceUtil(context).uninstallApp("a.a", object2 -> {
                    if (!object2.equals(true)) {
                        listener.onResult(false);
                        return;
                    }

                    new DchaServiceUtil(context).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object3 ->
                            listener.onResult(object3.equals(true)));
                });
            });
        });
    }

    public static boolean copyFile(File srcFile, File dstFile) {
        try (InputStream in = new FileInputStream(srcFile)) {
            try (OutputStream out = new FileOutputStream(dstFile)) {
                byte[] buf = new byte[Constants.BUFFER_SIZE_SMALL];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                return true;
            }
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean copyDirectory(@NonNull File srcDir, File dstDir) {
        if (!srcDir.isDirectory()) {
            return false;
        }

        if (!dstDir.isDirectory() && !dstDir.mkdirs()) {
            return false;
        }

        File[] files = srcDir.listFiles();

        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (!copyDirectory(file, new File(dstDir, file.getName()))) {
                    return false;
                }
            } else {
                if (!copyFile(file, new File(dstDir, file.getName()))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setNormalEnv(Context context) {
        if (isFlagEnabled(context, Constants.KEY_FLAG_NORMAL_ENV)) {
            // 通常環境モード有効 //
            // サービスの維持機能を停止フラグに変更
            setFlagEnabled(context, Constants.KEY_FLAG_KEEP_DCHA_STATE, false);
            setFlagEnabled(context, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false);
            setFlagEnabled(context, Constants.KEY_FLAG_KEEP_MARKET_APP, false);
            setFlagEnabled(context, Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
            setFlagEnabled(context, Constants.KEY_FLAG_KEEP_HOME, false);
            // サービスを起動(自動停止)
            startKeepService(context);

            if (getFlag(context, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE) == Constants.UPDATE_MODE_DCHA) {
                // Dcha に設定されている
                // インストールモードをリセット
                setFlag(context, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
            }
            // Dcha を使用しない設定に変更
            setFlagEnabled(context, Constants.KEY_FLAG_DCHA_FUNCTION, false);
            setFlagEnabled(context, Constants.KEY_FLAG_APP_SETTING_DCHA, false);
        }
    }

    public static boolean isAnyKeepFlagEnabled(@NonNull Context context) {
        return isFlagEnabled(context, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR) ||
                isFlagEnabled(context, Constants.KEY_FLAG_KEEP_DCHA_STATE) ||
                isFlagEnabled(context, Constants.KEY_FLAG_KEEP_MARKET_APP) ||
                isFlagEnabled(context, Constants.KEY_FLAG_KEEP_USB_DEBUG) ||
                isFlagEnabled(context, Constants.KEY_FLAG_KEEP_HOME);
    }

    public static void startKeepService(@NonNull Context context) {
        context.startService(new Intent(context, KeepService.class));
        context.startService(new Intent(context, ProtectKeepService.class));
    }

    @NonNull
    public static List<ApplicationInfo> getUserApplications(@NonNull Context context) {
        List<ApplicationInfo> userApps = new ArrayList<>();

        for (ApplicationInfo app : context.getPackageManager().getInstalledApplications(0)) {
            if (isUserApp(app)) {
                userApps.add(app);
            }
        }
        return userApps;
    }

    @NonNull
    public static String[] getRequiredPermissions(@NonNull Context context, @NonNull String packageName) {
        try {
            String[] permissions = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;

            if (permissions != null && permissions.length > 0) {
                return permissions;
            }
        } catch (Exception ignored) {
        }
        return new String[0];
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static int setPermissionGrantState(@NonNull Context context, @NonNull String packageName, int grantState) {
        return setPermissionGrantState(getDevicePolicyManager(context), getDeviceAdminComponent(context), context, packageName, grantState);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static int setPermissionGrantState(@NonNull DevicePolicyManager dpm, @NonNull ComponentName adminComponent,
                                              @NonNull Context context, @NonNull String packageName, int grantState) {
        String[] permissions = getRequiredPermissions(context, packageName);

        for (String permission : permissions) {
            dpm.setPermissionGrantState(adminComponent, packageName, permission, grantState);
        }
        return permissions.length;
    }

    public static int getVersionCode(@NonNull Context context, @NonNull String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return -1;
        }
    }

    public static boolean isVersionBelow(@NonNull Context context, @NonNull String packageName, int versionCode) {
        int currentVersionCode = getVersionCode(context, packageName);
        return currentVersionCode >= 0 && currentVersionCode < versionCode;
    }

    @NonNull
    public static JSONObject getUpdateInfo(@NonNull File json) throws JSONException, IOException {
        return parseJson(json).getJSONObject("update");
    }

    @NonNull
    public static JSONArray getNotices(@NonNull File json) throws JSONException, IOException {
        JSONArray source = parseJson(json).getJSONArray("notice");
        JSONArray notices = new JSONArray();

        for (int i = 0; i < source.length(); i++) {
            JSONObject notice = source.getJSONObject(i);

            if (notice.optString("title").isEmpty()
                    && notice.optString("message").isEmpty()
                    && notice.optString("url").isEmpty()) {
                continue;
            }
            notices.put(notice);
        }
        return notices;
    }

    public static void setDisplaySizeWithDcha(@NonNull Context context, int width, int height) {
        try {
            BenesseExtension.setForcedDisplaySize(width, height);
        } catch (SecurityException ignored) {
            Toast.makeText(context, context.getString(R.string.dialog_error_resolution_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void resetResolution(@NonNull Context context) {
        if (isCTX() || isCTZ()) {
            // CTXとCTZ
            BenesseExtension.putInt(Constants.BC_COMPATSCREEN, Constants.WUXGA);
        } else {
            // CT2とCT3
            setDisplaySizeWithDcha(context, Constants.WXGA_WIDTH, Constants.WXGA_HEIGHT);
        }
    }

    public static void setDchaState(@NonNull Context context, int status, @NonNull Runnable onFailure) {
        new DchaServiceUtil(context).setSetupStatus(status, object -> {
            if (!Boolean.TRUE.equals(object)) {
                onFailure.run();
            }
        });
    }

    public static void setNavigationBar(@NonNull Context context, boolean hidden, @NonNull Runnable onFailure) {
        new DchaServiceUtil(context).hideNavigationBar(hidden, object -> {
            if (!Boolean.TRUE.equals(object)) {
                onFailure.run();
            }
        });
    }

    @Nullable
    public static ResolveInfo resolveHomeActivity(@NonNull Context context) {
        return context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
    }

    @Nullable
    public static String getLauncherPackage(@NonNull Context context) {
        ResolveInfo resolveInfo = resolveHomeActivity(context);
        return resolveInfo != null ? resolveInfo.activityInfo.packageName : null;
    }

    @Nullable
    public static String getLauncherName(@NonNull Context context) {
        ResolveInfo resolveInfo = resolveHomeActivity(context);
        return resolveInfo != null ? resolveInfo.loadLabel(context.getPackageManager()).toString() : null;
    }

    @NonNull
    public static AlertDialog showProgressSpinner(@NonNull Context context, String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        AlertDialog progressDialog = new DialogUtil(context).setCancelable(false).setView(view).create();
        progressDialog.show();
        return progressDialog;
    }

    public static void cancelDialog(@Nullable Dialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        try {
            dialog.cancel();
        } catch (RuntimeException ignored) {
        }
    }

    public interface TaskUi {
        @Nullable
        Context context();

        void showLoading();

        void cancelLoading();
    }

    @NonNull
    public static ApkInstallTask.Listener apkInstallTaskListener(@NonNull TaskUi taskUi, @Nullable Runnable onResult) {
        return new ApkInstallTask.Listener() {

            @Override
            public void onShow() {
                if (taskUi.context() == null) {
                    return;
                }
                taskUi.showLoading();
            }

            @Override
            public void onSuccess() {
                Context context = taskUi.context();

                if (context == null) {
                    return;
                }
                deleteDirectoryAsync(context.getExternalCacheDir());
                taskUi.cancelLoading();
                new DialogUtil(context)
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            if (onResult != null) {
                                onResult.run();
                            }
                        })
                        .show();
            }

            @Override
            public void onFailure(String message) {
                Context context = taskUi.context();

                if (context == null) {
                    return;
                }
                deleteDirectoryAsync(context.getExternalCacheDir());
                taskUi.cancelLoading();
                new DialogUtil(context)
                        .setMessage(context.getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            if (onResult != null) {
                                onResult.run();
                            }
                        })
                        .show();
            }

            @Override
            public void onError(String message) {
                Context context = taskUi.context();

                if (context == null) {
                    return;
                }

                deleteDirectoryAsync(context.getExternalCacheDir());
                taskUi.cancelLoading();
                new DialogUtil(context)
                        .setTitle(context.getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            if (onResult != null) {
                                onResult.run();
                            }
                        })
                        .show();
            }
        };
    }

    @NonNull
    public static DchaInstallTask.Listener dchaInstallTaskListener(@NonNull TaskUi taskUi, @Nullable Runnable onResult) {
        return new DchaInstallTask.Listener() {

            @Override
            public void onShow() {
                if (taskUi.context() == null) {
                    return;
                }
                taskUi.showLoading();
            }

            @Override
            public void onSuccess() {
                if (taskUi.context() == null) {
                    return;
                }
                taskUi.cancelLoading();
                new DialogUtil(taskUi.context())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            if (onResult != null) {
                                onResult.run();
                            }
                        })
                        .show();
            }

            @Override
            public void onFailure() {
                if (taskUi.context() == null) {
                    return;
                }
                taskUi.cancelLoading();
                new DialogUtil(taskUi.context())
                        .setMessage(R.string.dialog_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            if (onResult != null) {
                                onResult.run();
                            }
                        })
                        .show();
            }
        };
    }
}
