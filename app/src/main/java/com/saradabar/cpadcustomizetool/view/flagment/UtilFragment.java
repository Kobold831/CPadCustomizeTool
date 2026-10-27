package com.saradabar.cpadcustomizetool.view.flagment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.DeviceInfoActivity;
import com.saradabar.cpadcustomizetool.view.activity.WebViewActivity;
import com.saradabar.cpadcustomizetool.view.views.LaunchAppListView;

import java.util.ArrayList;
import java.util.List;

public class UtilFragment extends PreferenceFragmentCompat {

    Preference preOtherStartSettings,
            preStartUiAdjustment,
            preStartDevSettings,
            preWebView,
            preLaunchApp,
            preDeviceInfo,
            preReEnableSystemApps;

    AlertDialog progressDialog;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pre_util, rootKey);

        preOtherStartSettings = findPreference("pre_other_start_settings");
        preStartUiAdjustment = findPreference("pre_other_start_ui_adjustment");
        preStartDevSettings = findPreference("pre_other_start_dev_settings");
        preWebView = findPreference("pre_other_web_view");
        preLaunchApp = findPreference("pre_other_launch_app");
        preDeviceInfo = findPreference("pre_other_start_device_info");
        preReEnableSystemApps = findPreference("pre_util_re-enable_system-apps");

        preOtherStartSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preStartDevSettings.setOnPreferenceClickListener(preference -> {
            if (Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == Constants.SETTING_ON) {
                try {
                    if (Common.getDchaCompletedPast() && BenesseExtension.getDchaState() != Constants.DIGICHALIZE_STATUS_DIGICHALIZED) {
                        Settings.System.putInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, Constants.DIGICHALIZE_STATUS_DIGICHALIZED);
                    }
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                } catch (ActivityNotFoundException ignored) {
                }
            } else {
                Toast.makeText(requireActivity(), R.string.toast_no_debug_option, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preStartUiAdjustment.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent().setClassName("com.android.systemui", "com.android.systemui.DemoMode").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preWebView.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(requireActivity(), WebViewActivity.class).putExtra(Constants.URI_EXTRA_NAME, "https://www.google.com/intl/ja/").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (Exception ignored) {
                new DialogUtil(requireActivity())
                        .setMessage(R.string.dialog_error_web_view)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preLaunchApp.setOnPreferenceClickListener(preference -> {
            showLoadingDialog(getString(R.string.progress_state_check_file));
            Context appContext = requireContext().getApplicationContext();
            AppExecutors.io(() -> {
                List<LaunchAppListView.AppData> dataList = loadLaunchableApps(appContext);
                AppExecutors.main(() -> {
                    if (!isAdded()) {
                        cancelLoadingDialog();
                        return;
                    }
                    cancelLoadingDialog();
                    showLaunchAppDialog(dataList);
                });
            });
            return false;
        });

        preReEnableSystemApps.setOnPreferenceClickListener(preference -> {
            if (enableSystemApps()) {
                showSystemAppsEnabledDialog();
                return false;
            }

            showLoadingDialog(getString(R.string.progress_state_processing));
            Common.acquireBypassPermission(requireActivity(), success -> {
                cancelLoadingDialog();

                if (!isAdded()) {
                    return;
                }

                if (success && enableSystemApps()) {
                    showSystemAppsEnabledDialog();
                    return;
                }

                new DialogUtil(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_error)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            });
            return false;
        });

        preDeviceInfo.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), DeviceInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            requireActivity().overridePendingTransition(0, 0);
            return false;
        });
        initPreference();
    }

    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(requireActivity(), message);
    }

    private void cancelLoadingDialog() {
        Common.cancelDialog(progressDialog);
    }

    @Override
    public void onDestroyView() {
        cancelLoadingDialog();
        super.onDestroyView();
    }

    @NonNull
    private List<LaunchAppListView.AppData> loadLaunchableApps(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedAppList = packageManager.getInstalledApplications(0);
        List<LaunchAppListView.AppData> dataList = new ArrayList<>();

        for (ApplicationInfo app : installedAppList) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) == null) {
                continue;
            }

            LaunchAppListView.AppData data = new LaunchAppListView.AppData();
            data.icon = app.loadIcon(packageManager);
            data.label = app.loadLabel(packageManager).toString();
            data.packName = app.packageName;
            dataList.add(data);
        }
        return dataList;
    }

    private void showLaunchAppDialog(@NonNull List<LaunchAppListView.AppData> dataList) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launch_app_list, null);
        ListView listView = view.findViewById(R.id.launch_app_list);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new LaunchAppListView.LaunchAppAdapter(requireActivity(), dataList));
        listView.setOnItemClickListener((parent, mView, position, id) -> {
            try {
                startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage(dataList.get(position).packName));
            } catch (Exception e) {
                new DialogUtil(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        });

        new DialogUtil(requireActivity())
                .setView(view)
                .setTitle(R.string.dialog_title_choose_app)
                .setPositiveButton(R.string.dialog_common_cancel, null)
                .show();
    }

    private boolean enableSystemApps() {
        try {
            PackageManager packageManager = requireActivity().getPackageManager();
            packageManager.setApplicationEnabledSetting("com.android.browser", PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            packageManager.setApplicationEnabledSetting("com.android.quicksearchbox", PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            packageManager.setApplicationEnabledSetting("com.android.traceur", PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private void showSystemAppsEnabledDialog() {
        new DialogUtil(requireActivity())
                .setMessage(R.string.dialog_success_enable_system_apps)
                .setPositiveButton(R.string.dialog_common_ok, null)
                .show();
    }

    private void initPreference() {
        if (!Common.getDchaCompletedPast()) {
            preReEnableSystemApps.setVisible(false);
        }

        if (Common.isCT2()) {
            preStartUiAdjustment.setEnabled(false);
            preStartUiAdjustment.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
        }

        if (!Common.isCTZ()) {
            preReEnableSystemApps.setEnabled(false);
            preReEnableSystemApps.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
        }

        if (Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV)) {
            preWebView.setVisible(false);
        }
    }
}
