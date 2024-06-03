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
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.view.activity.WebViewActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class OtherFragment extends PreferenceFragmentCompat {

    Preference preOtherStartSettings,
            preStartUiAdjustment,
            preStartDevSettings,
            preScreenOffTimeOut,
            preWebView;

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
            Button button = view.findViewById(R.id.time_out_button);
            EditText editText = view.findViewById(R.id.time_out_edit);
            editText.setHint(getString(R.string.layout_time_out_hint, String.valueOf(Integer.MAX_VALUE)));
            setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle("スクリーンのタイムアウト")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        try {
                            if (Integer.parseInt(editText.getText().toString()) >= 5000) {
                                Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.parseInt(editText.getText().toString()));
                                setSummaryScreenOffTimeConvert();
                            } else {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage("最低値5000以下の値を設定することはできません")
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                            }
                        } catch (Exception ignored) {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            button.setOnClickListener(view1 -> {
                ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                try {
                    editText.setText(String.valueOf(Integer.MAX_VALUE));
                    Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.MAX_VALUE);
                    setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
                    setSummaryScreenOffTimeConvert();
                } catch (Exception ignored) {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.dialog_error)
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