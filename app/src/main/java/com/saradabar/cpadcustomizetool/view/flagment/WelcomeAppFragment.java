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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.saradabar.cpadcustomizetool.view.activity.CheckActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.stephentuso.welcome.WelcomePage;

public class WelcomeAppFragment extends Fragment implements WelcomePage.OnChangeListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_welcome, container, false);
        AppCompatTextView textView = view.findViewById(R.id.wel_scroll);

        textView.setText(getString(R.string.wel_terms_of_service));
        view.findViewById(R.id.wel_no).setOnClickListener(v -> requireActivity().finishAffinity());
        view.findViewById(R.id.wel_yes).setOnClickListener(v -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_APP_WELCOME_COMPLETE, true);
            startActivity(new Intent(requireActivity(), CheckActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            requireActivity().overridePendingTransition(0, 0);
            requireActivity().finish();
        });
        return view;
    }

    @Override
    public void onWelcomeScreenPageScrolled(int pageIndex, float offset, int offsetPixels) {
    }

    @Override
    public void onWelcomeScreenPageSelected(int pageIndex, int selectedPageIndex) {
    }

    @Override
    public void onWelcomeScreenPageScrollStateChanged(int pageIndex, int state) {
    }
}