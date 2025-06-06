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

package com.saradabar.cpadcustomizetool.view.activity;

import androidx.fragment.app.Fragment;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.view.flagment.WelcomeAppFragment;
import com.stephentuso.welcome.BasicPage;
import com.stephentuso.welcome.FragmentWelcomePage;
import com.stephentuso.welcome.TitlePage;
import com.stephentuso.welcome.WelcomeActivity;
import com.stephentuso.welcome.WelcomeConfiguration;

public class WelcomeAppActivity extends WelcomeActivity {

    @Override
    protected WelcomeConfiguration configuration() {
        return new WelcomeConfiguration.Builder(this)
                .bottomLayout(WelcomeConfiguration.BottomLayout.INDICATOR_ONLY)
                .defaultBackgroundColor(R.color.colorPrimary)
                .page(new TitlePage(R.drawable.cpad_grid_icon_v2, getString(R.string.wel_title_page_1)).titleColor(getResources().getColor(R.color.textColor)))
                .page(new BasicPage(R.drawable.navigationbar, getString(R.string.wel_title_page_2), getString(R.string.wel_description_page_2)).descriptionColor(getResources().getColor(R.color.textColor)).headerColor(getResources().getColor(R.color.textColor)))
                .page(new BasicPage(R.drawable.ex, getString(R.string.wel_title_page_3), getString(R.string.wel_description_page_3)).descriptionColor(getResources().getColor(R.color.textColor)).headerColor(getResources().getColor(R.color.textColor)))
                .page(new FragmentWelcomePage() {
                    @Override
                    protected Fragment fragment() {
                        return new WelcomeAppFragment();
                    }
                })
                .swipeToDismiss(false)
                .build();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}