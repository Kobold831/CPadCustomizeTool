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

import android.annotation.SuppressLint;
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
import com.saradabar.cpadcustomizetool.util.Common;
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
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.view_time_out, null);
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
                    .setTitle("スクリーンのタイムアウト")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        //noinspection SizeReplaceableByIsEmpty
                        if (editText.getText() == null || editText.getText().length() == 0) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage("数値を入力してください。")
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        try {
                            Integer.parseInt(editText.getText().toString());
                        } catch (NumberFormatException ignored) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage("数値が、\"2147483647\" より大きいため、変更できません。")
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        try {
                            Settings.Secure.putInt(requireActivity().getContentResolver(), "sleep_timeout", Integer.parseInt(editText.getText().toString()));
                        } catch (Exception e) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage("\"android.permission.WRITE_SECURE_SETTINGS\" 権限が必要です。")
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
            if ((boolean) o) {
                Common.exec("cmd uimode night yes");
                swUiModeNight.setSummary("外観モードは、\"ダークモード\" に設定されています。");
            } else {
                Common.exec("cmd uimode night no");
                swUiModeNight.setSummary("外観モードは、\"ライトモード\" に設定されています。");
            }
            return true;
        });
        initPreference();
    }

    /** @noinspection SequencedCollectionMethodCanBeUsed*/
    private void initPreference() {
        setSummaryScreenOffTimeConvert();
        ArrayList<String> stringArrayList = Common.exec("cmd uimode night");

        if (!stringArrayList.isEmpty()) {
            if (String.valueOf(stringArrayList.get(0)).equals("Night mode: yes")) {
                swUiModeNight.setChecked(true);
                swUiModeNight.setSummary("外観モードは、\"ダークモード\" に設定されています。");
            } else if (String.valueOf(stringArrayList.get(0)).equals("Night mode: no")) {
                swUiModeNight.setChecked(false);
                swUiModeNight.setSummary("外観モードは、\"ライトモード\" に設定されています。");
            }
        }

        if (Common.isCT2()) {
            swUiModeNight.setEnabled(false);
            swUiModeNight.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
        }

        if (Common.isCT3()) {
            swUiModeNight.setEnabled(false);
            swUiModeNight.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
        }
    }

    private void setTextScreenOffTimeConvert(@NonNull AppCompatTextView textView, AppCompatEditText editText) {
        long time, sec, min, hour, day;
        int editTime;

        //noinspection SizeReplaceableByIsEmpty
        if (editText.getText() == null || editText.getText().length() == 0) {
            editTime = 0;
        } else {
            try {
                editTime = Integer.parseInt(editText.getText().toString());
            } catch (NumberFormatException ignored) {
                editTime = 0;
            }
        }
        time = editTime / 1000;
        sec = time % 60;
        min = (time / 60) % 60;
        hour = (time / 3600) % 24;
        day = (time / 86400) % 31;
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.SECOND, (int) time);

        date = calendar.getTime();
        DateFormat df = new SimpleDateFormat("yyyy 年 MM 月 dd 日 HH:mm:ss", Locale.JAPAN);

        textView.setText(getString(R.string.time_out_label, editTime + " ミリ秒" + " (" + time + " 秒" + ") ", day + " 日 " + hour + " 時間 " + min + " 分 " + sec + " 秒", df.format(date)));
    }

    private void setSummaryScreenOffTimeConvert() {
        long time, sec, min, hour, day;

        time = Settings.System.getInt(requireActivity().getContentResolver(), "screen_off_timeout", 60) / 1000;
        sec = time % 60;
        min = (time / 60) % 60;
        hour = (time / 3600) % 24;
        day = (time / 86400) % 31;

        preScreenOffTimeOut.setSummary("操作が行われない状態で " + day + " 日 " + hour + " 時間 " + min + " 分 " + sec + " 秒" + "経過後");
    }
}
