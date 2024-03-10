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
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.util.List;
import java.util.Objects;

public class SingleListView {

    public static class AppData {
        public String label;
        public int updateMode;
    }

    public static class AppListAdapter extends ArrayAdapter<SingleListView.AppData> {

        private final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<SingleListView.AppData> dataList) {
            super(context, R.layout.view_update_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            SingleListView.ViewHolder holder = new SingleListView.ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_update_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.update_label);
                convertView.setTag(holder);
            } else {
                holder = (SingleListView.ViewHolder) convertView.getTag();
            }

            final SingleListView.AppData data = getItem(position);

            if (data != null) {
                holder.textLabel.setText(data.label);

                /* RadioButtonの更新 */
                RadioButton button = convertView.findViewById(R.id.update_button);
                button.setChecked(isUpdater(data.updateMode));
            }

            return convertView;
        }

        /* ランチャーに設定されているかの確認 */
        private boolean isUpdater(int i) {
            try {
                return Objects.equals(i, Preferences.load(getContext(), Constants.KEY_FLAG_UPDATE_MODE, 1));
            } catch (NullPointerException ignored) {
                return false;
            }
        }
    }

    private static class ViewHolder {
        TextView textLabel;
    }
}