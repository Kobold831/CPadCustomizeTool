package com.saradabar.cpadcustomizetool.view.flagment;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainOtherFragment extends PreferenceFragmentCompat {

    Preference preferenceOtherSettings,
            preferenceSysUiAdjustment,
            preferenceDevelopmentSettings,
            preferenceScreenOffTimeOut;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_main_setting, rootKey);
        preferenceOtherSettings = findPreference("intent_android_settings");
        preferenceSysUiAdjustment = findPreference("intent_sys_ui_adjustment");
        preferenceDevelopmentSettings = findPreference("intent_development_settings");
        preferenceScreenOffTimeOut = findPreference("android_screen_off_time");

        preferenceOtherSettings.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent().setClassName("com.android.settings", "com.android.settings.Settings").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preferenceDevelopmentSettings.setOnPreferenceClickListener(preference -> {
            if (Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1) {
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                } catch (ActivityNotFoundException ignored) {
                }
            } else {
                Toast.toast(getActivity(), R.string.toast_no_development_option);
            }
            return false;
        });

        preferenceSysUiAdjustment.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent().setClassName("com.android.systemui", "com.android.systemui.DemoMode").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        preferenceScreenOffTimeOut.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_time_out, null);
            Button button = view.findViewById(R.id.time_out_button);
            EditText editText = view.findViewById(R.id.time_out_edit);
            editText.setHint(getString(R.string.layout_time_out_hint, String.valueOf(Integer.MAX_VALUE)));
            setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
            new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setTitle("スクリーンのタイムアウト")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        try {
                            if (Integer.parseInt(editText.getText().toString()) >= 5000) {
                                Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.parseInt(editText.getText().toString()));
                                setSummaryScreenOffTimeConvert();
                            } else {
                                new AlertDialog.Builder(getActivity())
                                        .setMessage("最低値5000以下の値を設定することはできません")
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                            }
                        } catch (Exception ignored) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        }
                    })
                    .show();
            button.setOnClickListener(view1 -> {
                ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                try {
                    editText.setText(String.valueOf(Integer.MAX_VALUE));
                    Settings.System.putInt(requireActivity().getContentResolver(), "screen_off_timeout", Integer.MAX_VALUE);
                    setTextScreenOffTimeConvert(view.findViewById(R.id.time_out_label));
                    setSummaryScreenOffTimeConvert();
                } catch (Exception ignored) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_error)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                            .show();
                }
            });
            Runnable runnable = () -> ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
            new Handler().postDelayed(runnable, 10);
            return false;
        });
        if (Preferences.GET_MODEL_ID(getActivity()) == 0) {
            preferenceSysUiAdjustment.setEnabled(false);
            preferenceSysUiAdjustment.setSummary(Build.MODEL + "ではこの機能は使用できません");
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
        preferenceScreenOffTimeOut.setSummary("操作が行われない状態で" + day + "日" + hour + "時間" + min + "分" + sec + "秒" + "経過後");
    }
}