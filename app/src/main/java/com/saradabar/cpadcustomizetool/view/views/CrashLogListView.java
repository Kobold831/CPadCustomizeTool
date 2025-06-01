package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.card.MaterialCardView;
import com.saradabar.cpadcustomizetool.R;

import java.util.List;

public class CrashLogListView {

    public static class AppData {
        public String strMessage;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        private OnClickListener listener;
        final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_crash_log_list_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        public interface OnClickListener {
            void onClick(View view, int position);
        }

        public void setOnItemClickListener(OnClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_crash_log_list_item, parent, false);
                holder.cardView = convertView.findViewById(R.id.view_crash_log_list_item_card);
                holder.textMessage = convertView.findViewById(R.id.view_crash_log_list_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AppData data = getItem(position);

            if (data != null) {
                holder.cardView.setOnClickListener(view -> listener.onClick(view, position));
                holder.textMessage.setText(data.strMessage);
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        MaterialCardView cardView;
        AppCompatTextView textMessage;
    }
}
