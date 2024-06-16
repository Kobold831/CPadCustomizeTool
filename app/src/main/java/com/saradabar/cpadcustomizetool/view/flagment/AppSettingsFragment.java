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

import android.app.AlertDialog;
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
import com.saradabar.cpadcustomizetool.view.views.SingleListView;

import java.util.ArrayList;
import java.util.List;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class AppSettingsFragment extends PreferenceFragmentCompat {

    IDchaService mDchaService;

    PreferenceCategory catDebugRestriction;

    SwitchPreferenceCompat swUpdateCheck,
            swUseDcha,
            swDebugRestriction;

    Preference preCrashLog,
            preDelCrashLog,
            preUpdateMode;

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

        swUpdateCheck.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE, !((boolean) newValue));
            return true;
        });

        swUseDcha.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_SETTINGS_DCHA, (boolean) newValue);
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
                        if (Preferences.delete(requireActivity(), Constants.KEY_CRASH_LOG)) {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage("消去しました")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preUpdateMode.setOnPreferenceClickListener(preference -> {
            View v = requireActivity().getLayoutInflater().inflate(R.layout.layout_update_list, null);
            List<SingleListView.AppData> dataList = new ArrayList<>();
            int i = 0;

            for (String str : Constants.list) {
                SingleListView.AppData data = new SingleListView.AppData();
                data.label = str;
                data.updateMode = i;
                dataList.add(data);
                i++;
            }

            ListView listView = v.findViewById(R.id.update_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new SingleListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                switch (position) {
                    case 0:
                        if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 1:
                        Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                        listView.invalidateViews();
                        break;
                    case 2:
                        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_SERVICE, false)) {
                            if (Common.tryBindDchaService(requireActivity(), mDchaService, null, mDchaServiceConnection, true, Constants.FLAG_CHECK, 0, 0, "", "") && Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
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
                        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName()) && Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
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
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
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

        initialize();
    }

    private void initialize() {
        swUpdateCheck.setChecked(!Preferences.load(requireActivity(), Constants.KEY_FLAG_UPDATE, true));
        swUseDcha.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_SETTINGS_DCHA, false));

        if (!Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_SETTINGS_DCHA, false);
            swUseDcha.setChecked(false);
            swUseDcha.setSummary(getString(R.string.pre_app_sum_confirmation_dcha));
            swUseDcha.setEnabled(false);
        }

        if (BuildConfig.DEBUG) {
            catDebugRestriction.setVisible(true);
        }
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
}