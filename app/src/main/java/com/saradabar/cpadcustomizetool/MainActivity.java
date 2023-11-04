package com.saradabar.cpadcustomizetool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.data.connection.Checker;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.UpdateEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.activity.WelAppActivity;
import com.stephentuso.welcome.WelcomeHelper;

import java.io.File;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainActivity extends Activity implements UpdateEventListener {
    IDchaService mDchaService;
    ProgressDialog ldDialog;
    boolean result = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        if (Preferences.GET_CRASH(this)) {
            crashError();
            return;
        }
        firstCheck();
    }

    private void firstCheck() {
        /* ネットワークチェック */
        if (!isNetworkState()) {
            networkError();
            return;
        }
        /* アップデートチェックの可否を確認 */
        if (Preferences.GET_UPDATE_FLAG(this)) updateCheck();
        else supportCheck();
    }

    private void crashError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("前回" + getApplicationInfo().loadLabel(getPackageManager()).toString() + "が予期せずに終了しました\n繰り返し発生する場合は\"ログを見る\"を押してクラッシュログを開発者に送信してください")
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                    Preferences.SET_CRASH(this, false);
                    firstCheck();
                })
                .setNeutralButton("ログを見る", (dialog, which) -> startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                .show();
    }

    /* ネットワークの接続を確認 */
    private boolean isNetworkState() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void networkError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_wifi)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    result = false;
                    if (Preferences.GET_SETTINGS_FLAG(this)) {
                        if (supportModelCheck()) checkDchaService();
                        else supportModelError();
                    } else new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    private void updateCheck() {
        showLdDialog();
        new Updater(this, Constants.URL_UPDATE_CHECK, 1).updateCheck();
    }

    private void supportCheck() {
        if (!Preferences.GET_UPDATE_FLAG(this)) showLdDialog();
        new Checker(this, Constants.URL_SUPPORT_CHECK).supportCheck();
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(() -> new Updater(this, Constants.URL_UPDATE_CHECK, 1).installApk(this));
    }

    @Override
    public void onUpdateAvailable(String string) {
    }

    @Override
    public void onUpdateUnavailable() {
    }

    public void onSupportAvailable() {
        cancelLdDialog();
        showSupportDialog();
    }

    public void onSupportUnavailable() {
        cancelLdDialog();
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (supportModelCheck()) checkDchaService();
            else supportModelError();
        } else {
            new WelcomeHelper(this, WelAppActivity.class).forceShow();
        }
    }

    @Override
    public void onUpdateAvailable1(String string) {
        cancelLdDialog();
        showUpdateDialog(string);
    }

    @Override
    public void onUpdateUnavailable1() {
        supportCheck();
    }

    @Override
    public void onDownloadError() {
        cancelLdDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    @Override
    public void onConnectionError() {
        cancelLdDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_connection)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    private void showUpdateDialog(String string) {
        if (!Preferences.GET_SETTINGS_FLAG(this)) {
            switch (Build.MODEL) {
                case "TAB-A05-BD":
                case "TAB-A05-BA1":
                    Preferences.SET_MODEL_ID(2, this);
                    break;
            }
        }
        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView mTextView = view.findViewById(R.id.update_information);
        mTextView.setText(string);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE_INFO)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()));
                    asyncFileDownload.execute();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_title_update);
                    progressDialog.setMessage(getString(R.string.progress_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        if (isNetworkState()) {
                            showLdDialog();
                            supportCheck();
                        } else networkError();
                    });
                    progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                    if (isNetworkState()) {
                        showLdDialog();
                        supportCheck();
                    } else networkError();
                })
                .show();
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_use)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    result = false;
                    if (Preferences.GET_SETTINGS_FLAG(this)) {
                        if (supportModelCheck()) checkDchaService();
                        else supportModelError();
                    } else new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    private void showLdDialog() {
        ldDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_connecting), true);
        ldDialog.show();
    }

    private void cancelLdDialog() {
        try {
            if (ldDialog != null) ldDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    private boolean supportModelCheck() {
        String[] modelName = {"TAB-A03-BS", "TAB-A03-BR", "TAB-A03-BR2", "TAB-A03-BR3", "TAB-A05-BD", "TAB-A05-BA1"};
        for (String string : modelName) if (Objects.equals(string, Build.MODEL)) return true;
        return false;
    }

    /* 端末チェックエラー */
    private void supportModelError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_error_start_cpad)
                .setIcon(R.drawable.alert)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDchaService() {
        if (!Preferences.GET_DCHASERVICE_FLAG(this)) {
            switch (Build.MODEL) {
                case "TAB-A03-BR3":
                    confCheckPad3();
                    break;
                case "TAB-A05-BD":
                case "TAB-A05-BA1":
                    confCheckPadNEO();
                    break;
                default:
                    confCheckPad2();
                    break;
            }
            return;
        }

        /* DchaServiceの使用可否を確認 */
        if (!isBindDchaService()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_common_error)
                    .setMessage(R.string.dialog_error_start_dcha_service)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .setNeutralButton(R.string.dialog_common_continue, (dialogInterface, i) -> {
                        Preferences.SET_DCHASERVICE_FLAG(false, this);
                        switch (Build.MODEL) {
                            case "TAB-A03-BR3":
                                confCheckPad3();
                                break;
                            case "TAB-A05-BD":
                            case "TAB-A05-BA1":
                                confCheckPadNEO();
                                break;
                            default:
                                confCheckPad2();
                                break;
                        }
                    })
                    .show();
            return;
        }

        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                confCheckPad3();
                break;
            case "TAB-A05-BD":
            case "TAB-A05-BA1":
                confCheckPadNEO();
                break;
            default:
                confCheckPad2();
                break;
        }
    }

    /* Pad2起動設定チェック */
    private void confCheckPad2() {
        Preferences.SET_MODEL_ID(0, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
            overridePendingTransition(0, 0);
            finish();
        } else tosDialog();
    }

    /* Pad3起動設定チェック */
    private void confCheckPad3() {
        Preferences.SET_MODEL_ID(1, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                overridePendingTransition(0, 0);
                finish();
            }
        } else tosDialog();
    }

    /* PadNeo起動設定チェック */
    private void confCheckPadNEO() {
        Preferences.SET_MODEL_ID(2, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                overridePendingTransition(0, 0);
                finish();
            }
        } else tosDialog();
    }

    /* 初回起動お知らせ */
    public void tosDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (isPermissionCheck()) {
                        Preferences.SET_SETTINGS_FLAG(true, this);
                        startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                        overridePendingTransition(0, 0);
                        finish();
                    }
                })
                .show();
    }

    /* 権限設定 */
    private boolean isPermissionCheck() {
        if (isWriteSystemPermissionCheck()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_grant_permission)
                    .setMessage(R.string.dialog_error_start_permission)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_settings, (dialog, which) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), Constants.REQUEST_PERMISSION);
                    })
                    .setNeutralButton(R.string.dialog_common_exit, (dialogInterface, i) -> finishAndRemoveTask())
                    .show();
            return false;
        } else {
            return true;
        }
    }

    /* システム設定変更権限チェック */
    private boolean isWriteSystemPermissionCheck() {
        boolean canWrite = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            canWrite = Settings.System.canWrite(this);
        return !canWrite;
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

    public boolean isBindDchaService() {
        return bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_UPDATE:
                if (isNetworkState()) {
                    showLdDialog();
                    supportCheck();
                } else networkError();
                break;
            case WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
            case Constants.REQUEST_PERMISSION:
                if (supportModelCheck()) checkDchaService();
                else supportModelError();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDchaService != null) unbindService(mDchaServiceConnection);
    }
}