package com.saradabar.cpadcustomizetool.view.flagment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.installer.SplitInstaller;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Path;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.BlockerActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;

import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DeviceOwnerFragment extends PreferenceFragmentCompat {

    String[] splitInstallData = new String[256];

    double totalByte;

    Preference preferenceBlockToUninstallSettings,
            preferenceDisableDeviceOwner,
            preferenceNowSetOwnerApp,
            preferenceOwnerSilentInstall;

    SwitchPreference switchPreferencePermissionForced;

    @SuppressLint("StaticFieldLeak")
    private static DeviceOwnerFragment instance = null;

    public static DeviceOwnerFragment getInstance() {
        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_device_owner, rootKey);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        instance = this;
        preferenceBlockToUninstallSettings = findPreference("intent_block_to_uninstall_settings");
        preferenceDisableDeviceOwner = findPreference("disable_device_owner");
        switchPreferencePermissionForced = findPreference("permission_forced");
        preferenceNowSetOwnerApp = findPreference("now_set_owner_package");
        preferenceOwnerSilentInstall = findPreference("owner_silent_install");

        preferenceBlockToUninstallSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(getActivity(), BlockerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        switchPreferencePermissionForced.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                switchPreferencePermissionForced.setChecked(true);
                switchPreferencePermissionForced.setSummary("PERMISSION_POLICY_AUTO_GRANT/PERMISSION_GRANT_STATE_GRANTED" + getString(R.string.pre_owner_sum_permission_forced));
                devicePolicyManager.setPermissionPolicy(new ComponentName(getActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                    /* ユーザーアプリか確認 */
                    if (app.sourceDir.startsWith("/data/app/")) {
                        Common.setPermissionGrantState(requireActivity(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                    }
                }
            } else {
                switchPreferencePermissionForced.setChecked(false);
                switchPreferencePermissionForced.setSummary("PERMISSION_POLICY_PROMPT/PERMISSION_GRANT_STATE_DEFAULT" + getString(R.string.pre_owner_sum_permission_default));
                devicePolicyManager.setPermissionPolicy(new ComponentName(getActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                    /* ユーザーアプリか確認 */
                    if (app.sourceDir.startsWith("/data/app/")) {
                        Common.setPermissionGrantState(requireActivity(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
                    }
                }
            }
            return true;
        });

        preferenceDisableDeviceOwner.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_question_device_owner)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        devicePolicyManager.clearDeviceOwnerApp(requireActivity().getPackageName());
                        requireActivity().finish();
                        requireActivity().overridePendingTransition(0, 0);
                        startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", true));
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preferenceOwnerSilentInstall.setOnPreferenceClickListener(preference -> {
            preferenceOwnerSilentInstall.setEnabled(false);
            try {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/*"}).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), ""), Constants.REQUEST_INSTALL);
            } catch (ActivityNotFoundException ignored) {
                preferenceOwnerSilentInstall.setEnabled(true);
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        if (getNowOwnerPackage() != null) {
            preferenceNowSetOwnerApp.setSummary(getString(R.string.pre_owner_sum_message_1) + getNowOwnerPackage() + getString(R.string.pre_owner_sum_message_2));
        } else preferenceNowSetOwnerApp.setSummary(getString(R.string.pre_owner_sum_no_device_owner));

        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
                switchPreferencePermissionForced.setEnabled(false);
                switchPreferencePermissionForced.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceOwnerSilentInstall.setEnabled(false);
                preferenceOwnerSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                setPreferenceSettings();
                break;
            case 1:
                switchPreferencePermissionForced.setEnabled(false);
                switchPreferencePermissionForced.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceBlockToUninstallSettings.setEnabled(false);
                preferenceBlockToUninstallSettings.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceDisableDeviceOwner.setEnabled(false);
                preferenceDisableDeviceOwner.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceOwnerSilentInstall.setEnabled(false);
                preferenceOwnerSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
            case 2:
                setPreferenceSettings();
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setPreferenceSettings() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!devicePolicyManager.isDeviceOwnerApp(requireActivity().getPackageName())) {
            preferenceBlockToUninstallSettings.setEnabled(false);
            preferenceDisableDeviceOwner.setEnabled(false);
            switchPreferencePermissionForced.setEnabled(false);
            preferenceOwnerSilentInstall.setEnabled(false);
            preferenceBlockToUninstallSettings.setSummary(getString(R.string.pre_owner_sum_not_use_function));
            preferenceDisableDeviceOwner.setSummary(getString(R.string.pre_owner_sum_not_use_function));
            switchPreferencePermissionForced.setSummary(getString(R.string.pre_owner_sum_not_use_function));
            preferenceOwnerSilentInstall.setSummary(getString(R.string.pre_owner_sum_not_use_function));
        } else {
            if (Preferences.GET_MODEL_ID(requireActivity()) != 0) {
                switch (devicePolicyManager.getPermissionPolicy(new ComponentName(getActivity(), AdministratorReceiver.class))) {
                    case DevicePolicyManager.PERMISSION_POLICY_PROMPT:
                        switchPreferencePermissionForced.setChecked(false);
                        switchPreferencePermissionForced.setSummary("PERMISSION_POLICY_PROMPT/PERMISSION_GRANT_STATE_DEFAULT" + getString(R.string.pre_owner_sum_permission_default));
                        break;
                    case DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT:
                        switchPreferencePermissionForced.setChecked(true);
                        switchPreferencePermissionForced.setSummary("PERMISSION_POLICY_AUTO_GRANT/PERMISSION_GRANT_STATE_GRANTED" + getString(R.string.pre_owner_sum_permission_forced));
                        break;
                }
            }
        }
    }

    private String getNowOwnerPackage() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                if (devicePolicyManager.isDeviceOwnerApp(app.packageName)) {
                    return app.loadLabel(requireActivity().getPackageManager()).toString();
                }
            }
        }
        return null;
    }

    /* 再表示 */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();
        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
                switchPreferencePermissionForced.setEnabled(false);
                switchPreferencePermissionForced.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceOwnerSilentInstall.setEnabled(false);
                preferenceOwnerSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                setPreferenceSettings();
                break;
            case 1:
                switchPreferencePermissionForced.setEnabled(false);
                switchPreferencePermissionForced.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceBlockToUninstallSettings.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceDisableDeviceOwner.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preferenceBlockToUninstallSettings.setEnabled(false);
                preferenceDisableDeviceOwner.setEnabled(false);
                preferenceOwnerSilentInstall.setEnabled(false);
                preferenceOwnerSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
            case 2:
                setPreferenceSettings();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_INSTALL) {
            preferenceOwnerSilentInstall.setEnabled(true);
            if (setInstallFiles(data)) {
                String str = new File(splitInstallData[0]).getName();
                /* ファイルの拡張子 */
                switch (str.substring(str.lastIndexOf("."))) {
                    case ".apk":
                        DeviceOwnerFragment.OwnerInstallTask ownerInstallTask = new DeviceOwnerFragment.OwnerInstallTask();
                        ownerInstallTask.setListener(StartActivity.getInstance().OwnerInstallCreateListener());
                        ownerInstallTask.execute();
                        return;
                    case ".XAPK":
                    case ".xapk":
                        TryXApkTask tryXApkTask = new TryXApkTask();
                        tryXApkTask.setListener(StartActivity.getInstance().XApkListener());
                        tryXApkTask.execute();
                        return;
                }
            }
            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.dialog_error_no_file_data))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    /* インストールファイルの取得 */
    private boolean setInstallFiles(Intent data) {
        try {
            try {
                /* 一時ファイルを消去 */
                FileUtils.deleteDirectory(requireActivity().getExternalCacheDir());
            } catch (IOException ignored) {
            }
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                /* シングルApk */
                splitInstallData[0] = getInstallData(getActivity(), data.getData());
                if (splitInstallData[0] == null) return false;
                String str = new File(splitInstallData[0]).getName();
                /* ファイルの拡張子 */
                switch (str.substring(str.lastIndexOf("."))) {
                    case ".apk":
                    case ".XAPK":
                    case ".xapk":
                        break;
                    default:
                        /* 未対応またはインストールファイルでないなら終了 */
                        return false;
                }
            } else {
                /* マルチApk */
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    /* 処理 */
                    splitInstallData[i] = getInstallData(getActivity(), clipData.getItemAt(i).getUri());
                }
            }
            return splitInstallData != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /* 選択したファイルデータを取得 */
    private String getInstallData(Context context, Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String[] str = DocumentsContract.getDocumentId(uri).split(":");
                switch (uri.getAuthority()) {
                    case "com.android.externalstorage.documents":
                        return Environment.getExternalStorageDirectory() + "/" + str[1];
                    case "com.android.providers.downloads.documents":
                        return str[1];
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressLint("NewApi")
    private double getDirectorySize(File file) {
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

    /* 解凍コピータスク */
    public static class TryXApkTask extends AsyncTask<Object, Void, Object> {

        public static TryXApkTask.Listener mListener;

        public static String obbPath1;
        public static String obbPath2;

        @Override
        protected void onPreExecute() {
            mListener.onShow();
        }

        @Override
        protected Object doInBackground(Object... value) {
            String str = new File(getInstance().splitInstallData[0]).getParent() + File.separator + new File(getInstance().splitInstallData[0]).getName().replaceFirst("\\..*", ".zip");
            /* 拡張子.xapkを.zipに変更 */
            onProgressUpdate(getInstance().getString(R.string.progress_state_rename));
            new File(getInstance().splitInstallData[0]).renameTo(new File(str));
            File file = new File(Path.getTemporaryPath(getInstance().requireActivity()));
            /* zipを展開して外部ディレクトリに一時保存 */
            onProgressUpdate(getInstance().getString(R.string.progress_state_unpack));
            getInstance().totalByte = new File(str).length();
            try {
                ZipUtil.unpack(new File(str), file);
            } catch (Exception e) {
                return getInstance().getString(R.string.installer_status_no_allocatable_space) + e.getMessage();
            }
            /* 拡張子.zipを.xapkに変更 */
            onProgressUpdate(getInstance().getString(R.string.progress_state_rename));
            new File(str).renameTo(new File(new File(str).getParent() + File.separator + new File(str).getName().replaceFirst("\\..*", ".xapk")));
            File[] list = file.listFiles();
            if (list != null) {
                int c = 0;
                /* ディレクトリのなかのファイルを取得 */
                for (int i = 0; i < list.length; i++) {
                    /* obbデータを取得 */
                    if (list[i].isDirectory()) {
                        c++;
                        try {
                            /* obbデータをコピー */
                            onProgressUpdate(getInstance().getString(R.string.progress_state_copy_file));
                            File[] obbName = new File(list[i].getPath() + "/obb").listFiles();
                            File[] obbFile = obbName != null ? obbName[0].listFiles() : new File[0];
                            getInstance().totalByte = obbFile != null ? obbFile[0].length() : 0;
                            obbPath1 = obbName[0].getName();
                            obbPath2 = obbFile != null ? obbFile[0].getName() : null;
                            FileUtils.copyDirectory(new File(list[i].getPath() + "/obb/"), new File(Environment.getExternalStorageDirectory() + "/Android/obb"));
                        } catch (Exception e) {
                            return getInstance().getString(R.string.installer_status_no_allocatable_space) + e.getMessage();
                        }
                    } else {
                        onProgressUpdate(getInstance().getString(R.string.progress_state_check_file));
                        str = list[i].getName();
                        /* apkファイルならパスをインストールデータへ */
                        if (str.substring(str.lastIndexOf(".")).equalsIgnoreCase(".apk")) {
                            getInstance().splitInstallData[i - c] = list[i].getPath();
                        } else {
                            /* apkファイルでなかったときのリストの順番を修正 */
                            c++;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        private void onProgressUpdate(String str) {
            mListener.onProgressUpdate(str);
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result == null) {
                mListener.onError(getInstance().getString(R.string.installer_status_unknown_error));
                return;
            }
            if (result.equals(true)) {
                mListener.onSuccess();
                return;
            }
            if (result.equals(false)) {
                mListener.onFailure();
                return;
            }
            mListener.onError(result.toString());
        }

        public void setListener(TryXApkTask.Listener listener) {
            mListener = listener;
        }

        /* StartActivity */
        public interface Listener {
            void onShow();

            void onSuccess();

            void onFailure();

            void onError(String str);

            void onProgressUpdate(String str);
        }

        @SuppressLint("NewApi")
        public int getLoadedBytePercent() {
            double fileSize = 0;
            if (getInstance().totalByte <= 0) return 0;
            if (obbPath1 == null) {
                fileSize = getInstance().getDirectorySize(new File(Path.getTemporaryPath(getInstance().requireActivity())));
            } else {
                try {
                    fileSize = Files.size(Paths.get(Environment.getExternalStorageDirectory() + "/Android/obb/" + obbPath1 + "/" + obbPath2));
                } catch (IOException ignored) {
                }
            }
            return (int) Math.floor(100 * fileSize / getInstance().totalByte);
        }

        public int getLoadedTotalByte() {
            return (int) getInstance().totalByte / (1024 * 1024);
        }

        @SuppressLint("NewApi")
        public int getLoadedCurrentByte() {
            double fileSize = 0;
            if (getInstance().totalByte <= 0) return 0;
            if (obbPath1 == null) {
                fileSize = getInstance().getDirectorySize(new File(Path.getTemporaryPath(getInstance().requireActivity())));
            } else {
                try {
                    fileSize = Files.size(Paths.get(Environment.getExternalStorageDirectory() + "/Android/obb/" + obbPath1 + "/" + obbPath2));
                } catch (IOException ignored) {
                }
            }
            return (int) fileSize / (1024 * 1024);
        }
    }

    /* インストールタスク */
    public static class OwnerInstallTask extends AsyncTask<Object, Void, Object> {
        public static OwnerInstallTask.Listener mListener;

        @Override
        protected void onPreExecute() {
            mListener.onShow();
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        @Override
        protected Object doInBackground(Object... value) {
            SplitInstaller splitInstaller = new SplitInstaller();
            int sessionId;
            try {
                sessionId = splitInstaller.splitCreateSession(getInstance().requireActivity()).i;
                if (sessionId < 0) {
                    return false;
                }
            } catch (Exception e) {
                return e.getMessage();
            }
            /* インストールデータの長さ回数繰り返す */
            for (String str : getInstance().splitInstallData) {
                /* 配列の中身を確認 */
                if (str != null) {
                    try {
                        if (!splitInstaller.splitWriteSession(getInstance().requireActivity(), new File(str), sessionId).bl) {
                            return false;
                        }
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                } else {
                    /* つぎの配列がnullなら終了 */
                    break;
                }
            }
            try {
                return splitInstaller.splitCommitSession(getInstance().requireActivity(), sessionId, 0).bl;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result == null) {
                mListener.onError(getInstance().getString(R.string.installer_status_unknown_error));
                return;
            }
            if (result.equals(true)) {
                return;
            }
            if (result.equals(false)) {
                mListener.onFailure("");
                return;
            }
            mListener.onError(result.toString());
        }

        public void setListener(OwnerInstallTask.Listener listener) {
            mListener = listener;
        }

        /* StartActivity */
        public interface Listener {
            void onShow();

            void onSuccess();

            void onFailure(String str);

            void onError(String str);
        }
    }
}