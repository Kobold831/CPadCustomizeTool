package com.saradabar.cpadcustomizetool.view.flagment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
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
            preDeviceInfo;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pre_util, rootKey);

        preOtherStartSettings = findPreference("pre_other_start_settings");
        preStartUiAdjustment = findPreference("pre_other_start_ui_adjustment");
        preStartDevSettings = findPreference("pre_other_start_dev_settings");
        preWebView = findPreference("pre_other_web_view");
        preLaunchApp = findPreference("pre_other_launch_app");
        preDeviceInfo = findPreference("pre_other_start_device_info");

        preOtherStartSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preStartDevSettings.setOnPreferenceClickListener(preference -> {
            if (Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1) {
                try {
                    // DchaCompletedPast && DchaState != 3
                    if (Common.getDchaCompletedPast() && Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 0) != 3) {
                        Settings.System.putInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 3);
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
            startActivity(new Intent(requireActivity(), WebViewActivity.class).putExtra("URL", "https://www.google.com/intl/ja/").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return false;
        });

        preLaunchApp.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launch_app_list, null);
            List<ApplicationInfo> installedAppList = requireActivity().getPackageManager().getInstalledApplications(0);
            List<LaunchAppListView.AppData> dataList = new ArrayList<>();

            for (ApplicationInfo app : installedAppList) {
                if(requireActivity().getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                    LaunchAppListView.AppData data = new LaunchAppListView.AppData();
                    data.icon = app.loadIcon(requireActivity().getPackageManager());
                    data.label = app.loadLabel(requireActivity().getPackageManager()).toString();
                    data.packName = app.packageName;
                    dataList.add(data);
                }
            }

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
                    .setTitle("アプリを選択")
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preDeviceInfo.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), DeviceInfoActivity.class));
            return false;
        });
        initPreference();
    }

    private void initPreference() {
        if (Common.isCT2()) {
            preStartUiAdjustment.setEnabled(false);
            preStartUiAdjustment.setSummary(Build.MODEL + requireActivity().getString(R.string.pre_main_sum_message_1));
        }
    }
}
