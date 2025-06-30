package com.saradabar.cpadcustomizetool.view.flagment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class SimpleFunctionFragment extends Fragment {

    public SimpleFunctionFragment() {
        super(R.layout.layout_simple_function);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ((MainActivity) requireActivity()).initNavigationState();

        MaterialCardView materialCardView1 = view.findViewById(R.id.lo_simple_function_card_1);
        MaterialCardView materialCardView2 = view.findViewById(R.id.lo_simple_function_card_2);
        MaterialCardView materialCardView3 = view.findViewById(R.id.lo_simple_function_card_3);
        MaterialCardView materialCardView4 = view.findViewById(R.id.lo_simple_function_card_4);

        materialCardView1.setOnClickListener(v -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return;
            }
            new DchaServiceUtil(requireActivity()).setSetupStatus(0, object -> {
            });
        });
        materialCardView2.setOnClickListener(v -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return;
            }
            new DchaServiceUtil(requireActivity()).setSetupStatus(3, object -> {
            });
        });
        materialCardView3.setOnClickListener(v -> new DchaServiceUtil(requireActivity()).hideNavigationBar(false, object -> {
        }));
        materialCardView4.setOnClickListener(v -> new DchaServiceUtil(requireActivity()).hideNavigationBar(true, object -> {
        }));
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
}
