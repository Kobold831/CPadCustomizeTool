package com.saradabar.cpadcustomizetool.view.activity;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.view.flagment.WelScrollFragment;
import com.stephentuso.welcome.BasicPage;
import com.stephentuso.welcome.FragmentWelcomePage;
import com.stephentuso.welcome.TitlePage;
import com.stephentuso.welcome.WelcomeActivity;
import com.stephentuso.welcome.WelcomeConfiguration;

public class WelAppActivity extends WelcomeActivity {
    @SuppressLint("ResourceAsColor")
    @Override
    protected WelcomeConfiguration configuration() {
        return new WelcomeConfiguration.Builder(this)
                .bottomLayout(WelcomeConfiguration.BottomLayout.INDICATOR_ONLY)
                .defaultBackgroundColor(R.color.white)
                .page(new TitlePage(R.drawable.cpadmaterial, getString(R.string.wel_title_page_1)).titleColor(R.color.black))
                .page(new BasicPage(R.drawable.navigationbar, getString(R.string.wel_title_page_2), getString(R.string.wel_description_page_2)).descriptionColor(R.color.black).headerColor(R.color.black))
                .page(new BasicPage(R.drawable.ex, getString(R.string.wel_title_page_3), getString(R.string.wel_description_page_3)).descriptionColor(R.color.black).headerColor(R.color.black))
                .page(new FragmentWelcomePage() {
                    @Override
                    protected Fragment fragment() {
                        return new WelScrollFragment();
                    }
                })
                .swipeToDismiss(false)
                .build();
    }

    @Override
    public void onBackPressed() {
    }
}