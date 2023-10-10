package com.saradabar.cpadcustomizetool.view.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;

import java.util.List;
import java.util.Objects;

public class LauncherView {

    public static class AppData {
        public String label;
        public Drawable icon;
        public String packName;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        private final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_launcher_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_launcher_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.launcher_text);
                holder.imageIcon = convertView.findViewById(R.id.launcher_image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final AppData data = getItem(position);

            holder.textLabel.setText(data.label);
            holder.imageIcon.setImageDrawable(data.icon);

            /* RadioButtonの更新 */
            RadioButton button = convertView.findViewById(R.id.launcher_button);
            button.setChecked(isLauncher(data.packName));

            return convertView;
        }

        /* ランチャーに設定されているかの確認 */
        private boolean isLauncher(String s) {
            return Objects.equals(s, getContext().getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0).activityInfo.packageName);
        }
    }

    public static class ViewHolder {
        TextView textLabel;
        ImageView imageIcon;
    }
}