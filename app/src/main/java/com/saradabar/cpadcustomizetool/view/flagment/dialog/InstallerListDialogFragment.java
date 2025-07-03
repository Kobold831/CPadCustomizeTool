package com.saradabar.cpadcustomizetool.view.flagment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.views.UpdateModeListView;

import java.util.ArrayList;
import java.util.List;

public class InstallerListDialogFragment extends DialogFragment {

    public interface InstallerListDialogListener {
        void onPositiveButton();
    }

    final int reqCode;
    final InstallerListDialogListener installerListDialogListener;

    public InstallerListDialogFragment(int reqCode, InstallerListDialogListener installerListDialogListener) {
        this.reqCode = reqCode;
        this.installerListDialogListener = installerListDialogListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = requireActivity();
        Context context = requireActivity();
        View viewInstallerList = getLayoutInflater().inflate(R.layout.layout_update_list, null);
        List<UpdateModeListView.AppData> dataList = new ArrayList<>();
        int i = 0;

        for (String str1 : Constants.LIST_UPDATE_MODE) {
            UpdateModeListView.AppData data = new UpdateModeListView.AppData();
            data.label = str1;
            data.updateMode = i;
            dataList.add(data);
            i++;
        }
        ListView listView = viewInstallerList.findViewById(R.id.update_list);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new UpdateModeListView.AppListAdapter(context, dataList));
        listView.setOnItemClickListener((parent, mView, position, id) -> {
            switch (position) {
                case 0:// パッケージインストーラー
                    Preferences.save(context, Constants.KEY_INT_UPDATE_MODE, (int) id);
                    listView.invalidateViews();
                    break;
                case 1:// Adb
                    Preferences.save(context, Constants.KEY_INT_UPDATE_MODE, (int) id);
                    listView.invalidateViews();
                    break;
                case 2:// Dcha
                    if (reqCode != 0 &&
                            !Preferences.load(context, Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
                        // reqCodeが0以外(MainActivityからの要求以外)かつDcha機能を使用する設定が無効
                        new DialogUtil(context)
                                .setMessage(getString(R.string.pre_app_sum_confirmation_dcha))
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                        return;
                    }

                    try {
                        if (Common.isDchaActive(requireActivity()) && !Common.isCT2() ||
                                Common.isDchaActive(requireActivity()) && Common.isCT2() && requireActivity().getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, 0).versionCode > 4) {
                            // dchaが機能でCT2でないまたはCT2でdchaでのサイレントインストールが可能
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
                            return;
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                    new DialogUtil(context)
                            .setMessage(getString(R.string.dialog_error_no_mode))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    break;
                case 3:// デバイスオーナー
                    if (((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(context.getPackageName()) &&
                            !Common.isCT2()) {
                        // このアプリがデバイスオーナーかつCT2ではない
                        Preferences.save(context, Constants.KEY_INT_UPDATE_MODE, (int) id);
                        listView.invalidateViews();
                    } else {
                        new DialogUtil(context)
                                .setMessage(getString(R.string.dialog_error_no_mode))
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                    }
                    break;
                case 4://  Dhizuku
                    if (Common.isCT2() || !Common.isDhizukuActive(context)) {
                        // CT2またはDhizuku が動作していない
                        new DialogUtil(context)
                                .setMessage(getString(R.string.dialog_error_no_mode))
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                        return;
                    }

                    if (Dhizuku.init() && !Dhizuku.isPermissionGranted()) {
                        // Dhizukuが動作しているが、権限なし
                        Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                            @Override
                            public void onRequestPermission(int grantResult) {
                                activity.runOnUiThread(() -> {
                                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                        Preferences.save(context, Constants.KEY_INT_UPDATE_MODE, (int) id);
                                        listView.invalidateViews();
                                    } else {
                                        new DialogUtil(context)
                                                .setMessage(R.string.dialog_dhizuku_deny_permission)
                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                .show();
                                    }
                                });
                            }
                        });
                        return;
                    }

                    if (Common.isDhizukuAllActive(context)) {
                        // Dhizukuが動作していて権限あり
                        try {
                            if (context.getPackageManager().getPackageInfo(DhizukuVariables.OFFICIAL_PACKAGE_NAME, 0).versionCode < 12) {
                                // Dhizukuのバージョンコードが12未満
                                new DialogUtil(context)
                                        .setCancelable(false)
                                        .setMessage(getString(R.string.dialog_dhizuku_require_12))
                                        .setPositiveButton(getString(R.string.dialog_common_ok), null)
                                        .show();
                            }
                            Preferences.save(context, Constants.KEY_INT_UPDATE_MODE, (int) id);
                            listView.invalidateViews();
                        } catch (PackageManager.NameNotFoundException ignored) {
                            new DialogUtil(context)
                                    .setMessage(getString(R.string.dialog_error_no_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    }
                    break;
            }
        });
        return new DialogUtil(context)
                .setCancelable(false)
                .setView(viewInstallerList)
                .setTitle(getString(R.string.dialog_title_select_mode))
                .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> installerListDialogListener.onPositiveButton())
                .show();
    }
}
