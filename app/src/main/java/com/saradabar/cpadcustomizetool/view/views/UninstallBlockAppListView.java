package com.saradabar.cpadcustomizetool.view.views;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
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

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.List;

public class UninstallBlockAppListView {

    public static class AppData {
        public String label;
        public String packName;
        public Drawable icon;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        public View view;
        LayoutInflater mInflater;
        DevicePolicyManager dpm;

        IDhizukuService mDhizukuService;

        public AppListAdapter(Context context, List<AppData> dataList, IDhizukuService iDhizukuService) {
            super(context, R.layout.view_uninstall_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDhizukuService = iDhizukuService;
            addAll(dataList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_uninstall_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.un_label);
                holder.imageIcon = convertView.findViewById(R.id.un_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            view = convertView;
            final AppData data = getItem(position);

            if (data != null) {
                holder.textLabel.setText(data.label);
                holder.imageIcon.setImageDrawable(data.icon);

                if (Common.isDhizukuActive(getContext())) {
                    try {
                        ((SwitchCompat) view.findViewById(R.id.un_switch)).setChecked(mDhizukuService.isUninstallBlocked(data.packName));
                    } catch (Exception ignored) {
                    }
                } else {
                    ((SwitchCompat) view.findViewById(R.id.un_switch)).setChecked(dpm.isUninstallBlocked(new ComponentName(getContext(), DeviceAdminReceiver.class), data.packName));
                }
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        AppCompatTextView textLabel;
        AppCompatImageView imageIcon;
    }
}
