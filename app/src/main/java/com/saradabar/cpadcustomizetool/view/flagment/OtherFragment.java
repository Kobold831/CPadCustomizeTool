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

package com.saradabar.cpadcustomizetool.view.flagment;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.view.activity.WebViewActivity;
import com.saradabar.cpadcustomizetool.view.views.LaunchAppView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OtherFragment extends PreferenceFragmentCompat {

    Preference preOtherStartSettings,
            preStartUiAdjustment,
            preStartDevSettings,
            preScreenOffTimeOut,
            preWebView,
            preLaunchApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_other);

        preOtherStartSettings = findPreference("pre_other_start_settings");
        preStartUiAdjustment = findPreference("pre_other_start_ui_adjustment");
        preStartDevSettings = findPreference("pre_other_start_dev_settings");
        preScreenOffTimeOut = findPreference("pre_other_screen_off_time");
        preWebView = findPreference("pre_other_web_view");
        preLaunchApp = findPreference("pre_other_launch_app");

        preOtherStartSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent().setClassName("com.android.settings", "com.android.settings.Settings").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preStartDevSettings.setOnPreferenceClickListener(preference -> {
            if (Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1) {
                try {
                    if (!Constants.COUNT_DCHA_COMPLETED_FILE.exists()) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        return false;
                    } else if (Constants.COUNT_DCHA_COMPLETED_FILE.exists()) {
                        if (Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 0) != 0) {
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        } else {
                            Settings.System.putInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 3);
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        }
                    }
                } catch (ActivityNotFoundException ignored) {
                }
            } else {
                Toast.toast(requireActivity(), R.string.toast_no_development_option);
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

        preScreenOffTimeOut.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_time_out, null);
            EditText editText = view.findViewById(R.id.time_out_edit);
            editText.setHint(getString(R.string.layout_time_out_hint, String.valueOf(Integer.MAX_VALUE)));
            setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle("スクリーンのタイムアウト")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        try {
                            Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.parseInt(editText.getText().toString()));
                            setSummaryScreenOffTimeConvert();
                        } catch (Exception e) {
                            new AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.dialog_error)
                                    .setMessage(e.getMessage())
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            view.findViewById(R.id.time_out_button).setOnClickListener(view1 -> {
                try {
                    editText.setText(String.valueOf(Integer.MAX_VALUE));
                    Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.MAX_VALUE);
                    setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
                    setSummaryScreenOffTimeConvert();
                } catch (Exception e) {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.dialog_error)
                            .setMessage(e.getMessage())
                            .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                            .show();
                }
            });
            return false;
        });

        preWebView.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), WebViewActivity.class).putExtra("URL", "https://www.google.com").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return false;
        });

        preLaunchApp.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_app_list, null);
            List<ApplicationInfo> installedAppList = requireActivity().getPackageManager().getInstalledApplications(0);
            List<LaunchAppView.AppData> dataList = new ArrayList<>();

            for (ApplicationInfo app : installedAppList) {
                if(requireActivity().getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                    LaunchAppView.AppData data = new LaunchAppView.AppData();
                    data.icon = app.loadIcon(requireActivity().getPackageManager());
                    data.label = app.loadLabel(requireActivity().getPackageManager()).toString();
                    data.packName = app.packageName;
                    dataList.add(data);
                }
            }

            ListView listView = view.findViewById(R.id.app_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new LaunchAppView.LaunchAppAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                try {
                    startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage(dataList.get(position).packName));
                } catch (Exception e) {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("エラーが発生しました")
                            .setMessage(e.getMessage())
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                            .show();
                }
            });

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setTitle("アプリを選択してください")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT2) {
            preStartUiAdjustment.setEnabled(false);
            preStartUiAdjustment.setSummary(Build.MODEL + "ではこの機能は使用できません");
        }
        setSummaryScreenOffTimeConvert();
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        setSummaryScreenOffTimeConvert();
    }

    private void setTextScreenOffTimeConvert(TextView textView) {
        long time, sec, min, hour, day;

        time = Settings.System.getInt(requireActivity().getContentResolver(), "screen_off_timeout", 60) / 1000;
        sec = time % 60;
        min = (time / 60) % 60;
        hour = (time / 3600) % 24;
        day = (time / 86400) % 31;
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.SECOND, (int) time);

        date = calendar.getTime();
        DateFormat df = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.JAPAN);

        textView.setText(getString(R.string.layout_time_out_label, Settings.System.getInt(requireActivity().getContentResolver(), "screen_off_timeout", 60) + "（" + time + "）", day + "日" + hour + "時間" + min + "分" + sec + "秒", df.format(date)));
    }

    private void setSummaryScreenOffTimeConvert() {
        long time, sec, min, hour, day;

        time = Settings.System.getInt(requireActivity().getContentResolver(), "screen_off_timeout", 60) / 1000;
        sec = time % 60;
        min = (time / 60) % 60;
        hour = (time / 3600) % 24;
        day = (time / 86400) % 31;

        preScreenOffTimeOut.setSummary("操作が行われない状態で" + day + "日" + hour + "時間" + min + "分" + sec + "秒" + "経過後");
    }
}