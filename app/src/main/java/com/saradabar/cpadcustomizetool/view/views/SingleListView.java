package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            SingleListView.ViewHolder holder = new SingleListView.ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_update_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.update_label);
                convertView.setTag(holder);
            } else {
                holder = (SingleListView.ViewHolder) convertView.getTag();
            }

            final SingleListView.AppData data = getItem(position);

            holder.textLabel.setText(data.label);

            /* RadioButtonの更新 */
            RadioButton button = convertView.findViewById(R.id.update_button);
            button.setChecked(isUpdater(data.updateMode));

            return convertView;
        }

        /* ランチャーに設定されているかの確認 */
        private boolean isUpdater(int i) {
            try {
                return Objects.equals(i, Preferences.GET_UPDATE_MODE(getContext()));
            } catch (NullPointerException ignored) {
                return false;
            }
        }
    }

    private static class ViewHolder {
        TextView textLabel;
    }
}