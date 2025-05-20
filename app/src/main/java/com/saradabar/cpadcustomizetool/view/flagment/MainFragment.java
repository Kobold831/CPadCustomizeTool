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

import static com.saradabar.cpadcustomizetool.util.Common.isDchaUtilActive;
import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuAllActive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.data.task.ResolutionTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.EditAdminActivity;
import com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity;
import com.saradabar.cpadcustomizetool.view.activity.NormalActivity;
import com.saradabar.cpadcustomizetool.view.activity.NoticeActivity;
import com.saradabar.cpadcustomizetool.view.activity.RebootActivity;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.view.views.GetAppListView;
import com.saradabar.cpadcustomizetool.view.views.HomeAppListView;
import com.saradabar.cpadcustomizetool.view.views.NormalModeHomeAppListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @noinspection deprecation*/
public class MainFragment extends PreferenceFragmentCompat implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    private String downloadFileUrl;

    private SwitchPreferenceCompat swDchaState,
            swKeepDchaState,
            swNavigation,
            swKeepNavigation,
            swUnkSrc,
            swKeepUnkSrc,
            swAdb,
            swKeepAdb,
            swKeepLauncher,
            swDeviceAdmin,
            swEnableDchaService;

    private Preference preEmgManual,
            preEmgExecute,
            preEmgShortcut,
            preSelNorLauncher,
            preNorManual,
            preNorExecute,
            preNorShortcut,
            preOtherSettings,
            preReboot,
            preRebootShortcut,
            preSilentInstall,
            preLauncher,
            preResolution,
            preResetResolution,
            preDeviceOwnerFn,
            preEditAdmin,
            preSystemUpdate,
            preGetApp,
            preNotice;

    PreferenceCategory catEmergency,
            catNormal;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_main, rootKey);

        swDchaState = findPreference("pre_dcha_state");
        swKeepDchaState = findPreference("pre_keep_dcha_state");
        swNavigation = findPreference("pre_navigation");
        swKeepNavigation = findPreference("pre_keep_navigation");
        swUnkSrc = findPreference("pre_unk_src");
        swKeepUnkSrc = findPreference("pre_keep_unk_src");
        swAdb = findPreference("pre_adb");
        swKeepAdb = findPreference("pre_keep_adb");
        preLauncher = findPreference("pre_launcher");
        swKeepLauncher = findPreference("pre_keep_launcher");
        preOtherSettings = findPreference("pre_other_settings");
        swEnableDchaService = findPreference("pre_enable_dcha_service");
        preEmgManual = findPreference("pre_emg_manual");
        preEmgExecute = findPreference("pre_emg_execute");
        preEmgShortcut = findPreference("pre_emg_shortcut");
        preSelNorLauncher = findPreference("pre_sel_nor_launcher");
        preNorManual = findPreference("pre_nor_manual");
        preNorExecute = findPreference("pre_nor_execute");
        preNorShortcut = findPreference("pre_nor_shortcut");
        preReboot = findPreference("pre_reboot");
        preRebootShortcut = findPreference("pre_reboot_shortcut");
        preSilentInstall = findPreference("pre_silent_install");
        preResolution = findPreference("pre_resolution");
        preResetResolution = findPreference("pre_reset_resolution");
        preSystemUpdate = findPreference("pre_system_update");
        preDeviceOwnerFn = findPreference("pre_device_owner_fn");
        preEditAdmin = findPreference("pre_edit_admin");
        swDeviceAdmin = findPreference("pre_device_admin");
        preGetApp = findPreference("pre_get_app");
        preNotice = findPreference("pre_notice");
        catEmergency = findPreference("category_emergency");
        catNormal = findPreference("category_normal");

        setListener();
    }

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().getContentResolver().unregisterContentObserver(dchaStateObserver);
        requireActivity().getContentResolver().unregisterContentObserver(navigationBarObserver);
        requireActivity().getContentResolver().unregisterContentObserver(marketObserver);
        requireActivity().getContentResolver().unregisterContentObserver(usbDebugObserver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ACTIVITY_INSTALL:// サイレントインストール要求
                preSilentInstall.setEnabled(true);

                if (data == null) {
                    // 何も選択していない
                    return;
                }
                String installData = Common.getFilePath(requireActivity(), data.getData());

                if (installData != null) {
                    //　選択されたファイルの取得成功
                    new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), installData);
                    return;
                } else {
                    //　選択されたファイルの取得失敗
                    new DialogUtil(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
                break;
            case Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE:// システムアップデート要求
                preSystemUpdate.setEnabled(true);

                if (data == null) {
                    // 何も選択していない
                    return;
                }
                String updateData = Common.getFilePath(requireActivity(), data.getData());

                if (updateData != null) {
                    //　選択されたファイルの取得成功
                    new DchaServiceUtil(requireActivity()).execSystemUpdate(updateData, 0, object -> {
                        if (!object.equals(true)) {
                            new DialogUtil(requireActivity())
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    });
                } else {
                    //　選択されたファイルの取得失敗
                    new DialogUtil(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
                break;
            case Constants.REQUEST_ACTIVITY_ADMIN:
                initialize();
                break;
        }
    }

    private void setListener() {
        swDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }
            new DchaServiceUtil(requireActivity()).setSetupStatus((boolean) o ? 3 : 0, object -> {
            });
            return false;
        });

        swKeepDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object -> {
                });
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swNavigation.setOnPreferenceChangeListener((preference, o) -> {
            new DchaServiceUtil(requireActivity()).hideNavigationBar((boolean) o, object -> {
            });
            return false;
        });

        swKeepNavigation.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                new DchaServiceUtil(requireActivity()).hideNavigationBar(false, object -> {
                });
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? 1 : 0);
            } catch (RuntimeException ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? 1 : 0);
            } catch (RuntimeException ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.isCTX() || Common.isCTZ()) {
                    // CTXまたはCTZ
                    new DchaServiceUtil(requireActivity()).setSetupStatus(3, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(100);
                                if (!adbEnabled(true)) {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                }
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            }
                        } else {
                            // 失敗
                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // CT2またはCT3
                    if (!adbEnabled(true)) {
                        // 設定変更失敗
                        Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (!adbEnabled(false)) {
                    // 設定変更失敗
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        swKeepAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.isCTX() || Common.isCTZ()) {
                    // CTXまたはCTZ
                    new DchaServiceUtil(requireActivity()).setSetupStatus(3, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(100);
                                if (adbEnabled(true)) {
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                        if (object1.equals(true)) {
                                            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                                            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                                            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
                                        } else {
                                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                    });
                                }
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            }
                        } else {
                            // 失敗
                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // CT2またはCT3
                    if (adbEnabled(true)) {
                        Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
                    } else {
                        // 設定変更失敗
                        Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            } else {
                Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            }
            return true;
        });

        preLauncher.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<HomeAppListView.AppData> dataList = new ArrayList<>();

            for (ResolveInfo resolveInfo : installedAppList) {
                HomeAppListView.AppData data = new HomeAppListView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }
            ListView listView = view.findViewById(R.id.launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new HomeAppListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) ->
                    new DchaServiceUtil(requireActivity()).setPreferredHomeApp(getLauncherPackage(requireActivity()), Uri.fromParts("package",
                            installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""), object -> {
                        listView.invalidateViews();
                        initialize();
                    })
            );
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        swKeepLauncher.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                Preferences.save(requireActivity(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, getLauncherPackage(requireActivity()));
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        preOtherSettings.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new OtherFragment(), true);
            return false;
        });

        swEnableDchaService.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (Common.isShowCfmDialog(requireActivity())) {
                    // 確認ダイアログが必要
                    cfmDialog();
                    return false;
                }

                if (!Preferences.load(requireActivity(), "debug_restriction", false) &&
                        !Common.isDchaActive(requireActivity())) {
                    // デバッグモードが無効かつdcha接続失敗
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
                Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, true);
                initialize();
            } else {
                Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false);
                initialize();
            }
            return true;
        });

        preEmgManual.setOnPreferenceClickListener(preference -> {
            AppCompatTextView textView = new AppCompatTextView(requireActivity());
            textView.setText(R.string.dialog_emergency_manual_red);
            textView.setTextSize(16);
            textView.setTextColor(Color.RED);
            textView.setPadding(32, 0, 32, 0);
            new DialogUtil(requireActivity())
                    .setTitle(R.string.dialog_title_emergency_manual)
                    .setMessage(R.string.dialog_emergency_manual)
                    .setView(textView)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preEmgExecute.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.note_start_emergency_mode)
                    .setNeutralButton(R.string.dialog_common_yes, (dialog, which) ->
                            startActivity(new Intent(requireActivity(), EmergencyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preEmgShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_emergency))
                        .setShortLabel(getString(R.string.activity_emergency))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_dialog_alert))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), EmergencyActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), EmergencyActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_dialog_alert))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_emergency));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preSelNorLauncher.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_normal_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<NormalModeHomeAppListView.AppData> dataList = new ArrayList<>();

            for (ResolveInfo resolveInfo : installedAppList) {
                NormalModeHomeAppListView.AppData data = new NormalModeHomeAppListView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }
            ListView listView = view.findViewById(R.id.normal_launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new NormalModeHomeAppListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                Preferences.save(requireActivity(), Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, Uri.fromParts("package", installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""));
                /* listviewの更新 */
                listView.invalidateViews();
                initialize();
            });
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preNorManual.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setTitle(R.string.dialog_title_normal_manual)
                    .setMessage(R.string.dialog_normal_manual)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preNorExecute.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.note_start_normal_mode)
                    .setNeutralButton(R.string.dialog_common_yes, (dialog, which) ->
                            startActivity(new Intent(requireActivity(), NormalActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preNorShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_normal))
                        .setShortLabel(getString(R.string.activity_normal))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_menu_revert))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), NormalActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), NormalActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_menu_revert))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_normal));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                            new DchaServiceUtil(requireActivity()).rebootPad(0, "", object -> {
                            }))
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.reboot))
                        .setShortLabel(getString(R.string.reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_popup_sync))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_popup_sync))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preSilentInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSilentInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/vnd.android.package-archive")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (RuntimeException ignored) {
                preSilentInstall.setEnabled(true);
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preResolution.setOnPreferenceClickListener(preference -> {
            if (Common.isCT2() || Common.isCT3()) {
                // CT2またはCT3
                if (!isDchaUtilActive(requireActivity())) {
                    // DchaUtilService が機能していない
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                // sdk23以上かつWRITE_SECURE_SETTINGSが付与されていない
                LinearLayout linearLayoutAny = view.findViewById(R.id.v_resolution_linearlayout_any);
                LinearLayout linearLayoutBenesse = view.findViewById(R.id.v_resolution_linearlayout_benesse);
                linearLayoutAny.setVisibility(View.GONE);
                linearLayoutBenesse.setVisibility(View.VISIBLE);
                new DialogUtil(requireActivity())
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            AppCompatRadioButton rb1024 = view.findViewById(R.id.v_resolution_radio_1024);
                            AppCompatRadioButton rb1280 = view.findViewById(R.id.v_resolution_radio_1280);
                            AppCompatRadioButton rb1920 = view.findViewById(R.id.v_resolution_radio_1920);

                            int width, height;

                            if (rb1024.isChecked()) {
                                width = 1024;
                                height = 768;
                            } else if (rb1280.isChecked()) {
                                width = 1280;
                                height = 800;
                            } else if (rb1920.isChecked()) {
                                width = 1920;
                                height = 1200;
                            } else {
                                Toast.makeText(requireActivity(), R.string.toast_not_selected, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new ResolutionTask().execute(requireActivity(), ((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, null)
                        .show();
                return false;
            }
            new DialogUtil(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_resolution)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                        AppCompatEditText editTextWidth = view.findViewById(R.id.edit_text_1);
                        AppCompatEditText editTextHeight = view.findViewById(R.id.edit_text_2);

                        try {
                            //noinspection DataFlowIssue
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            //noinspection DataFlowIssue
                            int height = Integer.parseInt(editTextHeight.getText().toString());

                            if (width < 0 || height < 0) {
                                new DialogUtil(requireActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            } else {
                                new ResolutionTask().execute(requireActivity(), ((MainActivity) requireActivity()).resolutionTaskListener(), width, height);
                            }
                        } catch (NumberFormatException ignored) {
                            new DialogUtil(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_illegal_value)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preResetResolution.setOnPreferenceClickListener(preference -> {
            if (Common.isCT2() || Common.isCT3()) {
                // CT2またはCT3
                if (!isDchaUtilActive(requireActivity())) {
                    // DchaUtilService が機能していない
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }
            ((MainActivity) requireActivity()).resetResolution();
            return false;
        });

        preSystemUpdate.setOnPreferenceClickListener(preference -> {
            try {
                preSystemUpdate.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/zip")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE);
            } catch (RuntimeException ignored) {
                preSystemUpdate.setEnabled(true);
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preDeviceOwnerFn.setOnPreferenceClickListener(preference -> {
            requireActivity().runOnUiThread(() ->
                    ((MainActivity) requireActivity()).transitionFragment(new DeviceOwnerFragment(), true));
            return false;
        });

        preEditAdmin.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), EditAdminActivity.class));
            return false;
        });

        swDeviceAdmin.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (!((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class))) {
                    startActivityForResult(
                            new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(requireActivity(), DeviceAdminReceiver.class))
                                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.device_admin_detail))
                            , Constants.REQUEST_ACTIVITY_ADMIN);
                }
            } else {
                swDeviceAdmin.setChecked(true);
                new DialogUtil(requireActivity())
                        .setTitle(R.string.dialog_disable_device_admin)
                        .setMessage(R.string.dialog_question_admin)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            ((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(new ComponentName(requireActivity(), DeviceAdminReceiver.class));
                            swDeviceAdmin.setChecked(false);
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> swDeviceAdmin.setChecked(true))
                        .show();
            }
            return false;
        });

        preGetApp.setOnPreferenceClickListener(preference -> {
            showLoadingDialog(getString(R.string.progress_state_connecting));
            new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(requireActivity().getExternalCacheDir(), Constants.CHECK_JSON), Constants.REQUEST_DOWNLOAD_APP_CHECK);
            return false;
        });

        preNotice.setOnPreferenceClickListener(preference -> {
            requireActivity().startActivity(new Intent(requireActivity(), NoticeActivity.class));
            requireActivity().overridePendingTransition(0, 0);
            return false;
        });

        /* 一括変更 */
        initialize();
    }

    /* 初期化 */
    private void initialize() {
        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
            swEnableDchaService.setChecked(true);
            preSilentInstall.setVisible(true);
            preLauncher.setVisible(true);
            swKeepLauncher.setVisible(true);
            catEmergency.setVisible(true);
            catNormal.setVisible(true);
            preReboot.setVisible(true);
            preRebootShortcut.setVisible(true);
            preResolution.setVisible(true);
            preResetResolution.setVisible(true);
            preSystemUpdate.setVisible(true);
        } else {
            swEnableDchaService.setChecked(false);
            preSilentInstall.setVisible(false);
            preLauncher.setVisible(false);
            swKeepLauncher.setVisible(false);
            catEmergency.setVisible(false);
            catNormal.setVisible(false);
            preReboot.setVisible(false);
            preRebootShortcut.setVisible(false);
            preResolution.setVisible(false);
            preResetResolution.setVisible(false);
            preSystemUpdate.setVisible(false);
        }
        /* オブサーバーを有効化 */
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);

        try {
            swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }

        try {
            swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }

        try {
            //noinspection deprecation
            swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }

        try {
            swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        swDeviceAdmin.setChecked(((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class)));
        swKeepNavigation.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false));
        swKeepUnkSrc.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, false));
        swKeepDchaState.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false));
        swKeepLauncher.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, false));
        preLauncher.setSummary(getLauncherName(requireActivity()));

        try {
            // "com.android.launcher" + (Build.VERSION.SDK_INT == 22 ? "2" : "3");
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_message_2, requireActivity().getPackageManager().getApplicationLabel(requireActivity()
                    .getPackageManager().getApplicationInfo(Preferences.load(requireActivity(), Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, ""), 0))));
        } catch (PackageManager.NameNotFoundException ignored) {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_no_setting_launcher));
        }
        // サービス起動(必要ないときは自動終了)
        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));

        /* 端末ごとにPreferenceの状態を設定 */
        if (Common.isCT2()) {
            try {
                if (requireActivity().getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, 0).versionCode < 5) {
                    preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                    preSilentInstall.setEnabled(false);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preSilentInstall.setEnabled(false);
            }
        }

        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            preDeviceOwnerFn.setEnabled(false);
            preDeviceOwnerFn.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
            swDeviceAdmin.setVisible(false);
        }

        if (((UserManager) requireActivity().getSystemService(Context.USER_SERVICE)).hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            swKeepUnkSrc.setVisible(false);
            swUnkSrc.setVisible(false);
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }
        new FileDownloadTask().execute(this, Constants.URL_NOTICE, new File(requireActivity().getExternalCacheDir(), Constants.NOTICE_JSON), Constants.REQUEST_DOWNLOAD_NOTICE);
    }

    /* システムUIオブザーバー */
    final ContentObserver dchaStateObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* ナビゲーションバーオブザーバー */
    final ContentObserver navigationBarObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* 提供元オブザーバー */
    final ContentObserver marketObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* USBデバッグオブザーバー */
    final ContentObserver usbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    private boolean adbEnabled(boolean bool) {
        try {
            if (bool) {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                if (Common.isBenesseExtensionExist("getDchaState")) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);
                }
            } else {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                if (Common.isBenesseExtensionExist("getDchaState")) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 0);
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    private void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    private void cfmDialog() {
        new DialogUtil(requireActivity())
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_dcha_warning))
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                        Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION_CONFIRMATION, true))
                .setNegativeButton(R.string.dialog_common_cancel, null)
                .show();
    }

    private void startDownload(String str) {
        FileDownloadTask fileDownloadTask = new FileDownloadTask();
        fileDownloadTask.execute(this, str, new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK), Constants.REQUEST_DOWNLOAD_APK);
        ProgressHandler progressHandler = new ProgressHandler(Looper.getMainLooper());
        progressHandler.fileDownloadTask = fileDownloadTask;
        progressHandler.sendEmptyMessage(0);
        View view = getLayoutInflater().inflate(R.layout.view_progress, null);
        progressPercentText = view.findViewById(R.id.progress_percent);
        progressPercentText.setText("");
        progressByteText = view.findViewById(R.id.progress_byte);
        progressByteText.setText("");
        dialogProgressBar = view.findViewById(R.id.progress);
        dialogProgressBar.setProgress(0);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.setMessage("");
        progressDialog.show();
    }

    /* ランチャーのパッケージ名を取得 */
    @Nullable
    private String getLauncherPackage(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    /* ランチャーのアプリ名を取得 */
    @Nullable
    private String getLauncherName(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.loadLabel(context.getPackageManager()).toString();
        }
        return null;
    }

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return new DchaInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(requireActivity().getResources().getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    @SuppressLint("InflateParams")
    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APP_CHECK:
                cancelLoadingDialog();
                ArrayList<GetAppListView.AppData> appDataArrayList = new ArrayList<>();

                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), Constants.CHECK_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("appList");
                    String model = "";

                    if (Common.isCT2()) {
                        model = "CT2";
                    } else if (Common.isCT3()) {
                        model = "CT3";
                    } else if (Common.isCTX()) {
                        model = "CTX";
                    } else if (Common.isCTZ()) {
                        model = "CTZ";
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray jsonArray1 = jsonArray.getJSONObject(i).getJSONArray("targetModel");
                        for (int s = 0; s < jsonArray1.length(); s++) {
                            if (Objects.equals(jsonArray1.getString(s), model) || jsonArray1.getString(s).isEmpty()) {
                                GetAppListView.AppData data = new GetAppListView.AppData();
                                data.name = jsonArray.getJSONObject(i).getString("name");
                                data.description = jsonArray.getJSONObject(i).getString("description");
                                data.url = jsonArray.getJSONObject(i).getString("url");
                                appDataArrayList.add(data);
                            }
                        }
                    }
                } catch (JSONException | IOException ignored) {
                }
                View view = getLayoutInflater().inflate(R.layout.layout_app_list, null);
                ListView listView = view.findViewById(R.id.app_list);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setAdapter(new GetAppListView.AppListAdapter(requireActivity(), appDataArrayList));
                listView.setOnItemClickListener((parent, view1, position, id) -> {
                    // 選択位置を保存
                    Preferences.save(requireActivity(), Constants.KEY_INT_GET_APP_TMP, position);
                    listView.invalidateViews();
                });
                new DialogUtil(requireActivity())
                        .setView(view)
                        .setTitle("アプリを選択")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            StringBuilder str = new StringBuilder();
                            // 選択位置参照
                            int i = Preferences.load(requireActivity(), Constants.KEY_INT_GET_APP_TMP, 0);
                            str.append("アプリ名：")
                                    .append("\n")
                                    .append(appDataArrayList.get(i).name)
                                    .append("\n\n")
                                    .append("説明：")
                                    .append("\n")
                                    .append(appDataArrayList.get(i).description)
                                    .append("\n");
                            downloadFileUrl = appDataArrayList.get(i).url;

                            if (str.toString().isEmpty()) {
                                return;
                            }
                            new DialogUtil(requireActivity())
                                    .setMessage(str + "\n" + "よろしければ OK を押下してください。")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(appDataArrayList.get(i).url, "MYURL")) {
                                            startDownload(appDataArrayList.get(i).url);
                                            dialog.dismiss();
                                        } else {
                                            View view1 = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            AppCompatEditText editText = view1.findViewById(R.id.edit_app_url);
                                            new DialogUtil(requireActivity())
                                                    .setMessage("http:// または https:// を含む URL を指定してください。")
                                                    .setView(view1)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                                        //noinspection DataFlowIssue
                                                        startDownload(editText.getText().toString());
                                                    })
                                                    .setNegativeButton(R.string.dialog_common_cancel, null)
                                                    .show();
                                        }
                                    })
                                    .show();
                        })
                        .show();
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                cancelLoadingDialog();
                switch (Preferences.load(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1)) {
                    case 0:
                        requireActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(requireActivity().getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        new DialogUtil(requireActivity())
                                .setCancelable(false)
                                .setTitle(getString(R.string.dialog_title_error))
                                .setMessage(getString(R.string.dialog_no_installer, downloadFileUrl))
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                        break;
                    case 2:
                        new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (!dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1);
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), new ArrayList<>(List.of(new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                    case 4:
                        if (!isDhizukuAllActive(requireActivity())) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1);
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), new ArrayList<>(List.of(new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                }
                break;
            case Constants.REQUEST_DOWNLOAD_NOTICE:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), Constants.NOTICE_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("noticeList");

                    if (jsonArray.length() == 0) {
                        // 表示しない
                        preNotice.setVisible(false);
                    } else {
                        preNotice.setTitle("アプリのお知らせ");
                        preNotice.setSummary("お知らせは " + jsonArray.length() + " 件あります。タップして確認できます。");
                    }
                } catch (JSONException | IOException | IllegalStateException ignored) {
                }
                break;
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_CHECK -> {
                cancelLoadingDialog();
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_download))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onConnectionError(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_CHECK -> {
                cancelLoadingDialog();
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_connection))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressPercentText.setText(new StringBuilder(String.valueOf(progress)).append("%"));
        progressByteText.setText(new StringBuilder(String.valueOf(currentByte)).append(" MB").append("/").append(totalByte).append(" MB"));
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(new StringBuilder(getString(R.string.progress_state_download_file)));
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        apkInstallTaskListener().onSuccess();
    }

    @Override
    public void onInstallFailure(int reqCode, String message) {
        apkInstallTaskListener().onFailure(message);
    }

    @Override
    public void onInstallError(int reqCode, String message) {
        apkInstallTaskListener().onError(message);
    }

    private ApkInstallTask.Listener apkInstallTaskListener() {
        return new ApkInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                AlertDialog alertDialog = new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }
            }

            /* 失敗 */
            @Override
            public void onFailure(String message) {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }
}
