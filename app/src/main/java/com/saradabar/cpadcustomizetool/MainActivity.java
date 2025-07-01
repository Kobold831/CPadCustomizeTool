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

package com.saradabar.cpadcustomizetool;

import static com.saradabar.cpadcustomizetool.util.Common.parseJson;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.data.task.ResolutionTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.AppInfoActivity;
import com.saradabar.cpadcustomizetool.view.activity.CheckActivity;
import com.saradabar.cpadcustomizetool.view.activity.SelfUpdateActivity;
import com.saradabar.cpadcustomizetool.view.flagment.AppSettingsFragment;
import com.saradabar.cpadcustomizetool.view.flagment.DchaFunctionFragment;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;
import com.saradabar.cpadcustomizetool.view.flagment.SimpleFunctionFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/** @noinspection deprecation*/
public class MainActivity extends AppCompatActivity implements DownloadEventListener {

    Menu menu;

    public MainActivity() {
        super(R.layout.activity_main);
    }

    /* 設定画面表示 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (supportModelCheck()) {
            // サポート対象端末
            setLayoutParams();
        }

        if (savedInstanceState == null) {
            if (Preferences.load(this, Constants.KEY_FLAG_SIMPLE_MODE, Constants.DEF_BOOL)) {
                transitionFragment(new SimpleFunctionFragment(), false, "シンプルモード");
            } else {
                transitionFragment(new MainFragment(), false, null);
            }

            if (Preferences.load(this, Constants.KEY_FLAG_APP_START_UPDATE_CHECK, true)) {
                // アップデートチェックする設定
                new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), Constants.CHECK_JSON), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
            }
        }
        setNavigationView();
    }

    private void setNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationRailView navigationRailView = findViewById(R.id.navigation_rail);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            navigationRailView.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.setOnItemSelectedListener(this::initNavigationListener);
        } else {
            bottomNavigationView.setVisibility(View.GONE);
            navigationRailView.setVisibility(View.VISIBLE);
            navigationRailView.setOnItemSelectedListener(this::initNavigationListener);
        }
        initNavigationState();
    }

    private boolean initNavigationListener(MenuItem item) {
        if (item.getItemId() == R.id.navi_0) {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof SimpleFunctionFragment)) {
                transitionFragment(new SimpleFunctionFragment(), false, "シンプルモード");
            }
            return true;
        }

        if (item.getItemId() == R.id.navi_1) {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof MainFragment)) {
                transitionFragment(new MainFragment(), false, null);
            }
            return true;
        }

        if (item.getItemId() == R.id.navi_2) {
            if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
                new DialogUtil(this)
                        .setMessage(getString(R.string.pre_main_sum_check_enabled, getString(R.string.pre_main_title_use_dcha)))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
                return false;
            }
            transitionFragment(new DchaFunctionFragment(), true, "Dcha アプリの機能");
            return true;
        }

        if (item.getItemId() == R.id.navi_3) {
            transitionFragment(new AppSettingsFragment(), true, getString(R.string.menu_app_settings));
            return true;
        }
        return false;
    }

    public void initNavigationState() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

            if (Preferences.load(this, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
                bottomNavigationView.getMenu().getItem(2).setVisible(false);
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof SimpleFunctionFragment) {
                bottomNavigationView.getMenu().getItem(0).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof MainFragment) {
                bottomNavigationView.getMenu().getItem(1).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof DchaFunctionFragment) {
                bottomNavigationView.getMenu().getItem(2).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof AppSettingsFragment) {
                bottomNavigationView.getMenu().getItem(3).setChecked(true);
            }
        } else {
            NavigationRailView navigationRailView = findViewById(R.id.navigation_rail);

            if (Preferences.load(this, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
                navigationRailView.getMenu().getItem(2).setVisible(false);
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof SimpleFunctionFragment) {
                navigationRailView.getMenu().getItem(0).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof MainFragment) {
                navigationRailView.getMenu().getItem(1).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof DchaFunctionFragment) {
                navigationRailView.getMenu().getItem(2).setChecked(true);
                return;
            }

            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof AppSettingsFragment) {
                navigationRailView.getMenu().getItem(3).setChecked(true);
            }
        }
    }

    /* メニュー表示 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    /* メニュー選択 */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.app_info_1) {
            // アプリ情報
            startActivity(new Intent(this, AppInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return true;
        }

        if (item.getItemId() == R.id.app_info_2) {
            // アップデートの確認
            startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            return true;
        }

        if (item.getItemId() == R.id.app_info_3) {
            // アプリ設定
            menu.findItem(R.id.app_info_3).setVisible(false);
            transitionFragment(new AppSettingsFragment(), true, getString(R.string.menu_app_settings));
            return true;
        }

        if (item.getItemId() == android.R.id.home) {
            // 戻る
            menu.findItem(R.id.app_info_3).setVisible(true);
            //noinspection deprecation
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            transitionFragment(new MainFragment(), false, null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* 戻るボタン */
    @Override
    public void onBackPressed() {
        if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof MainFragment)) {
            menu.findItem(R.id.app_info_3).setVisible(true);
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            transitionFragment(new MainFragment(), false, null);
        }
    }

    public void transitionFragment(Fragment fragment, boolean showHomeAsUp, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .commitAllowingStateLoss();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showHomeAsUp);

            if (title == null || title.isEmpty()) {
                title = getString(R.string.activity_start);
            }
            getSupportActionBar().setTitle(title);
        }
    }

    /* 表示 */
    @Override
    public void onResume() {
        super.onResume();
        if (!Preferences.load(this, "debug_restriction", Constants.DEF_BOOL) &&
                Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL) &&
                !Common.isDchaActive(this)) {
            // デバッグモードが無効かつ DchaService を使う設定かつ　DchaService と通信できない
            Preferences.save(this, Constants.KEY_FLAG_DCHA_FUNCTION, false);
            startActivity(new Intent(this, CheckActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        }
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            // アップデートチェック要求
            try {
                JSONObject jsonObj1 = parseJson(new File(getExternalCacheDir(), Constants.CHECK_JSON));
                JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                    new DialogUtil(this)
                            .setMessage(R.string.dialog_new_version_available)
                            .setPositiveButton(getString(R.string.dialog_common_ok), (dialog, which) ->
                                    startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                            .setNegativeButton(getString(R.string.dialog_common_cancel), null)
                            .show();
                }
            } catch (JSONException | IOException ignored) {
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
            setLayoutParams();
        }
        setNavigationView();
    }

    private boolean supportModelCheck() {
        for (String model : Constants.LIST_MODEL) {
            if (Objects.equals(model, Build.MODEL)) {
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
                AlertDialog alertDialog = new DialogUtil(MainActivity.this)
                        .setTitle(R.string.dialog_apply_resolution)
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
                        alertDialog.setMessage(i + " 秒で初期設定に戻ります。");
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
                new DialogUtil(MainActivity.this)
                        .setMessage(getString(R.string.dialog_error))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                new DialogUtil(MainActivity.this)
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    /* 解像度のリセット */
    public void resetResolution() {
        if (Common.isCTX() || Common.isCTZ()) {
            // CTXとCTZ
            try {
                //noinspection ResultOfMethodCallIgnored
                BenesseExtension.putInt(Constants.BC_COMPATSCREEN, 0);
            } catch (Exception e) {
                new DialogUtil(this)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        } else {
            // CT2とCT3
            new DchaUtilServiceUtil(this).setForcedDisplaySize(1280, 800, object -> {
                if (object.equals(false)) {
                    new DialogUtil(MainActivity.this)
                            .setMessage(R.string.dialog_error)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
            });
        }
    }

    private void setLayoutParams() {
        FrameLayout frameLayout = findViewById(R.id.fragment_container_view);
        ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (Common.isCT2() || Common.isCT3()) {
                marginLayoutParams.setMargins(64, 0, 64, 0);
            } else if (Common.isCTX() || Common.isCTZ()) {
                marginLayoutParams.setMargins(72, 0, 72, 0);
            }
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Common.isCT2() || Common.isCT3()) {
                marginLayoutParams.setMargins(112, 0, 112, 0);
            } else if (Common.isCTX() || Common.isCTZ()) {
                marginLayoutParams.setMargins(144, 0, 144, 0);
            }
        }
        frameLayout.setLayoutParams(marginLayoutParams);
    }
}
