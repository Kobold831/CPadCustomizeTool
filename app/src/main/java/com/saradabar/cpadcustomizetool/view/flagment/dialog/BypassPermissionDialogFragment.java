package com.saradabar.cpadcustomizetool.view.flagment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;

public class BypassPermissionDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireActivity();
        return new DialogUtil(context)
                .setTitle("機能を使用するために権限を付与しますか？")
                .setMessage("権限がないため、設定を変更できません。”OK”を押すと、権限設定を行います。処理は数秒で終わります。")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (Common.copyAssetsFile(context)) {
                        new DchaServiceUtil(context).setSetupStatus(3, object -> {
                            if (object.equals(true)) {
                                new DchaServiceUtil(context).installApp(context.getExternalCacheDir() + "/" + "base.apk", 2, object1 -> {
                                    if (object1.equals(true)) {
                                        new DchaServiceUtil(context).uninstallApp("a.a", 0, object2 -> {
                                            if (object2.equals(true)) {
                                                new DchaServiceUtil(context).setSetupStatus(0, object3 -> {
                                                    if (object3.equals(true)) {
                                                        new DialogUtil(context)
                                                                .setTitle("処理完了")
                                                                .setMessage("権限設定は完了しました。もう一度操作すれば、設定を変更できます。")
                                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                                .show();
                                                    } else {
                                                        new DialogUtil(context)
                                                                .setMessage(R.string.dialog_error)
                                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                                .show();
                                                    }
                                                });
                                            } else {
                                                new DialogUtil(context)
                                                        .setMessage(R.string.dialog_error)
                                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                                        .show();
                                            }
                                        });
                                    } else {
                                        new DialogUtil(context)
                                                .setMessage(R.string.dialog_error)
                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                .show();
                                    }
                                });
                            } else {
                                new DialogUtil(context)
                                        .setMessage(R.string.dialog_error)
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            }
                        });
                    } else {
                        new DialogUtil(context)
                                .setMessage(R.string.dialog_error)
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                    }
                })
                .show();
    }
}
