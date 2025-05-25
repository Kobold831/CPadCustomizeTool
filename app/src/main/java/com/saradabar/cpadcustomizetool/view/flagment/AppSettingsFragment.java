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

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.AlwaysNotiService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.flagment.dialog.InstallerListDialogFragment;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.activity.ForceCrashActivity;

public class AppSettingsFragment extends PreferenceFragmentCompat {

    PreferenceCategory catDebugRestriction;

    SwitchPreferenceCompat swUpdateCheck,
            swNotiAlways,
            swUseDcha,
            swDebugRestriction;

    Preference preCrashLog,
            preDelCrashLog,
            preUpdateMode,
            preClearCache,
            preClearData,
            preDebugForceCrash;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_app_settings, rootKey);

        swUpdateCheck = findPreference("pre_app_update_check");
        swNotiAlways = findPreference("pre_app_noti_always");
        swUseDcha = findPreference("pre_app_use_dcha");
        preCrashLog = findPreference("pre_app_crash_log");
        preDelCrashLog = findPreference("pre_app_del_crash_log");
        preUpdateMode = findPreference("pre_app_update_mode");
        catDebugRestriction = findPreference("pre_app_category_debug");
        swDebugRestriction = findPreference("pre_app_debug_restriction");
        preClearCache = findPreference("pre_app_clear_cache");
        preClearData = findPreference("pre_app_clear_data");
        preDebugForceCrash = findPreference("pre_app_debug_force_crash");

        swUpdateCheck.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, !((boolean) newValue));
            return true;
        });

        swNotiAlways.setOnPreferenceChangeListener((preference, newValue) -> {
            Intent notiService = new Intent(requireContext(), AlwaysNotiService.class);
            if ((boolean) newValue)
                ContextCompat.startForegroundService(requireContext(), notiService);
            else
                requireContext().stopService(notiService);
            return true;
        });

        swUseDcha.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, (boolean) newValue);
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
            new InstallerListDialogFragment(1, () -> {
            }).show(requireActivity().getSupportFragmentManager(), "");
            return false;
        });

        swDebugRestriction.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), "debug_restriction", (boolean) newValue);
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

        preDebugForceCrash.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), ForceCrashActivity.class));
            return false;
        });
        initPreference();
    }

    private void initPreference() {
        swUpdateCheck.setChecked(!Preferences.load(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, true));
        swUseDcha.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, false));

        if (!Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_APP_SETTING_DCHA, false);
            swUseDcha.setChecked(false);
            swUseDcha.setSummary(getString(R.string.pre_app_sum_confirmation_dcha));
            swUseDcha.setEnabled(false);
        }

        if (BuildConfig.DEBUG) {
            catDebugRestriction.setVisible(true);
        }
    }
}
