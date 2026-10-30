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

package com.saradabar.cpadcustomizetool.view.flagment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.saradabar.cpadcustomizetool.R;

public class WelcomeAppPageFragment extends Fragment {

    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";

    @NonNull
    public static WelcomeAppPageFragment newInstance(int imageRes, int titleRes, int descriptionRes) {
        WelcomeAppPageFragment fragment = new WelcomeAppPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putInt(ARG_TITLE, titleRes);
        args.putInt(ARG_DESCRIPTION, descriptionRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome_page, container, false);
        Bundle args = requireArguments();

        AppCompatImageView imageView = view.findViewById(R.id.wel_page_image);
        int imageRes = args.getInt(ARG_IMAGE);

        if (imageRes != 0) {
            imageView.setImageResource(imageRes);
        } else {
            imageView.setVisibility(View.GONE);
        }

        ((AppCompatTextView) view.findViewById(R.id.wel_page_title)).setText(args.getInt(ARG_TITLE));

        AppCompatTextView descriptionView = view.findViewById(R.id.wel_page_description);
        int descriptionRes = args.getInt(ARG_DESCRIPTION);

        if (descriptionRes != 0) {
            descriptionView.setText(descriptionRes);
        } else {
            descriptionView.setVisibility(View.GONE);
        }
        return view;
    }
}
