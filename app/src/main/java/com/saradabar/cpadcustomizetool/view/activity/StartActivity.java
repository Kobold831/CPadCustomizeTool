package com.saradabar.cpadcustomizetool.view.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.flagment.AppSettingsFragment;
import com.saradabar.cpadcustomizetool.view.flagment.DeviceOwnerFragment;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;
import com.saradabar.cpadcustomizetool.view.views.AppListView;
import com.stephentuso.welcome.WelcomeHelper;

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
    ProgressDialog loadingDialog;
    Menu menu;

    public static StartActivity getInstance() {
        return instance;
    }

    /* 設定画面表示 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }

        setContentView(R.layout.activity_main);
        transitionFragment(new MainFragment(), false);
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
                alertDialog = new AlertDialog.Builder(StartActivity.this)
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

                new AlertDialog.Builder(StartActivity.this)
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
                alertDialog = new AlertDialog.Builder(StartActivity.this)
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

                new AlertDialog.Builder(StartActivity.this)
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

    public DeviceOwnerFragment.TryApkTask.Listener apkListener() {
        return new DeviceOwnerFragment.TryApkTask.Listener() {
            ProgressDialog progressDialog;

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                progressDialog = new ProgressDialog(StartActivity.this);
                progressDialog.setTitle("");
                progressDialog.setMessage(getString(R.string.progress_state_installing));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                progressDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this)
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
                progressDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String str) {
                progressDialog.dismiss();

                try {
                    /* 一時ファイルを消去 */
                    FileUtils.deleteDirectory(getExternalCacheDir());
                } catch (IOException ignored) {
                }

                new DeviceOwnerFragment.TryApkTask().cancel(true);

                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + str)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    public MainFragment.installTask.Listener installListener() {
        return new MainFragment.installTask.Listener() {
            ProgressDialog progressDialog;

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                progressDialog = new ProgressDialog(StartActivity.this);
                progressDialog.setTitle("");
                progressDialog.setMessage(getString(R.string.progress_state_installing));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                progressDialog.dismiss();

                new AlertDialog.Builder(StartActivity.this)
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                progressDialog.dismiss();

                new AlertDialog.Builder(StartActivity.this)
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
                AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this)
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
                new AlertDialog.Builder(StartActivity.this)
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
        if (Preferences.GET_DCHASERVICE_FLAG(this)) {
            if (!tryBindDchaService()) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
            }
        }
    }

    @Override
    public void onInstallSuccess() {
        DeviceOwnerFragment.TryApkTask.mListener.onSuccess();
    }

    @Override
    public void onInstallFailure(String str) {
        DeviceOwnerFragment.TryApkTask.mListener.onFailure(str);
    }

    @Override
    public void onInstallError(String str) {
        DeviceOwnerFragment.TryApkTask.mListener.onError(str);
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
            case Constants.REQUEST_DOWNLOAD_APP_CHECK:
                ArrayList<AppListView.AppData> list = new ArrayList<>();

                try {
                    JSONObject jsonObj1 = Common.parseJson(this);
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
                lv.setAdapter(new AppListView.AppListAdapter(this, list));
                lv.setOnItemClickListener((parent, view, position, id) -> {
                    CheckBox checkBox = lv.getChildAt(position).findViewById(R.id.v_app_list_check);
                    checkBox.setChecked(!checkBox.isChecked());
                });

                cancelLdDialog();

                new AlertDialog.Builder(this)
                        .setView(v)
                        .setTitle("アプリを選択してください（１つのみチェック）")
                        .setMessage("選択したあとOKを押すと詳細な情報が表示されます")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            StringBuilder str = new StringBuilder();

                            for (int i = 0; i <lv.getCount(); i++) {
                                CheckBox checkBox = lv.getChildAt(i).findViewById(R.id.v_app_list_check);
                                if (checkBox.isChecked()) {
                                    try {
                                        JSONObject jsonObj1 = Common.parseJson(this);
                                        JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                                        JSONArray jsonArray = jsonObj2.getJSONArray("appList");
                                        str.append(jsonArray.getJSONObject(i).getString("name")).append("\n").append(jsonArray.getJSONObject(i).getString("description")).append("\n");
                                        Variables.DOWNLOAD_FILE_URL = jsonArray.getJSONObject(i).getString("url");
                                    } catch (JSONException | IOException ignored) {
                                    }
                                }
                            }

                            if (str.toString().equals("")) {
                                return;
                            }

                            new AlertDialog.Builder(this)
                                    .setMessage(str + "\n" + "よろしければOKを押下してください")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(Variables.DOWNLOAD_FILE_URL, "MYURL")) {
                                            startDownload();
                                            dialog.dismiss();
                                        } else {
                                            View view = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            EditText editText = view.findViewById(R.id.edit_app_url);
                                            new AlertDialog.Builder(this)
                                                    .setMessage("http://またはhttps://を含むURLを指定してください")
                                                    .setView(view)
                                                    .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                        Variables.DOWNLOAD_FILE_URL = editText.getText().toString();
                                                        startDownload();
                                                    })
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
    public void onDownloadError() {
        cancelLdDialog();
        new AlertDialog.Builder(this)
                .setMessage("ダウンロードに失敗しました\nネットワークが安定しているか確認してください")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onConnectionError() {
        cancelLdDialog();
        new AlertDialog.Builder(this)
                .setMessage("データ取得に失敗しました\nネットワークを確認してください")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void showLdDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_connecting), true);
        loadingDialog.show();
    }

    public void cancelLdDialog() {
        try {
            if (loadingDialog != null) {
                loadingDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    private void startDownload() {
        AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()), Constants.REQUEST_DOWNLOAD_APK);
        asyncFileDownload.execute();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("インストール");
        progressDialog.setMessage("インストールファイルをサーバーからダウンロード中...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_common_cancel), (dialog3, which3) -> {
            asyncFileDownload.cancel(true);
        });
        progressDialog.show();
        ProgressHandler progressHandler = new ProgressHandler();
        progressHandler.progressDialog = progressDialog;
        progressHandler.asyncfiledownload = asyncFileDownload;
        progressHandler.sendEmptyMessage(0);
    }
}