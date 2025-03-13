package com.saradabar.cpadcustomizetool.view.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.saradabar.cpadcustomizetool.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

public class DeviceInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        AppCompatButton button1 = findViewById(R.id.act_device_info_button_1);
        AppCompatButton button2 = findViewById(R.id.act_device_info_button_2);
        AppCompatButton button3 = findViewById(R.id.act_device_info_button_3);

        button1.setOnClickListener(view -> {
            ArrayList<String> stringArrayList = new ArrayList<>();
            stringArrayList.add(getSystemProperties());
            ListView listView = findViewById(R.id.act_device_info_list);
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stringArrayList));
            listView.invalidateViews();
        });

        button2.setOnClickListener(view -> {
            ArrayList<String> stringArrayList = new ArrayList<>();
            stringArrayList.add(exec());
            ListView listView = findViewById(R.id.act_device_info_list);
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stringArrayList));
            listView.invalidateViews();
        });

        button3.setOnClickListener(view -> {
            ArrayList<String> stringArrayList = new ArrayList<>();
            stringArrayList.add(getBatteryInfo());
            ListView listView = findViewById(R.id.act_device_info_list);
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stringArrayList));
            listView.invalidateViews();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private String getSystemProperties() {
        StringBuilder stringBuilder = new StringBuilder();
        Properties properties = System.getProperties();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String nextElement = propertyNames.nextElement().toString();
            if (properties.getProperty(nextElement).equals(System.lineSeparator())) {
                stringBuilder.append(nextElement).append("=").append(properties.getProperty(nextElement).replace(System.lineSeparator(), "")).append(System.lineSeparator());
            } else {
                stringBuilder.append(nextElement).append("=").append(properties.getProperty(nextElement)).append(System.lineSeparator());
            }
        }
        return stringBuilder.toString().trim();
    }

    @NonNull
    private String exec() {
        Process process;
        BufferedWriter bufferedWriter;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            process = Runtime.getRuntime().exec("getprop" + System.lineSeparator());
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            bufferedWriter.write("exit" + System.lineSeparator());
            bufferedWriter.flush();
            process.waitFor();

            String data;
            while ((data = bufferedReader.readLine()) != null)
                stringBuilder.append(data).append(System.lineSeparator());
            bufferedReader.close();
            bufferedWriter.close();
            process.destroy();
        } catch (Exception ignored) {
        }
        return stringBuilder.toString().trim();
    }

    @NonNull
    private String getBatteryInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryManager batteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);

        if (batteryStatus == null || batteryManager == null) {
            return "";
        }

        stringBuilder.append("BatteryLevel=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).append(System.lineSeparator())
                .append("BatteryScale=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).append(System.lineSeparator())
                .append("BatteryHealth=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)).append(System.lineSeparator())
                .append("BatteryTechnology=").append(batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)).append(System.lineSeparator())
                .append("BatteryVoltage=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)).append(System.lineSeparator())
                .append("BatteryTemperature=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)).append(System.lineSeparator())
                .append("BatteryStatus=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).append(System.lineSeparator())
                .append("BatteryPlugged=").append(batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)).append(System.lineSeparator())
                .append("BatteryPresent=").append(batteryStatus.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)).append(System.lineSeparator())
                .append("BatteryCapacity=").append(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).append(System.lineSeparator())
                .append("BatteryChargeCounter=").append(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)).append(System.lineSeparator())
                .append("BatteryCurrentAverage=").append(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)).append(System.lineSeparator())
                .append("BatteryEnergyCounter=").append(batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)).append(System.lineSeparator())
                .append("BatteryCurrentNow=").append(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)).append(System.lineSeparator());

        return stringBuilder.toString().trim();
    }
}
