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

package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.util.List;

public class GetAppListView {

    public static class AppData {
        public String name;
        public String description;
        public String url;
    }

    public static class AppListAdapter extends ArrayAdapter<GetAppListView.AppData> {

        private final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<GetAppListView.AppData> dataList) {
            super(context, R.layout.view_app_list_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            GetAppListView.ViewHolder holder = new GetAppListView.ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_app_list_item, parent, false);
                holder.tv = convertView.findViewById(R.id.v_app_list_text);
                convertView.setTag(holder);
            } else {
                holder = (GetAppListView.ViewHolder) convertView.getTag();
            }

            final GetAppListView.AppData data = getItem(position);

            if (data != null) {
                holder.tv.setText(data.name);
            }

            /* RadioButtonの更新 */
            AppCompatRadioButton button = convertView.findViewById(R.id.v_app_list_radio);
            button.setChecked(Preferences.load(getContext(), Constants.KEY_INT_GET_APP_TMP, 0) == position);

            return convertView;
        }
    }

    public static class ViewHolder {
        AppCompatTextView tv;
    }
}
