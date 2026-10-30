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

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.flagment.dialog.InstallerListDialogFragment;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;

public class AppSettingsFragment extends PreferenceFragmentCompat {

    PreferenceCategory catDebugRestriction;

    SwitchPreferenceCompat swDisableUpdateCheck,
            swUseDcha,
            swDebugRestriction,
            swSimpleMode,
            swNormalEnv;

    Preference preCrashLog,
            preDelCrashLog,
            preUpdateMode,
            preClearCache,
            preClearData,
            preDebugDevice;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_app_settings, rootKey);

        swDisableUpdateCheck = findPreference("pre_app_update_check");
        swUseDcha = findPreference("pre_app_use_dcha");
        preCrashLog = findPreference("pre_app_crash_log");
        preDelCrashLog = findPreference("pre_app_del_crash_log");
        preUpdateMode = findPreference("pre_app_update_mode");
        catDebugRestriction = findPreference("pre_app_category_debug");
        swDebugRestriction = findPreference("pre_app_debug_restriction");
        preClearCache = findPreference("pre_app_clear_cache");
        preClearData = findPreference("pre_app_clear_data");
        preDebugDevice = findPreference("pre_app_debug_device");
        swSimpleMode = findPreference("pre_app_simple_mode");
        swNormalEnv = findPreference("pre_app_normal_env");

        swDisableUpdateCheck.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                swDisableUpdateCheck.setSummary(R.string.pre_app_sum_update_check_off);
            } else {
                swDisableUpdateCheck.setSummary(R.string.pre_app_sum_update_check_on);
            }
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, !((boolean) newValue));
            return true;
        });

        swUseDcha.setOnPreferenceChangeListener((preference, newValue) -> {
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, (boolean) newValue);
            return true;
        });

        preCrashLog.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), CrashLogActivity.class));
            return false;
        });

        preDelCrashLog.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_check_delete)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        Preferences.delete(requireActivity(), Constants.KEY_LIST_CRASH_LOG);
                        Toast.makeText(requireActivity(), R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preUpdateMode.setOnPreferenceClickListener(preference -> {
            new InstallerListDialogFragment(Constants.INSTALLER_LIST_FROM_SETTINGS, () -> {
            }).show(requireActivity().getSupportFragmentManager(), "");
            return false;
        });

        swDebugRestriction.setOnPreferenceChangeListener((preference, newValue) -> {
            Common.setFlagEnabled(requireActivity(), Constants.KEY_DEBUG_RESTRICTION, (boolean) newValue);
            return true;
        });

        preClearCache.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_check_delete)
                    .setPositiveButton(getString(R.string.dialog_common_ok), (dialog, which) -> {
                        if (requireActivity().getCacheDir().delete()) {
                            if (Common.deleteDirectory(requireActivity().getExternalCacheDir())) {
                                Toast.makeText(requireActivity(), R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireActivity(), R.string.toast_not_deleted, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(getString(R.string.dialog_common_cancel), null)
                    .show();
            return false;
        });

        preClearData.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_confirm_delete)
                    .setPositiveButton(getString(R.string.dialog_common_yes), (dialog, which) -> {
                        ActivityManager activityManager = (ActivityManager) requireActivity().getSystemService(Service.ACTIVITY_SERVICE);
                        activityManager.clearApplicationUserData();
                    })
                    .setNegativeButton(getString(R.string.dialog_common_cancel), null)
                    .show();
            return false;
        });

        preDebugDevice.setOnPreferenceClickListener(preference -> {
            AppCompatEditText editText = new AppCompatEditText(requireActivity());
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setHint(String.valueOf(Preferences.load(requireActivity(), Constants.KEY_INT_DEBUG_DEVICE, 0)));
            new DialogUtil(requireActivity())
                    .setView(editText)
                    .setPositiveButton(getString(R.string.dialog_common_ok), (dialog, which) -> {
                        if (editText.getText() == null || editText.length() != Constants.DEBUG_DEVICE_DIGIT_LENGTH) {
                            return;
                        }
                        Common.setFlag(requireActivity(), Constants.KEY_INT_DEBUG_DEVICE, Integer.parseInt(editText.getText().toString()));
                    })
                    .setNegativeButton(getString(R.string.dialog_common_cancel), null)
                    .show();
            return false;
        });

        swSimpleMode.setOnPreferenceChangeListener((preference, o) -> {
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_SIMPLE_MODE, (boolean) o);
            return true;
        });

        swNormalEnv.setOnPreferenceChangeListener((preference, o) -> {
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV, (boolean) o);

            if ((boolean) o) {
                // 通常環境モードに設定されている場合に、一部の機能を停止
                Common.setNormalEnv(requireActivity());
            } else {
                // オフにされたときは、ダイアログの表示フラグをリセット
                Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_ALREADY_DIALOG_NORMAL_ENV, false);
            }
            return true;
        });
        initPreference();
    }

    private void initPreference() {
        swDisableUpdateCheck.setChecked(!Preferences.load(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, true));
        swUseDcha.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, false));
        swSimpleMode.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_SIMPLE_MODE, Constants.DEF_BOOL));
        swNormalEnv.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL));

        if (Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, true)) {
            swDisableUpdateCheck.setSummary(R.string.pre_app_sum_update_check_on);
        } else {
            swDisableUpdateCheck.setSummary(R.string.pre_app_sum_update_check_off);
        }

        if (!Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION)) {
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, false);
            swUseDcha.setChecked(false);
            swUseDcha.setSummary(getString(R.string.pre_app_sum_confirmation_dcha));
            swUseDcha.setEnabled(false);
        }

        if (BuildConfig.DEBUG) {
            catDebugRestriction.setVisible(true);
        }

        if (Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV)) {
            swUseDcha.setVisible(false);
        }
    }
}
