package com.saradabar.cpadcustomizetool.view.flagment;

import static com.saradabar.cpadcustomizetool.util.Common.isDchaUtilActive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.ResolutionTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.RebootActivity;
import com.saradabar.cpadcustomizetool.view.views.HomeAppListView;

import java.util.ArrayList;
import java.util.List;

/** @noinspection deprecation*/
public class DchaFunctionFragment extends PreferenceFragmentCompat {

    AlertDialog progressDialog;

    Preference preLauncher,
            preReboot,
            preRebootShortcut,
            preSilentInstall,
            preResolution,
            preResetResolution,
            preSystemUpdate;

    SwitchPreferenceCompat swKeepLauncher;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ACTIVITY_INSTALL:// サイレントインストール要求
                preSilentInstall.setEnabled(true);

                if (data == null) {
                    // 何も選択していない
                    return;
                }
                String installData = Common.getFilePath(requireActivity(), data.getData());

                if (installData != null) {
                    //　選択されたファイルの取得成功
                    new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), installData);
                    return;
                } else {
                    //　選択されたファイルの取得失敗
                    new DialogUtil(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
                break;
            case Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE:// システムアップデート要求
                preSystemUpdate.setEnabled(true);

                if (data == null) {
                    // 何も選択していない
                    return;
                }
                String updateData = Common.getFilePath(requireActivity(), data.getData());

                if (updateData != null) {
                    //　選択されたファイルの取得成功
                    new DchaServiceUtil(requireActivity()).execSystemUpdate(updateData, 0, object -> {
                        if (!object.equals(true)) {
                            new DialogUtil(requireActivity())
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    });
                } else {
                    //　選択されたファイルの取得失敗
                    new DialogUtil(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
                break;
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pre_dcha_function, rootKey);

        preLauncher = findPreference("pre_launcher");
        swKeepLauncher = findPreference("pre_keep_launcher");
        preReboot = findPreference("pre_reboot");
        preRebootShortcut = findPreference("pre_reboot_shortcut");
        preSilentInstall = findPreference("pre_silent_install");
        preResolution = findPreference("pre_resolution");
        preResetResolution = findPreference("pre_reset_resolution");
        preSystemUpdate = findPreference("pre_system_update");

        preLauncher.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<HomeAppListView.AppData> dataList = new ArrayList<>();

            for (ResolveInfo resolveInfo : installedAppList) {
                HomeAppListView.AppData data = new HomeAppListView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }
            ListView listView = view.findViewById(R.id.launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new HomeAppListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) ->
                    new DchaServiceUtil(requireActivity()).setPreferredHomeApp(getLauncherPackage(requireActivity()), Uri.fromParts("package",
                            installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""), object -> {
                        listView.invalidateViews();
                        initPreference();
                    })
            );
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        swKeepLauncher.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                Preferences.save(requireActivity(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, getLauncherPackage(requireActivity()));
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                            new DchaServiceUtil(requireActivity()).rebootPad(0, "", object -> {
                            }))
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.reboot))
                        .setShortLabel(getString(R.string.reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_popup_sync))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_popup_sync))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preSilentInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSilentInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/vnd.android.package-archive")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (RuntimeException ignored) {
                preSilentInstall.setEnabled(true);
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preResolution.setOnPreferenceClickListener(preference -> {
            if (Common.isCT2() || Common.isCT3()) {
                // CT2またはCT3
                if (!isDchaUtilActive(requireActivity())) {
                    // DchaUtilService が機能していない
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                // sdk23以上かつWRITE_SECURE_SETTINGSが付与されていない
                LinearLayout linearLayoutAny = view.findViewById(R.id.v_resolution_linearlayout_any);
                LinearLayout linearLayoutBenesse = view.findViewById(R.id.v_resolution_linearlayout_benesse);
                linearLayoutAny.setVisibility(View.GONE);
                linearLayoutBenesse.setVisibility(View.VISIBLE);
                new DialogUtil(requireActivity())
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            AppCompatRadioButton rb1024 = view.findViewById(R.id.v_resolution_radio_1024);
                            AppCompatRadioButton rb1280 = view.findViewById(R.id.v_resolution_radio_1280);
                            AppCompatRadioButton rb1920 = view.findViewById(R.id.v_resolution_radio_1920);

                            int width, height;

                            if (rb1024.isChecked()) {
                                width = 1024;
                                height = 768;
                            } else if (rb1280.isChecked()) {
                                width = 1280;
                                height = 800;
                            } else if (rb1920.isChecked()) {
                                width = 1920;
                                height = 1200;
                            } else {
                                Toast.makeText(requireActivity(), R.string.toast_not_selected, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new ResolutionTask().execute(requireActivity(), ((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, null)
                        .show();
                return false;
            }
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_resolution)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                        AppCompatEditText editTextWidth = view.findViewById(R.id.edit_text_1);
                        AppCompatEditText editTextHeight = view.findViewById(R.id.edit_text_2);

                        try {
                            //noinspection DataFlowIssue
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            //noinspection DataFlowIssue
                            int height = Integer.parseInt(editTextHeight.getText().toString());

                            if (width < 0 || height < 0) {
                                new DialogUtil(requireActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            } else {
                                new ResolutionTask().execute(requireActivity(), ((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
                            }
                        } catch (NumberFormatException ignored) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_illegal_value)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preResetResolution.setOnPreferenceClickListener(preference -> {
            if (Common.isCT2() || Common.isCT3()) {
                // CT2またはCT3
                if (!isDchaUtilActive(requireActivity())) {
                    // DchaUtilService が機能していない
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }
            ((MainActivity) requireActivity()).resetResolution();
            return false;
        });

        preSystemUpdate.setOnPreferenceClickListener(preference -> {
            try {
                preSystemUpdate.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/zip")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE);
            } catch (RuntimeException ignored) {
                preSystemUpdate.setEnabled(true);
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });
        initPreference();
    }

    private void initPreference() {
        swKeepLauncher.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, false));
        preLauncher.setSummary(getLauncherName(requireActivity()));

        /* 端末ごとにPreferenceの状態を設定 */
        if (Common.isCT2()) {
            try {
                if (requireActivity().getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, 0).versionCode < 5) {
                    preSilentInstall.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
                    preSilentInstall.setEnabled(false);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                preSilentInstall.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
                preSilentInstall.setEnabled(false);
            }
        }
    }

    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    private void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    /* ランチャーのパッケージ名を取得 */
    @Nullable
    private String getLauncherPackage(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    /* ランチャーのアプリ名を取得 */
    @Nullable
    private String getLauncherName(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.loadLabel(context.getPackageManager()).toString();
        }
        return null;
    }

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return new DchaInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(requireActivity().getResources().getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                cancelLoadingDialog();
                new DialogUtil(DchaFunctionFragment.this.requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new DialogUtil(DchaFunctionFragment.this.requireActivity())
                        .setMessage(R.string.dialog_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }
}
