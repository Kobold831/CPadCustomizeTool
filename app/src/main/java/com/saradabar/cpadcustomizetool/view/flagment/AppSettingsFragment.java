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

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.views.UpdateModeListView;

import java.util.ArrayList;
import java.util.List;

public class AppSettingsFragment extends PreferenceFragmentCompat {

    PreferenceCategory catDebugRestriction;

    SwitchPreferenceCompat swUpdateCheck,
            swUseDcha,
            swDebugRestriction;

    Preference preCrashLog,
            preDelCrashLog,
            preUpdateMode,
            preClearCache,
            preClearData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_app);

        swUpdateCheck = (SwitchPreferenceCompat) findPreference("pre_app_update_check");
        swUseDcha = (SwitchPreferenceCompat) findPreference("pre_app_use_dcha");
        preCrashLog = findPreference("pre_app_crash_log");
        preDelCrashLog = findPreference("pre_app_del_crash_log");
        preUpdateMode = findPreference("pre_app_update_mode");
        catDebugRestriction = (PreferenceCategory) findPreference("pre_app_category_debug");
        swDebugRestriction = (SwitchPreferenceCompat) findPreference("pre_app_debug_restriction");
        preClearCache = findPreference("pre_app_clear_cache");
        preClearData = findPreference("pre_app_clear_data");

        swUpdateCheck.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_APP_START_UPDATE_CHECK, !((boolean) newValue));
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
            new AlertDialog.Builder(requireActivity())
                    .setMessage("消去しますか？")
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        Preferences.delete(requireActivity(), Constants.KEY_STRINGS_CRASH_LOG);
                        new AlertDialog.Builder(requireActivity())
                                .setMessage("消去しました。")
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preUpdateMode.setOnPreferenceClickListener(preference -> {
            View v = requireActivity().getLayoutInflater().inflate(R.layout.layout_update_list, null);
            List<UpdateModeListView.AppData> dataList = new ArrayList<>();
            int i = 0;

            for (String str : Constants.LIST_UPDATE_MODE) {
                UpdateModeListView.AppData data = new UpdateModeListView.AppData();
                data.label = str;
                data.updateMode = i;
                dataList.add(data);
                i++;
            }

            ListView listView = v.findViewById(R.id.update_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new UpdateModeListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                switch (position) {
                    case 0:
                        if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 1:
                        Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                        listView.invalidateViews();
                        break;
                    case 2:
                        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
                            if (tryBindDchaService() && Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                listView.invalidateViews();
                            } else {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage(getString(R.string.dialog_error_not_work_mode))
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                        .show();
                            }
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.pre_app_sum_confirmation_dcha))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 3:
                        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName()) && Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 4:
                        if (isDhizukuActive(requireActivity())) {
                            try {
                                if (requireActivity().getPackageManager().getPackageInfo("com.rosan.dhizuku", 0).versionCode > 11) {
                                    new AlertDialog.Builder(requireActivity())
                                            .setCancelable(false)
                                            .setMessage("Dhizuku の互換性がありません。バージョン 2.8 の Dhizuku をインストールしてください。")
                                            .setPositiveButton("OK", null)
                                            .show();
                                    return;
                                } else {
                                    Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                }
            });

            new AlertDialog.Builder(requireActivity())
                    .setView(v)
                    .setTitle(getString(R.string.dialog_title_select_mode))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        swDebugRestriction.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), "debug_restriction", (boolean) newValue);
            return true;
        });

        preClearCache.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage("消去しますか？")
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (requireActivity().getCacheDir().delete()) {
                            if (Common.deleteDirectory(requireActivity().getExternalCacheDir())) {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage("消去しました。")
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            }
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage("不要ファイルがないため消去できませんでした。")
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
            return false;
        });

        preClearData.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage("消去しますか？\nOK を押下すると、アプリは終了します。")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityManager activityManager = (ActivityManager) requireActivity().getSystemService(Service.ACTIVITY_SERVICE);
                        activityManager.clearApplicationUserData();
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
            return false;
        });

        initialize();
    }

    private void initialize() {
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryBindDchaService() {
        return requireActivity().bindService(Constants.DCHA_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
