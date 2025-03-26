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

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

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
import java.util.Objects;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class StartActivity extends AppCompatActivity implements DownloadEventListener {

    Menu menu;

    public aaListener aa;

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
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
            //noinspection deprecation
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
                .commitAllowingStateLoss();

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
                            .setMessage("新しいバージョンが利用可能です。")
                            .setPositiveButton(getString(R.string.dialog_common_ok), (dialog, which) -> startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                            .setNegativeButton(getString(R.string.dialog_common_cancel), null)
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            dialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> {
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
                        alertDialog.setMessage(i + "秒で初期設定に戻ります。");
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
                        .setMessage(getString(R.string.dialog_error))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                new AlertDialog.Builder(StartActivity.this)
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    /* 解像度のリセット */
    public void resetResolution() {
        if (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
            try {
                //noinspection ResultOfMethodCallIgnored
                BenesseExtension.putInt(Constants.BC_COMPATSCREEN, 0);
            } catch (Exception ignored) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.dialog_error))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        } else {
            bindService(Constants.DCHA_UTIL_SERVICE, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    IDchaUtilService iDchaUtilService = IDchaUtilService.Stub.asInterface(iBinder);
                    if (!new DchaUtilServiceUtil(iDchaUtilService).setForcedDisplaySize(1280, 800)) {
                        new AlertDialog.Builder(StartActivity.this)
                                .setMessage(R.string.dialog_error)
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // DeviceOwnerFragmentの画面でタスクキルされたときに実行
        if (aa != null) {
            aa.onA();
        }
    }

    public interface aaListener {
        void onA();
    }
}
