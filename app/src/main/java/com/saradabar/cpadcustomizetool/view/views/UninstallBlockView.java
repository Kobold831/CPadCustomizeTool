package com.saradabar.cpadcustomizetool.view.views;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.List;

public class UninstallBlockView {

    public static class AppData {
        public String label;
        public String packName;
        public Drawable icon;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        public View view;
        LayoutInflater mInflater;
        DevicePolicyManager dpm;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_uninstall_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
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
                    if (tryBindDhizukuService(getContext())) {
                        try {
                            ((Switch) convertView.findViewById(R.id.un_switch)).setChecked(((MyApplication) getContext().getApplicationContext()).mDhizukuService.isUninstallBlocked(data.packName));
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    ((Switch) convertView.findViewById(R.id.un_switch)).setChecked(dpm.isUninstallBlocked(new ComponentName(getContext(), AdministratorReceiver.class), data.packName));
                }
            }
            return convertView;
        }

        private boolean tryBindDhizukuService(Context context) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
            return Dhizuku.bindUserService(args, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder iBinder) {
                    ((MyApplication) context.getApplicationContext()).mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            });
        }
    }

    private static class ViewHolder {
        TextView textLabel;
        ImageView imageIcon;
    }
}
