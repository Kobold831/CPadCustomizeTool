/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.view.activity;

import static com.saradabar.cpadcustomizetool.util.Common.parseJson;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.Display;
import android.view.IWindowManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.data.task.ResolutionTask;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.flagment.AppSettingsFragment;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class StartActivity extends AppCompatActivity implements DownloadEventListener {

    Menu menu;

    /* 設定画面表示 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        if (supportModelCheck()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                FrameLayout frameLayout = findViewById(R.id.layout_main);
                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                switch (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
                    case Constants.MODEL_CT2:
                    case Constants.MODEL_CT3:
                        marginLayoutParams.setMargins(64, 0, 64, 0);
                        break;
                    case Constants.MODEL_CTX:
                    case Constants.MODEL_CTZ:
                        marginLayoutParams.setMargins(72, 0, 72, 0);
                        break;
                }
                frameLayout.setLayoutParams(marginLayoutParams);
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                FrameLayout frameLayout = findViewById(R.id.layout_main);
                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                switch (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
                    case Constants.MODEL_CT2:
                    case Constants.MODEL_CT3:
                        marginLayoutParams.setMargins(112, 0, 112, 0);
                        break;
                    case Constants.MODEL_CTX:
                    case Constants.MODEL_CTZ:
                        marginLayoutParams.setMargins(144, 0, 144, 0);
                        break;
                }
                frameLayout.setLayoutParams(marginLayoutParams);
            }
        }

        transitionFragment(new MainFragment(), false);

        /* アップデートチェックするか確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_APP_START_UPDATE_CHECK, true)) {
            new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), "Check.json"), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
        }
    }

    /* メニュー表示 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* メニュー選択 */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_info_1) {
            startActivity(new Intent(this, AppInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return true;
        }

        if (item.getItemId() == R.id.app_info_2) {
            startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return true;
        }

        if (item.getItemId() == R.id.app_info_3) {
            menu.findItem(R.id.app_info_3).setVisible(false);
            transitionFragment(new AppSettingsFragment(), true);
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            menu.findItem(R.id.app_info_3).setVisible(true);
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            transitionFragment(new MainFragment(), false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 戻るボタン */
    @SuppressWarnings({"deprecation", "MissingSuperCall"})
    @Override
    public void onBackPressed() {
        menu.findItem(R.id.app_info_3).setVisible(true);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        transitionFragment(new MainFragment(), false);
    }

    public void transitionFragment(PreferenceFragmentCompat preferenceFragmentCompat, boolean showHomeAsUp) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_main, preferenceFragmentCompat)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showHomeAsUp);
        }
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        /* DchaServiceが機能していな場合は再起動 */
        if (!Preferences.load(this, "debug_restriction", false)) {
            if (Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
                if (!bindService(Constants.DCHA_SERVICE, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                    }
                }, Context.BIND_AUTO_CREATE)) {
                    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    finish();
                }
            }
        }
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        /* アップデートチェック要求の場合 */
        if (reqCode == Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            try {
                JSONObject jsonObj1 = parseJson(new File(getExternalCacheDir(), "Check.json"));
                JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                    new AlertDialog.Builder(this)
                            .setMessage("新しいバージョンが利用可能です")
                            .setPositiveButton("更新", (dialog, which) -> startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                            .setNegativeButton("キャンセル", null)
                            .show();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
    }

    @Override
    public void onConnectionError(int reqCode) {
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (supportModelCheck()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                FrameLayout frameLayout = findViewById(R.id.layout_main);
                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                switch (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
                    case Constants.MODEL_CT2:
                    case Constants.MODEL_CT3:
                        marginLayoutParams.setMargins(64, 0, 64, 0);
                        break;
                    case Constants.MODEL_CTX:
                    case Constants.MODEL_CTZ:
                        marginLayoutParams.setMargins(72, 0, 72, 0);
                        break;
                }
                frameLayout.setLayoutParams(marginLayoutParams);
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                FrameLayout frameLayout = findViewById(R.id.layout_main);
                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                switch (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
                    case Constants.MODEL_CT2:
                    case Constants.MODEL_CT3:
                        marginLayoutParams.setMargins(112, 0, 112, 0);
                        break;
                    case Constants.MODEL_CTX:
                    case Constants.MODEL_CTZ:
                        marginLayoutParams.setMargins(144, 0, 144, 0);
                        break;
                }
                frameLayout.setLayoutParams(marginLayoutParams);
            }
        }
    }

    private boolean supportModelCheck() {
        for (String string : Constants.LIST_MODEL) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
        return false;
    }

    public ResolutionTask.Listener resolutionTaskListener() {
        return new ResolutionTask.Listener() {

            Handler mHandler;
            Runnable mRunnable;

            /* 成功 */
            @Override
            public void onSuccess() {
                /* 設定変更カウントダウンダイアログ表示 */
                AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this)
                        .setTitle("解像度の変更を適用しますか？")
                        .setCancelable(false)
                        .setMessage("")
                        .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                            dialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                        })
                        .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                            dialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                            resetResolution();
                        })
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }

                /* カウント開始 */
                mHandler = new Handler();
                mRunnable = new Runnable() {

                    int i = 10;

                    @Override
                    public void run() {
                        alertDialog.setMessage(i + "秒で初期設定に戻ります");
                        mHandler.postDelayed(this, 1000);

                        if (i <= 0) {
                            alertDialog.dismiss();
                            mHandler.removeCallbacks(this);
                            resetResolution();
                        }

                        i--;
                    }
                };

                mHandler.post(mRunnable);
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    /* 解像度のリセット */
    public void resetResolution() {
        int width = 0, height = 0;

        switch (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
            case Constants.MODEL_CT2, Constants.MODEL_CT3 -> {
                width = 1280;
                height = 800;
            }
            case Constants.MODEL_CTX, Constants.MODEL_CTZ -> {
                width = 1920;
                height = 1200;
            }
        }

        if (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
            try {
                String method = "setForcedDisplaySize";
                Class.forName("android.view.IWindowManager").getMethod(method, int.class, int.class, int.class).invoke(IWindowManager.Stub.asInterface(ServiceManager.getService("window")), Display.DEFAULT_DISPLAY, width, height);
            } catch (InvocationTargetException e) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + e.getTargetException())
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            } catch (Exception ignored) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.dialog_error))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        } else {
            if (!new DchaUtilServiceUtil(this, null).setForcedDisplaySize(width, height)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_error)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        }
    }
}
