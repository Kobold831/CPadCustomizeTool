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

public class NoticeListView {

    public static class AppData {
        public String title;
        public String message;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_notice_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_notice_item, parent, false);
                holder.textTitle = convertView.findViewById(R.id.notice_text_title);
                holder.textMessage = convertView.findViewById(R.id.notice_text_message);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AppData data = getItem(position);

            if (data != null) {
                holder.textTitle.setText(data.title);
                holder.textMessage.setText(data.message);
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        AppCompatTextView textTitle;
        AppCompatTextView textMessage;
    }
}
