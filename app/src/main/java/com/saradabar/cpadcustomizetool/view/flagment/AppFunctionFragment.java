package com.saradabar.cpadcustomizetool.view.flagment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity;
import com.saradabar.cpadcustomizetool.view.activity.NormalActivity;
import com.saradabar.cpadcustomizetool.view.views.NormalModeHomeAppListView;

import java.util.ArrayList;
import java.util.List;

public class AppFunctionFragment extends PreferenceFragmentCompat {

    Preference preEmgManual,
            preEmgExecute,
            preEmgShortcut,
            preSelNorLauncher,
            preNorManual,
            preNorExecute,
            preNorShortcut;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pre_app_function, rootKey);

        preEmgManual = findPreference("pre_emg_manual");
        preEmgExecute = findPreference("pre_emg_execute");
        preEmgShortcut = findPreference("pre_emg_shortcut");
        preSelNorLauncher = findPreference("pre_sel_nor_launcher");
        preNorManual = findPreference("pre_nor_manual");
        preNorExecute = findPreference("pre_nor_execute");
        preNorShortcut = findPreference("pre_nor_shortcut");

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
                initPreference();
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
        initPreference();
    }

    private void initPreference() {
        try {
            // "com.android.launcher" + (Build.VERSION.SDK_INT == 22 ? "2" : "3");
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_message_2, requireActivity().getPackageManager().getApplicationLabel(requireActivity()
                    .getPackageManager().getApplicationInfo(Preferences.load(requireActivity(), Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, ""), 0))));
        } catch (PackageManager.NameNotFoundException ignored) {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_no_setting_launcher));
        }
    }
}
