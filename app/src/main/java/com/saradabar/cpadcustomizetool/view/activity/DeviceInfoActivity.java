package com.saradabar.cpadcustomizetool.view.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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
            getSupportActionBar().setTitle("デバイス情報");
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
                "システムプロパティ情報",
                "システム情報",
                "バッテリー情報"))));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getSystemProperties()));
                    listView.setOnItemClickListener(null);
                    listView.invalidateViews();
                    break;
                case 1:
                    listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Common.exec("getprop")));
                    listView.setOnItemClickListener(null);
                    listView.invalidateViews();
                    break;
                case 2:
                    listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getBatteryInfo()));
                    listView.setOnItemClickListener(null);
                    listView.invalidateViews();
                    break;
            }
        });
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
