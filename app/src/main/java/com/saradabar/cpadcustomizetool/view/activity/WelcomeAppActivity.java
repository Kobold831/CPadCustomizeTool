/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.view.activity;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.view.flagment.WelcomeAppFragment;
import com.saradabar.cpadcustomizetool.view.flagment.WelcomeAppPageFragment;

public class WelcomeAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_app);

        ViewPager2 viewPager = findViewById(R.id.wel_pager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return WelcomeAppPageFragment.newInstance(R.drawable.ic_launcher_foreground, R.string.wel_title_page_1, 0);
                    case 1:
                        return WelcomeAppPageFragment.newInstance(R.drawable.emergency_home_24px, R.string.wel_title_page_2, R.string.wel_description_page_2);
                    default:
                        return new WelcomeAppFragment();
                }
            }

            @Override
            public int getItemCount() {
                return Constants.WELCOME_PAGE_COUNT;
            }
        });

        LinearLayout indicator = findViewById(R.id.wel_indicator);

        for (int i = 0; i < Constants.WELCOME_PAGE_COUNT; i++) {
            indicator.addView(createIndicatorDot(i == 0));
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < indicator.getChildCount(); i++) {
                    indicator.getChildAt(i).setAlpha(i == position ? 1f : 0.3f);
                }
            }
        });
    }

    @NonNull
    private View createIndicatorDot(boolean selected) {
        int size = (int) (Constants.WELCOME_INDICATOR_SIZE_DP * getResources().getDisplayMetrics().density);
        View dot = new View(this);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ContextCompat.getColor(this, R.color.textColor));
        dot.setBackground(background);
        dot.setAlpha(selected ? 1f : 0.3f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(size / 2);
        params.setMarginEnd(size / 2);
        dot.setLayoutParams(params);
        return dot;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
