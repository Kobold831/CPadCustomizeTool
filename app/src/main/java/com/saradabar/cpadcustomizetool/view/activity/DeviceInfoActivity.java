package com.saradabar.cpadcustomizetool.view.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DeviceInfoActivity extends AppCompatActivity {

    public DeviceInfoActivity() {
        super(R.layout.activity_device_info);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getSupportActionBar().setTitle(R.string.activity_device_info);
        }
        init();

        AppCompatButton button1 = findViewById(R.id.act_device_info_button_1);
        AppCompatButton button2 = findViewById(R.id.act_device_info_button_2);
        button1.setOnClickListener(view -> {
            finish();
            overridePendingTransition(0, 0);
        });
        button2.setOnClickListener(view -> init());
    }

    private void init() {
        ListView listView = findViewById(R.id.act_device_info_list);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(Arrays.asList(
                getString(R.string.device_info_system_property),
                getString(R.string.device_info_system),
                getString(R.string.device_info_battery),
                getString(R.string.device_info_shared_pref)))));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    showProperties(listView, getSystemProperties());
                    break;
                case 1:
                    AppExecutors.io(() -> {
                        ArrayList<String> properties = Common.exec("getprop");
                        AppExecutors.main(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            showProperties(listView, properties);
                        });
                    });
                    break;
                case 2:
                    showProperties(listView, getBatteryInfo());
                    break;
                case 3:
                    showProperties(listView, getSharedPreferencesList());
                    break;
            }
        });
    }

    private void showProperties(@NonNull ListView listView, @NonNull ArrayList<String> properties) {
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, properties));
        listView.setOnItemClickListener(null);
        listView.invalidateViews();
    }

    @NonNull
    private ArrayList<String> getSharedPreferencesList() {
        ArrayList<String> prefsList = new ArrayList<>();
        File[] files = new File(getApplicationInfo().dataDir, "shared_prefs").listFiles();

        if (files == null) {
            return prefsList;
        }

        Arrays.sort(files);

        for (File f : files) {
            String fileName = f.getName();

            if (!fileName.endsWith(".xml")) continue;
            if (fileName.equals("WebViewChromiumPrefs.xml") || fileName.equals("AwOriginVisitLoggerPrefs.xml"))
                continue;

            SharedPreferences prefs = getSharedPreferences(fileName.substring(0, fileName.length() - 4), Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
                if (Constants.KEY_LIST_CRASH_LOG.equals(e.getKey())) continue;
                prefsList.add(e.getKey() + "  =  " + e.getValue());
            }
        }
        return prefsList;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private ArrayList<String> getSystemProperties() {
        ArrayList<String> stringArrayList = new ArrayList<>();
        Properties properties = System.getProperties();
        Enumeration<?> propertyNames = properties.propertyNames();

        while (propertyNames.hasMoreElements()) {
            String nextElement = propertyNames.nextElement().toString();

            if (properties.getProperty(nextElement).equals(System.lineSeparator())) {
                stringArrayList.add(nextElement + " = " + properties.getProperty(nextElement).replace(System.lineSeparator(), ""));
            } else {
                stringArrayList.add(nextElement + " = " + properties.getProperty(nextElement));
            }
        }
        return stringArrayList;
    }

    @NonNull
    private ArrayList<String> getBatteryInfo() {
        ArrayList<String> stringArrayList = new ArrayList<>();
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);

        if (batteryStatus == null || batteryManager == null) {
            return new ArrayList<>();
        }
        stringArrayList.add("BatteryLevel = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
        stringArrayList.add("BatteryScale = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
        stringArrayList.add("BatteryHealth = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));
        stringArrayList.add("BatteryTechnology = " + batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
        stringArrayList.add("BatteryVoltage = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));
        stringArrayList.add("BatteryTemperature = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));
        stringArrayList.add("BatteryStatus = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
        stringArrayList.add("BatteryPlugged = " + batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1));
        stringArrayList.add("BatteryPresent = " + batteryStatus.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false));
        stringArrayList.add("BatteryCapacity = " + batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        stringArrayList.add("BatteryChargeCounter = " + batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
        stringArrayList.add("BatteryCurrentAverage = " + batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
        stringArrayList.add("BatteryEnergyCounter = " + batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER));
        stringArrayList.add("BatteryCurrentNow = " + batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
        return stringArrayList;
    }
}
