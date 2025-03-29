package com.saradabar.cpadcustomizetool.view.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rosan.dhizuku.shared.DhizukuVariables;

public class ForceCrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).killBackgroundProcesses(DhizukuVariables.OFFICIAL_PACKAGE_NAME);
        startActivity(new Intent("ACTION_NAME").setPackage("PACKAGE_NAME"));
    }
}
