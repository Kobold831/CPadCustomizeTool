package com.saradabar.cpadcustomizetool.view.views;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.card.MaterialCardView;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.List;

public class UninstallBlockAppListView {

    public static class AppData {
        public String label;
        public String packName;
        public Drawable icon;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        private OnClickListener listener;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_uninstall_item);
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
                LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = mInflater.inflate(R.layout.view_uninstall_item, parent, false);
                holder.cardView = convertView.findViewById(R.id.view_uninstall_item_card);
                holder.textLabel = convertView.findViewById(R.id.un_label);
                holder.imageIcon = convertView.findViewById(R.id.un_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AppData data = getItem(position);
            DevicePolicyManager dpm = Common.getDevicePolicyManager(getContext());

            if (data != null) {
                holder.cardView.setOnClickListener(view -> listener.onClick(view, position));
                holder.textLabel.setText(data.label);
                holder.imageIcon.setImageDrawable(data.icon);
                ((SwitchCompat) convertView.findViewById(R.id.un_switch)).setChecked(dpm.isUninstallBlocked(Common.getDeviceAdminComponent(getContext()), data.packName));
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        MaterialCardView cardView;
        AppCompatTextView textLabel;
        AppCompatImageView imageIcon;
    }
}
