package com.saradabar.cpadcustomizetool.view.flagment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.app.Activity;
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
                .setTitle(R.string.dialog_title_bypass_permission)
                .setMessage(R.string.dialog_bypass_permission_message)
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
                                                        showResultDialog(getString(R.string.dialog_title_process_complete), R.string.dialog_bypass_permission_complete);
                                                    } else {
                                                        showResultDialog(null, R.string.dialog_error);
                                                    }
                                                });
                                            } else {
                                                showResultDialog(null, R.string.dialog_error);
                                            }
                                        });
                                    } else {
                                        showResultDialog(null, R.string.dialog_error);
                                    }
                                });
                            } else {
                                showResultDialog(null, R.string.dialog_error);
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

    private void showResultDialog(String title, CharSequence message) {
        Activity activity = getActivity();
        if (!isAdded() || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        DialogUtil dialog = new DialogUtil(activity);
        dialog.setMessage(message)
                .setPositiveButton(R.string.dialog_common_ok, null);
        if (title != null) {
            dialog.setTitle(title);
        }
        dialog.show();
    }

    private void showResultDialog(String title, int messageResId) {
        Activity activity = getActivity();
        if (activity == null) return;
        showResultDialog(title, activity.getString(messageResId));
    }
}
