package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.saradabar.cpadcustomizetool.R;

import java.util.List;

public class CrashLogListView {

    public static class AppData {
        public String strMessage;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        public View view;
        LayoutInflater mInflater;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_crash_log_list_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_crash_log_list_item, parent, false);
                holder.textMessage = convertView.findViewById(R.id.view_crash_log_list_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            view = convertView;
            final AppData data = getItem(position);

            if (data != null) {
                holder.textMessage.setText(data.strMessage);
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        AppCompatTextView textMessage;
    }
}
