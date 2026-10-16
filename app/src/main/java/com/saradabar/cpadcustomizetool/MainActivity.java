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

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.saradabar.cpadcustomizetool.view.flagment.AppFunctionFragment;
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
                transitionFragment(new SimpleFunctionFragment(), false, getString(R.string.pre_app_title_simple_mode));
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

    private boolean initNavigationListener(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.navi_0) {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof SimpleFunctionFragment)) {
                transitionFragment(new SimpleFunctionFragment(), false, getString(R.string.pre_app_title_simple_mode));
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
            transitionFragment(new DchaFunctionFragment(), true, getString(R.string.pre_main_title_dcha_function));
            return true;
        }

        if (item.getItemId() == R.id.navi_3) {
            transitionFragment(new AppSettingsFragment(), true, getString(R.string.menu_app_settings));
            return true;
        }

        if (item.getItemId() == R.id.navi_4) {
            if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
                new DialogUtil(this)
                        .setMessage(getString(R.string.pre_main_sum_check_enabled, getString(R.string.pre_main_title_use_dcha)))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
                return false;
            }
            transitionFragment(new AppFunctionFragment(), true, getString(R.string.pre_main_title_app_function_settings));
            return true;
        }
        return false;
    }

    public void initNavigationState() {
        Menu navigationMenu = getNavigationMenu();

        if (navigationMenu == null) {
            return;
        }

        boolean isNormalEnv = Preferences.load(this, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL);
        navigationMenu.findItem(R.id.navi_2).setVisible(!isNormalEnv);
        navigationMenu.findItem(R.id.navi_4).setVisible(!isNormalEnv);

        int itemId = getNavigationItemId(getCurrentFragment());
        MenuItem item = itemId == 0 ? null : navigationMenu.findItem(itemId);

        if (item != null) {
            item.setChecked(true);
        }
    }

    private Menu getNavigationMenu() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            return bottomNavigationView == null ? null : bottomNavigationView.getMenu();
        }

        NavigationRailView navigationRailView = findViewById(R.id.navigation_rail);
        return navigationRailView == null ? null : navigationRailView.getMenu();
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);
    }

    private int getNavigationItemId(Fragment fragment) {
        if (fragment instanceof SimpleFunctionFragment) {
            return R.id.navi_0;
        }
        if (fragment instanceof MainFragment) {
            return R.id.navi_1;
        }
        if (fragment instanceof DchaFunctionFragment) {
            return R.id.navi_2;
        }
        if (fragment instanceof AppFunctionFragment) {
            return R.id.navi_4;
        }
        if (fragment instanceof AppSettingsFragment) {
            return R.id.navi_3;
        }
        return 0;
    }

    /* メニュー表示 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.findItem(R.id.app_info_3).setVisible(!(getCurrentFragment() instanceof AppSettingsFragment));
        return super.onPrepareOptionsMenu(menu);
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
            // デバッグモードが無効かつ DchaService を使う設定かつ DchaService と通信できない
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
                mHandler = new Handler(Looper.getMainLooper());
                mRunnable = new Runnable() {
                    int i = 10;

                    @Override
                    public void run() {
                        alertDialog.setMessage(getString(R.string.dialog_resolution_countdown, i));
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
                marginLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ct2_portrait), 0, getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ct2_portrait), 0);
            } else if (Common.isCTX() || Common.isCTZ()) {
                marginLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ctx_portrait), 0, getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ctx_portrait), 0);
            }
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Common.isCT2() || Common.isCT3()) {
                marginLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ct2_landscape), 0, getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ct2_landscape), 0);
            } else if (Common.isCTX() || Common.isCTZ()) {
                marginLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ctx_landscape), 0, getResources().getDimensionPixelSize(R.dimen.main_frame_margin_ctx_landscape), 0);
            }
        }
        frameLayout.setLayoutParams(marginLayoutParams);
    }
}
