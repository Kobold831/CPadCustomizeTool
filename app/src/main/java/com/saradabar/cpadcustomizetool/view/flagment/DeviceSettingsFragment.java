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

package com.saradabar.cpadcustomizetool.view.flagment;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DeviceSettingsFragment extends PreferenceFragmentCompat {

    Preference preScreenOffTimeOut;

    SwitchPreferenceCompat swUiModeNight;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_device_settings, rootKey);

        preScreenOffTimeOut = findPreference("pre_other_screen_off_time");
        swUiModeNight = findPreference("pre_other_ui_mode_night");

        preScreenOffTimeOut.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_time_out, null);
            AppCompatEditText editText = view.findViewById(R.id.time_out_edit);
            editText.setHint(getString(R.string.time_out_hint, String.valueOf(Integer.MAX_VALUE)));
            editText.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label), editText);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label), editText);
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_screen_timeout)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        if (editText.getText() == null || editText.getText().length() == 0) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_input_number)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        try {
                            Integer.parseInt(editText.getText().toString());
                        } catch (NumberFormatException ignored) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_number_too_large)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        try {
                            Settings.Secure.putInt(requireActivity().getContentResolver(), "sleep_timeout", Integer.parseInt(editText.getText().toString()));
                        } catch (SecurityException ignored) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_need_write_secure_settings)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        try {
                            Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.parseInt(editText.getText().toString()));
                            Settings.System.putInt(requireActivity().getContentResolver(), "screen_dim_timeout", Integer.parseInt(editText.getText().toString()));
                            setSummaryScreenOffTimeConvert();
                        } catch (Exception e) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(e.getMessage())
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            view.findViewById(R.id.time_out_button).setOnClickListener(view1 -> {
                editText.setText(String.valueOf(Integer.MAX_VALUE));
                setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label), editText);
            });
            return false;
        });

        swUiModeNight.setOnPreferenceChangeListener((preference, o) -> {
            boolean enabled = (boolean) o;
            swUiModeNight.setSummary(enabled ? R.string.pre_other_sum_ui_mode_night : R.string.pre_other_sum_ui_mode_light);
            AppExecutors.io(() -> Common.exec(enabled ? "cmd uimode night yes" : "cmd uimode night no"));
            return true;
        });
        initPreference();
    }

    private void initPreference() {
        setSummaryScreenOffTimeConvert();

        if (Common.isCT2() || Common.isCT3()) {
            swUiModeNight.setEnabled(false);
            swUiModeNight.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            return;
        }

        AppExecutors.io(() -> {
            ArrayList<String> stringArrayList = Common.exec("cmd uimode night");
            AppExecutors.main(() -> {
                if (!isAdded() || swUiModeNight == null) {
                    return;
                }
                applyUiModeNight(stringArrayList);
            });
        });
    }

    private void applyUiModeNight(@NonNull ArrayList<String> stringArrayList) {
        for (String line : stringArrayList) {
            if (line.isEmpty()) {
                continue;
            }

            if (line.contains("yes")) {
                swUiModeNight.setChecked(true);
                swUiModeNight.setSummary(R.string.pre_other_sum_ui_mode_night);
            } else if (line.contains("no")) {
                swUiModeNight.setChecked(false);
                swUiModeNight.setSummary(R.string.pre_other_sum_ui_mode_light);
            }
            return;
        }
    }

    private void setTextScreenOffTimeConvert(@NonNull AppCompatTextView textView, @NonNull AppCompatEditText editText) {
        long time, sec, min, hour, day;
        int editTime;

        if (editText.getText() == null || editText.getText().length() == 0) {
            editTime = 0;
        } else {
            try {
                editTime = Integer.parseInt(editText.getText().toString());
            } catch (NumberFormatException ignored) {
                editTime = 0;
            }
        }
        time = editTime / getResources().getInteger(R.integer.sec_ms);
        sec = time % Constants.SEC_PER_MIN;
        min = (time / Constants.SEC_PER_MIN) % Constants.SEC_PER_MIN;
        hour = (time / Constants.SEC_PER_HOUR) % Constants.HOUR_PER_DAY;
        day = (time / Constants.SEC_PER_DAY) % Constants.DAY_PER_MONTH;
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.SECOND, (int) time);

        date = calendar.getTime();
        DateFormat df = new SimpleDateFormat(getString(R.string.date_format), Locale.JAPAN);

        textView.setText(getString(R.string.time_out_label, getString(R.string.time_out_value, editTime, time), getString(R.string.time_out_duration, day, hour, min, sec), df.format(date)));
    }

    private void setSummaryScreenOffTimeConvert() {
        long time, sec, min, hour, day;

        time = Settings.System.getInt(requireActivity().getContentResolver(), "screen_off_timeout",
                getResources().getInteger(R.integer.def_screen_off_timeout_ms)) / getResources().getInteger(R.integer.sec_ms);
        sec = time % Constants.SEC_PER_MIN;
        min = (time / Constants.SEC_PER_MIN) % Constants.SEC_PER_MIN;
        hour = (time / Constants.SEC_PER_HOUR) % Constants.HOUR_PER_DAY;
        day = (time / Constants.SEC_PER_DAY) % Constants.DAY_PER_MONTH;

        preScreenOffTimeOut.setSummary(getString(R.string.pre_other_sum_screen_off_duration, day, hour, min, sec));
    }
}
