package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;

import java.util.List;

public class LaunchAppView {

    public static class AppData {
        public Drawable icon;
        public String label;
        public String packName;
    }

    public static class LaunchAppAdapter extends ArrayAdapter<LaunchAppView.AppData> {

        private final LayoutInflater mInflater;

        public LaunchAppAdapter(Context context, List<LaunchAppView.AppData> dataList) {
            super(context, R.layout.view_launch_app_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            LaunchAppView.ViewHolder holder = new LaunchAppView.ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_launch_app_item, parent, false);
                holder.imageIcon = convertView.findViewById(R.id.launch_app_image);
                holder.textLabel = convertView.findViewById(R.id.launch_app_text_label);
                holder.textPack = convertView.findViewById(R.id.launch_app_text_pack);
                convertView.setTag(holder);
            } else {
                holder = (LaunchAppView.ViewHolder) convertView.getTag();
            }

            final LaunchAppView.AppData data = getItem(position);

            if (data != null) {
                holder.imageIcon.setImageDrawable(data.icon);
                holder.textLabel.setText(data.label);
                holder.textPack.setText(data.packName);
            }

            return convertView;
        }
    }

    public static class ViewHolder {
        ImageView imageIcon;
        TextView textLabel;
        TextView textPack;
    }
}
