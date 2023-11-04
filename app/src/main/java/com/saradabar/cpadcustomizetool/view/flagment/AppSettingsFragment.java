package com.saradabar.cpadcustomizetool.view.flagment;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.views.SingleListView;

import java.util.ArrayList;
import java.util.List;

public class AppSettingsFragment extends PreferenceFragmentCompat {

    SwitchPreference swUpdateCheck,
            swUseDcha,
            swAdb;

    Preference preCrashLog,
            preDelCrashLog,
            preUpdateMode;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_app, rootKey);
        SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        ContentResolver resolver = requireActivity().getContentResolver();

        swUpdateCheck = findPreference("pre_app_update_check");
        swUseDcha = findPreference("pre_app_use_dcha");
        swAdb = findPreference("pre_app_adb");
        preCrashLog = findPreference("pre_app_crash_log");
        preDelCrashLog = findPreference("pre_app_del_crash_log");
        preUpdateMode = findPreference("pre_app_update_mode");

        swUpdateCheck.setChecked(!Preferences.GET_UPDATE_FLAG(getActivity()));
        swUseDcha.setChecked(Preferences.GET_CHANGE_SETTINGS_DCHA_FLAG(getActivity()));

        try {
            swAdb.setChecked(sp.getBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, false));
        } catch (NullPointerException e) {
            SharedPreferences.Editor spe = sp.edit();
            spe.putBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, false);
            spe.apply();
        }

        swUpdateCheck.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.SET_UPDATE_FLAG(!((boolean) newValue), getActivity());
            return true;
        });

        swUseDcha.setOnPreferenceChangeListener((preference, newValue) -> {
            Preferences.SET_CHANGE_SETTINGS_DCHA_FLAG((boolean) newValue, getActivity());
            return true;
        });

        swAdb.setOnPreferenceChangeListener((preference, newValue) -> {
            if (isCfmDialog()) {
                return false;
            }
            try {
                if (Preferences.GET_MODEL_ID(getActivity()) == 2)
                    Settings.System.putInt(resolver, Constants.DCHA_STATE, 3);
                Thread.sleep(100);
                Settings.Global.putInt(resolver, Settings.Global.ADB_ENABLED, 1);
                if (Preferences.GET_MODEL_ID(getActivity()) == 2)
                    Settings.System.putInt(resolver, Constants.DCHA_STATE, 0);
                sp.edit().putBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, (boolean) newValue).apply();
            } catch (SecurityException | InterruptedException ignored) {
                if (Preferences.GET_MODEL_ID(getActivity()) == 2)
                    Settings.System.putInt(resolver, Constants.DCHA_STATE, 0);
                Toast.toast(getActivity(), R.string.toast_not_change);
                swAdb.setChecked(false);
                return false;
            }
            return true;
        });

        preCrashLog.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), CrashLogActivity.class));
            return false;
        });

        preDelCrashLog.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(getActivity())
                    .setMessage("消去しますか？")
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        if (Preferences.REMOVE_CRASH_LOG(getActivity())) {
                            new AlertDialog.Builder(getActivity())
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
            List<String> list = new ArrayList<>();
            list.add("パッケージインストーラ");
            list.add("ADB");
            list.add("DchaService");
            list.add("デバイスオーナー");
            List<SingleListView.AppData> dataList = new ArrayList<>();
            int i = 0;
            for (String str : list) {
                SingleListView.AppData data = new SingleListView.AppData();
                data.label = str;
                data.updateMode = i;
                dataList.add(data);
                i++;
            }
            ListView listView = v.findViewById(R.id.update_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new SingleListView.AppListAdapter(getActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                switch (position) {
                    case 0:
                        if (Preferences.GET_MODEL_ID(getActivity()) != 2) {
                            Preferences.SET_UPDATE_MODE(getActivity(), (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 1:
                        Preferences.SET_UPDATE_MODE(getActivity(), (int) id);
                        listView.invalidateViews();
                        break;
                    case 2:
                        if (MainFragment.getInstance().bindDchaService(Constants.FLAG_CHECK, true) && Preferences.GET_MODEL_ID(getActivity()) != 0) {
                            Preferences.SET_UPDATE_MODE(getActivity(), (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                    case 3:
                        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName()) && Preferences.GET_MODEL_ID(getActivity()) != 0) {
                            Preferences.SET_UPDATE_MODE(getActivity(), (int) id);
                            listView.invalidateViews();
                        } else {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.dialog_error_not_work_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                        break;
                }
            });
            new AlertDialog.Builder(getActivity())
                    .setView(v)
                    .setTitle(getString(R.string.dialog_title_select_mode))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        if (!Preferences.GET_DCHASERVICE_FLAG(getActivity())) {
            Preferences.SET_CHANGE_SETTINGS_DCHA_FLAG(false, getActivity());
            swUseDcha.setChecked(false);
            swUseDcha.setSummary(getString(R.string.pre_app_sum_confirmation_dcha));
            swUseDcha.setEnabled(false);
        }

        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
                swAdb.setEnabled(false);
                swAdb.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
            case 1:
                swAdb.setEnabled(false);
                swAdb.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
        }
    }

    /* 確認ダイアログ */
    private boolean isCfmDialog() {
        if (!Constants.COUNT_DCHA_COMPLETED_FILE.exists() && Constants.IGNORE_DCHA_COMPLETED_FILE.exists()) {
            if (Preferences.GET_CONFIRMATION(getActivity())) {
                new AlertDialog.Builder(getActivity())
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_question_are_you_sure))
                        .setMessage(getString(R.string.dialog_confirmation))
                        .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> new AlertDialog.Builder(getActivity())
                                .setCancelable(false)
                                .setTitle(getString(R.string.dialog_title_final_confirmation))
                                .setMessage(getString(R.string.dialog_final_confirmation))
                                .setPositiveButton(R.string.dialog_common_continue, (dialog1, which1) -> {
                                    Preferences.SET_CONFIRMATION(true, getActivity());
                                    dialog1.dismiss();
                                })
                                .setNegativeButton(R.string.dialog_common_cancel, (dialog1, which1) -> dialog.dismiss())
                                .show())
                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}