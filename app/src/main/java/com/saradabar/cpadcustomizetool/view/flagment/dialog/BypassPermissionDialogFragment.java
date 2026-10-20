package com.saradabar.cpadcustomizetool.view.flagment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.DialogUtil;

public class BypassPermissionDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireActivity();
        return new DialogUtil(context)
                .setTitle(R.string.dialog_title_bypass_permission)
                .setMessage(R.string.dialog_bypass_permission_message)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                        Common.acquireBypassPermission(context, success -> {
                            if (success) {
                                showResultDialog(context, context.getString(R.string.dialog_title_process_complete), R.string.dialog_bypass_permission_complete);
                            } else {
                                showResultDialog(context, null, R.string.dialog_error);
                            }
                        }))
                .show();
    }

    private void showResultDialog(Context context, String title, CharSequence message) {
        if (!(context instanceof Activity activity)) {
            return;
        }

        if (activity.isFinishing() || activity.isDestroyed()) {
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

    private void showResultDialog(Context context, String title, int messageResId) {
        showResultDialog(context, title, context.getString(messageResId));
    }
}
