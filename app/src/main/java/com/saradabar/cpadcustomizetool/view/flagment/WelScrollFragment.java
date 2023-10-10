package com.saradabar.cpadcustomizetool.view.flagment;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.stephentuso.welcome.WelcomePage;

import java.util.regex.Pattern;

public class WelScrollFragment extends Fragment implements WelcomePage.OnChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_welcome, container, false);
        TextView textView = view.findViewById(R.id.wel_scroll);
        textView.setText(getString(R.string.wel_terms_of_service, Constants.URL_WIKI_MAIN));
        Linkify.addLinks(textView, Pattern.compile(Constants.URL_WIKI_MAIN), "");
        view.findViewById(R.id.wel_no).setOnClickListener(v -> requireActivity().finishAffinity());
        view.findViewById(R.id.wel_yes).setOnClickListener(v -> requireActivity().finish());
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