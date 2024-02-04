package com.saradabar.cpadcustomizetool.view.activity;

import static com.saradabar.cpadcustomizetool.util.Common.parseJson;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.flagment.AppSettingsFragment;
import com.saradabar.cpadcustomizetool.view.flagment.DeviceOwnerFragment;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;
import com.saradabar.cpadcustomizetool.view.views.AppListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class StartActivity extends AppCompatActivity implements InstallEventListener, DownloadEventListener {

    static StartActivity instance = null;
    IDchaService mDchaService;
    Menu menu;

    public static StartActivity getInstance() {
        return instance;
    }

    /* 設定画面表示 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setContentView(R.layout.activity_main);
        transitionFragment(new MainFragment(), false);

        /* アップデートチェックするか確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_UPDATE, true)) {
            new AsyncFileDownload(this, Constants.URL_CHECK, new File(new File(getExternalCacheDir(), "Check.json").getPath()), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK).execute();
        }
    }

    /* メニュー表示 */
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        menu = m;
        getMenuInflater().inflate(R.menu.main, m);
        return true;
    }

    /* メニュー選択 */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_info_1:
                startActivity(new Intent(this, AppInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                return true;
            case R.id.app_info_2:
                startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                return true;
            case R.id.app_info_3:
                menu.findItem(R.id.app_info_3).setVisible(false);
                nullTransitionFragment(new AppSettingsFragment());
                return true;
            case android.R.id.home:
                menu.findItem(R.id.app_info_3).setVisible(true);
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                transitionFragment(new MainFragment(), false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* 戻るボタン */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        menu.findItem(R.id.app_info_3).setVisible(true);
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        transitionFragment(new MainFragment(), false);
    }

    public void transitionFragment(PreferenceFragmentCompat preferenceFragmentCompat, boolean enabled) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_main, preferenceFragmentCompat)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
        }
    }

    private void nullTransitionFragment(PreferenceFragmentCompat nextPreferenceFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.layout_main, nextPreferenceFragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    /* DchaServiceへのバインドを試行 */
    public boolean tryBindDchaService() {
        return bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public DeviceOwnerFragment.TryApkMTask.Listener apkMListener() {
        return new DeviceOwnerFragment.TryApkMTask.Listener() {
            AlertDialog alertDialog;

            @SuppressLint("SetTextI18n")
            @Override
            public void onShow() {
                View view = getLayoutInflater().inflate(R.layout.view_progress, null);
                ProgressBar progressBar = view.findViewById(R.id.progress);
                progressBar.setProgress(0);
                TextView textPercent = view.findViewById(R.id.progress_percent);
                TextView textByte = view.findViewById(R.id.progress_byte);
                textPercent.setText(progressBar.getProgress() + getString(R.string.percent));
                alertDialog = new MaterialAlertDialogBuilder(StartActivity.this)
                        .setView(view)
                        .setMessage("")
                        .setCancelable(false)
                        .create();

                if (!alertDialog.isShowing()) alertDialog.show();

                ByteProgressHandler progressHandler = new ByteProgressHandler(1);
                progressHandler.progressBar = progressBar;
                progressHandler.textPercent = textPercent;
                progressHandler.textByte = textByte;
                progressHandler.tryApkMTask = new DeviceOwnerFragment.TryApkMTask();
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess() {
                alertDialog.dismiss();

                new DeviceOwnerFragment.TryApkMTask().cancel(true);
                DeviceOwnerFragment.TryApkTask tryApkTask = new DeviceOwnerFragment.TryApkTask();
                tryApkTask.setListener(apkListener());
                tryApkTask.execute();
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkMTask().cancel(true);

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String str) {
                alertDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkMTask().cancel(true);

                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onProgressUpdate(String str) {
                alertDialog.setMessage(str);
            }
        };
    }

    public DeviceOwnerFragment.TryXApkTask.Listener xApkListener() {
        return new DeviceOwnerFragment.TryXApkTask.Listener() {
            AlertDialog alertDialog;

            @SuppressLint("SetTextI18n")
            @Override
            public void onShow() {
                View view = getLayoutInflater().inflate(R.layout.view_progress, null);
                ProgressBar progressBar = view.findViewById(R.id.progress);
                progressBar.setProgress(0);
                TextView textPercent = view.findViewById(R.id.progress_percent);
                TextView textByte = view.findViewById(R.id.progress_byte);
                textPercent.setText(progressBar.getProgress() + getString(R.string.percent));
                alertDialog = new MaterialAlertDialogBuilder(StartActivity.this)
                        .setView(view)
                        .setMessage("")
                        .setCancelable(false)
                        .create();

                if (!alertDialog.isShowing()) alertDialog.show();

                ByteProgressHandler progressHandler = new ByteProgressHandler(0);
                progressHandler.progressBar = progressBar;
                progressHandler.textPercent = textPercent;
                progressHandler.textByte = textByte;
                progressHandler.tryXApkTask = new DeviceOwnerFragment.TryXApkTask();
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess() {
                alertDialog.dismiss();

                new DeviceOwnerFragment.TryXApkTask().cancel(true);
                DeviceOwnerFragment.TryApkTask tryApkTask = new DeviceOwnerFragment.TryApkTask();
                tryApkTask.setListener(apkListener());
                tryApkTask.execute();
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryXApkTask().cancel(true);

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String str) {
                alertDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryXApkTask().cancel(true);

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onProgressUpdate(String str) {
                alertDialog.setMessage(str);
            }
        };
    }

    public DeviceOwnerFragment.TryApkTask.Listener apkListener() {
        return new DeviceOwnerFragment.TryApkTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                DeviceOwnerFragment.getInstance().preSessionInstall.setSummary(getString(R.string.progress_state_installing));
                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                linearProgressIndicator.show();
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                DeviceOwnerFragment.getInstance().preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    if (linearProgressIndicator.isShown()) {
                        linearProgressIndicator.hide();
                    }
                } catch (Exception ignored) {
                }

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                AlertDialog alertDialog = new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }
            }

            /* 失敗 */
            @Override
            public void onFailure(String str) {
                DeviceOwnerFragment.getInstance().preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    if (linearProgressIndicator.isShown()) {
                        linearProgressIndicator.hide();
                    }
                } catch (Exception ignored) {
                }

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String str) {
                DeviceOwnerFragment.getInstance().preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    if (linearProgressIndicator.isShown()) {
                        linearProgressIndicator.hide();
                    }
                } catch (Exception ignored) {
                }

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    public MainFragment.installTask.Listener installListener() {
        return new MainFragment.installTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                MainFragment.getInstance().preSilentInstall.setSummary(getString(R.string.progress_state_installing));
                LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                linearProgressIndicator.show();
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                MainFragment.getInstance().preSilentInstall.setSummary(R.string.pre_main_sum_silent_install);
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    if (linearProgressIndicator.isShown()) {
                        linearProgressIndicator.hide();
                    }
                } catch (Exception ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                MainFragment.getInstance().preSilentInstall.setSummary(R.string.pre_main_sum_silent_install);
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    if (linearProgressIndicator.isShown()) {
                        linearProgressIndicator.hide();
                    }
                } catch (Exception ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(R.string.dialog_info_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    public MainFragment.resolutionTask.Listener resolutionListener() {
        return new MainFragment.resolutionTask.Listener() {
            Handler mHandler;
            Runnable mRunnable;

            /* 成功 */
            @Override
            public void onSuccess() {
                /* 設定変更カウントダウンダイアログ表示 */
                AlertDialog alertDialog = new MaterialAlertDialogBuilder(StartActivity.this)
                        .setTitle(R.string.dialog_title_resolution)
                        .setCancelable(false)
                        .setMessage("")
                        .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                            dialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                        })
                        .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                            dialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                            MainFragment.getInstance().resetResolution();
                        })
                        .create();

                if (!alertDialog.isShowing()) alertDialog.show();

                /* カウント開始 */
                mHandler = new Handler();
                mRunnable = new Runnable() {
                    int i = 10;

                    @Override
                    public void run() {
                        alertDialog.setMessage("変更を適用しますか？\n" + i + "秒後に元の設定に戻ります");
                        mHandler.postDelayed(this, 1000);

                        if (i <= 0) {
                            alertDialog.dismiss();
                            mHandler.removeCallbacks(this);
                            MainFragment.getInstance().resetResolution();
                        }

                        i--;
                    }
                };
                mHandler.post(mRunnable);
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        /* DchaServiceが機能していな場合は再起動 */
        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            if (!tryBindDchaService()) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
            }
        }
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_INSTALL_SILENT:
                DeviceOwnerFragment.TryApkTask.mListener.onSuccess();
                break;
            case Constants.REQUEST_INSTALL_GET_APP:
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    linearProgressIndicator.hide();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                } catch (Exception ignored) {
                }
                break;
        }
    }

    @Override
    public void onInstallFailure(int reqCode, String str) {
        switch (reqCode) {
            case Constants.REQUEST_INSTALL_SILENT:
                try {
                    DeviceOwnerFragment.TryApkTask.mListener.onFailure(str);
                } catch (Exception ignored) {
                }
                break;
            case Constants.REQUEST_INSTALL_GET_APP:
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    linearProgressIndicator.hide();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                } catch (Exception ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
                break;
        }
    }

    @Override
    public void onInstallError(int reqCode, String str) {
        switch (reqCode) {
            case Constants.REQUEST_INSTALL_SILENT:
                try {
                    DeviceOwnerFragment.TryApkTask.mListener.onError(str);
                } catch (Exception ignored) {
                }
                break;
            case Constants.REQUEST_INSTALL_GET_APP:
                try {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
                    linearProgressIndicator.hide();
                } catch (Exception ignored) {
                }

                try {
                    MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                } catch (Exception ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDchaService != null) {
            unbindService(mDchaServiceConnection);
        }
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            /* アップデートチェック要求の場合 */
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = parseJson(this);
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");
                    Variables.DOWNLOAD_FILE_URL = jsonObj3.getString("url");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        Snackbar.make(this, findViewById(R.id.layout_main), "新しいバージョンが利用可能です", Snackbar.LENGTH_LONG).setAction("更新", v ->
                                startActivity(new Intent(this, SelfUpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))).show();
                    }
                } catch (JSONException | IOException ignored) {
                }
                break;
            case Constants.REQUEST_DOWNLOAD_APP_CHECK:
                MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
                ArrayList<AppListView.AppData> list = new ArrayList<>();

                try {
                    JSONObject jsonObj1 = parseJson(this);
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("appList");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        AppListView.AppData data = new AppListView.AppData();
                        data.str = jsonArray.getJSONObject(i).getString("name");
                        list.add(data);
                    }
                } catch (JSONException | IOException ignored) {
                }

                View v = getLayoutInflater().inflate(R.layout.layout_app_list, null);
                ListView lv = v.findViewById(R.id.app_list);
                lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                lv.setAdapter(new AppListView.AppListAdapter(this, list));
                lv.setOnItemClickListener((parent, view, position, id) -> {
                    Preferences.save(this, Constants.KEY_RADIO_TMP, (int) id);
                    lv.invalidateViews();
                });

                cancelLoadingDialog();

                new MaterialAlertDialogBuilder(this)
                        .setView(v)
                        .setTitle("アプリを選択してください")
                        .setMessage("選択してOKを押下すると詳細な情報が表示されます")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            StringBuilder str = new StringBuilder();

                            for (int i = 0; i < lv.getCount(); i++) {
                                RadioButton radioButton = lv.getChildAt(i).findViewById(R.id.v_app_list_radio);
                                if (radioButton.isChecked()) {
                                    try {
                                        JSONObject jsonObj1 = parseJson(this);
                                        JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                                        JSONArray jsonArray = jsonObj2.getJSONArray("appList");
                                        str.append("アプリ名：").append(jsonArray.getJSONObject(i).getString("name")).append("\n\n").append("説明：").append(jsonArray.getJSONObject(i).getString("description")).append("\n");
                                        Variables.DOWNLOAD_FILE_URL = jsonArray.getJSONObject(i).getString("url");
                                    } catch (JSONException | IOException ignored) {
                                    }
                                }
                            }

                            if (str.toString().equals("")) {
                                return;
                            }

                            new MaterialAlertDialogBuilder(this)
                                    .setMessage(str + "\n" + "よろしければOKを押下してください")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(Variables.DOWNLOAD_FILE_URL, "MYURL")) {
                                            startDownload();
                                            dialog.dismiss();
                                        } else {
                                            View view = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            EditText editText = view.findViewById(R.id.edit_app_url);
                                            new MaterialAlertDialogBuilder(this)
                                                    .setMessage("http://またはhttps://を含むURLを指定してください")
                                                    .setView(view)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                                        Variables.DOWNLOAD_FILE_URL = editText.getText().toString();
                                                        startDownload();
                                                    })
                                                    .setNegativeButton(R.string.dialog_common_cancel, null)
                                                    .show();
                                        }
                                    })
                                    .show();
                        })
                        .show();
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                new Handler().post(() -> new Updater(this).installApk(this, 1));
                break;
            default:
                break;
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
        if (reqCode != Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            cancelLoadingDialog();
            new MaterialAlertDialogBuilder(this)
                    .setMessage("ダウンロードに失敗しました\nネットワークが安定しているか確認してください")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onConnectionError(int reqCode) {
        if (reqCode != Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            cancelLoadingDialog();
            new MaterialAlertDialogBuilder(this)
                    .setMessage("データ取得に失敗しました\nネットワークを確認してください")
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    public void showLoadingDialog() {
        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
        linearProgressIndicator.show();
    }

    public void cancelLoadingDialog() {
        try {
            MainFragment.getInstance().preGetApp.setSummary(R.string.pre_main_sum_get_app);
        } catch (Exception ignored) {
        }

        try {
            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
            if (linearProgressIndicator.isShown()) {
                linearProgressIndicator.hide();
            }
        } catch (Exception ignored) {
        }
    }

    private void startDownload() {
        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.act_progress_main);
        linearProgressIndicator.show();
        AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()), Constants.REQUEST_DOWNLOAD_APK);
        asyncFileDownload.execute();
        ProgressHandler progressHandler = new ProgressHandler();
        progressHandler.linearProgressIndicator = linearProgressIndicator;
        progressHandler.asyncfiledownload = asyncFileDownload;
        progressHandler.sendEmptyMessage(0);
    }
}