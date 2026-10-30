package com.saradabar.cpadcustomizetool.view.flagment;

import android.content.Context;
import android.content.Intent;
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
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
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

public class DchaFunctionFragment extends PreferenceFragmentCompat {

    AlertDialog progressDialog;

    Preference preLauncher,
            preReboot,
            preRebootShortcut,
            preSilentInstall,
            preResolution,
            preResetResolution;

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
                    new DchaInstallTask().execute(requireActivity(), Common.dchaInstallTaskListener(installTaskUi(), null), installData);
                    return;
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

        preLauncher.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launcher_list, null);
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
                    new DchaServiceUtil(requireActivity()).setPreferredHomeApp(Common.getLauncherPackage(requireActivity()), Uri.fromParts("package",
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
                Common.setName(requireActivity(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, Common.getLauncherPackage(requireActivity()));
            }
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, (boolean) o);
            Common.startKeepService(requireActivity());
            return true;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                            new DchaServiceUtil(requireActivity()).rebootPad(object -> {}))
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.reboot))
                        .setShortLabel(getString(R.string.reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), R.drawable.autorenew_24px))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), R.drawable.autorenew_24px))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preSilentInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSilentInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType(Constants.MIME_APK)
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
            boolean isWriteSecureSettingsGranted = Common.isWriteSecureSettingsGranted(requireActivity());

            if ((Common.isCT2() || Common.isCT3()) && !isWriteSecureSettingsGranted) {
                new DialogUtil(requireActivity())
                        .setMessage(R.string.dialog_error_need_write_secure_settings)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
                return false;
            }
            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);

            if ((Common.isCTX() || Common.isCTZ()) && !isWriteSecureSettingsGranted) {
                LinearLayout linearLayoutAny = view.findViewById(R.id.v_resolution_linearlayout_any);
                LinearLayout linearLayoutBenesse = view.findViewById(R.id.v_resolution_linearlayout_benesse);
                linearLayoutAny.setVisibility(View.GONE);
                linearLayoutBenesse.setVisibility(View.VISIBLE);
                new DialogUtil(requireActivity())
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            AppCompatRadioButton xga = view.findViewById(R.id.xga);
                            AppCompatRadioButton wxga = view.findViewById(R.id.wxga);
                            AppCompatRadioButton wuxga = view.findViewById(R.id.wuxga);

                            int width, height;

                            if (xga.isChecked()) {
                                width = Constants.XGA_WIDTH;
                                height = Constants.XGA_HEIGHT;
                            } else if (wxga.isChecked()) {
                                width = Constants.WXGA_WIDTH;
                                height = Constants.WXGA_HEIGHT;
                            } else if (wuxga.isChecked()) {
                                width = Constants.WUXGA_WIDTH;
                                height = Constants.WUXGA_HEIGHT;
                            } else {
                                Toast.makeText(requireActivity(), R.string.toast_not_selected, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new ResolutionTask().execute(((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
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
                        AppCompatEditText editTextWidth = view.findViewById(R.id.width);
                        AppCompatEditText editTextHeight = view.findViewById(R.id.height);

                        try {
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            int height = Integer.parseInt(editTextHeight.getText().toString());

                            if (width <= 0 || height <= 0) {
                                new DialogUtil(requireActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            } else {
                                new ResolutionTask().execute(((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
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
            Common.resetResolution(requireActivity());
            return false;
        });

        initPreference();
    }

    private void initPreference() {
        swKeepLauncher.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, Constants.DEF_BOOL));
        preLauncher.setSummary(Common.getLauncherName(requireActivity()));

        /* 端末ごとにPreferenceの状態を設定 */
        if (Common.isCT2()) {
            if (Common.getVersionCode(requireActivity(), Constants.PKG_DCHA_SERVICE) < Constants.MIN_VERSION_CODE_DCHA_SERVICE) {
                preSilentInstall.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
                preSilentInstall.setEnabled(false);
            }
        }
    }

    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(requireActivity(), message);
    }

    private void cancelLoadingDialog() {
        Common.cancelDialog(progressDialog);
    }

    @NonNull
    private Common.TaskUi installTaskUi() {
        return new Common.TaskUi() {
            @Override
            public Context context() {
                return isAdded() ? requireActivity() : null;
            }

            @Override
            public void showLoading() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            @Override
            public void cancelLoading() {
                cancelLoadingDialog();
            }
        };
    }
}
