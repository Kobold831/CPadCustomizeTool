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
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainActivity extends Activity implements DownloadEventListener {

    IDchaService mDchaService;
    ProgressDialog loadingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        /* 前回クラッシュしているかどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_CRASH_LOG, false)) {
            /* クラッシュダイアログ表示 */
            crashError();
        } else {
            /* 起動処理 */
            firstCheck();
        }
    }

    private void crashError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_error_crash, getApplicationInfo().loadLabel(getPackageManager())))
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                    Preferences.save(this, Constants.KEY_FLAG_CRASH_LOG, false);
                    firstCheck();
                })
                .setNeutralButton(R.string.dialog_common_check, (dialog, which) -> startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                .show();
    }

    private void firstCheck() {
        /* アップデートチェックするか確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_UPDATE, false)) {
            /* ネットワークチェック */
            if (!isNetworkState()) {
                networkError();
            } else {
                /* アップデートチェック */
                updateCheck();
            }
            return;
        }

        /* 初回起動か確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            /* 初回起動ではないならサポート端末か確認 */
            if (supportModelCheck()) {
                /* DchaServiceを確認 */
                checkDchaService();
            } else {
                supportModelError();
            }
        } else {
            /* 初回起動はウォークスルー起動 */
            new WelcomeHelper(this, WelAppActivity.class).forceShow();
        }
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
                    if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                        if (supportModelCheck()) checkDchaService();
                        else supportModelError();
                    } else new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    /* アップデートチェック */
    private void updateCheck() {
        showLoadingDialog();
        new AsyncFileDownload(this, "https://raw.githubusercontent.com/Kobold831/Server/main/production/json/Check.json", new File(new File(getExternalCacheDir(), "Check.json").getPath()), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK).execute();
    }

    /* json読み取り */
    public JSONObject parseJson() throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(getExternalCacheDir(), "Check.json").getPath()));
        JSONObject json;
        StringBuilder data = new StringBuilder();
        String str = bufferedReader.readLine();

        while(str != null){
            data.append(str);
            str = bufferedReader.readLine();
        }

        json = new JSONObject(data.toString());
        bufferedReader.close();
        return json;
    }

    /* ダウンロード完了 */
    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            /* アップデートチェック要求の場合 */
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = parseJson();
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");
                    Variables.DOWNLOAD_FILE_URL = jsonObj3.getString("url");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        cancelLoadingDialog();
                        showUpdateDialog(jsonObj3.getString("description"));
                    } else {
                        cancelLoadingDialog();

                        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                            if (supportModelCheck()) {
                                checkDchaService();
                            } else {
                                supportModelError();
                            }
                        } else {
                            new WelcomeHelper(this, WelAppActivity.class).forceShow();
                        }
                    }
                } catch (JSONException | IOException ignored) {
                }
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                new Handler().post(() -> new Updater(this).installApk(this, 0));
                break;
            default:
                break;
        }
    }

    /* ダウンロードエラー */
    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage("ダウンロードに失敗しました\nネットワークが安定しているか確認してください")
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                        if (supportModelCheck()) {
                            checkDchaService();
                        } else {
                            supportModelError();
                        }
                    } else {
                        new WelcomeHelper(this, WelAppActivity.class).forceShow();
                    }
                })
                .show();
    }


    /* サーバー接続エラー */
    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_connection)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                        if (supportModelCheck()) {
                            checkDchaService();
                        } else {
                            supportModelError();
                        }
                    } else {
                        new WelcomeHelper(this, WelAppActivity.class).forceShow();
                    }
                })
                .show();
    }

    /* アップデートダイアログ */
    private void showUpdateDialog(String str) {
        /* 初回起動ならモデルIDをセット */
        if (!Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            switch (Build.MODEL) {
                case "TAB-A05-BD":
                    Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTX);
                    break;
                case "TAB-A05-BA1":
                    Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTZ);
                    break;
            }
        }

        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView tv = view.findViewById(R.id.update_information);

        tv.setText(str);
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
                    AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()), Constants.REQUEST_DOWNLOAD_APK);
                    asyncFileDownload.execute();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_title_update);
                    progressDialog.setMessage(getString(R.string.progress_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                            if (supportModelCheck()) checkDchaService();
                            else supportModelError();
                        } else {
                            new WelcomeHelper(this, WelAppActivity.class).forceShow();
                        }
                    });
                    progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                    if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                        if (supportModelCheck()) checkDchaService();
                        else supportModelError();
                    } else {
                        new WelcomeHelper(this, WelAppActivity.class).forceShow();
                    }
                })
                .show();
    }

    /* ローディングダイアログを表示する */
    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_connecting), true);
        loadingDialog.show();
    }

    /* ろ＾ディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        try {
            if (loadingDialog != null) {
                loadingDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    private boolean supportModelCheck() {
        String[] modelName = {"TAB-A03-BS", "TAB-A03-BR", "TAB-A03-BR2", "TAB-A03-BR3", "TAB-A05-BD", "TAB-A05-BA1"};

        for (String string : modelName) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
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
        /* DchaServiceを使用するか確認 */
        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            switch (Build.MODEL) {
                case "TAB-A03-BR3":
                    confCheckCT3();
                    break;
                case "TAB-A05-BD":
                    confCheckCTX();
                    break;
                case "TAB-A05-BA1":
                    confCheckCTZ();
                    break;
                default:
                    confCheckCT2();
                    break;
            }
            return;
        }

        /* DchaServiceが機能しているか */
        if (!tryBindDchaService()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_common_error)
                    .setMessage(R.string.dialog_error_start_dcha_service)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .setNeutralButton(R.string.dialog_common_continue, (dialogInterface, i) -> {
                        Preferences.save(this, Constants.KEY_FLAG_DCHA_SERVICE, false);
                        switch (Build.MODEL) {
                            case "TAB-A03-BR3":
                                confCheckCT3();
                                break;
                            case "TAB-A05-BD":
                                confCheckCTX();
                                break;
                            case "TAB-A05-BA1":
                                confCheckCTZ();
                                break;
                            default:
                                confCheckCT2();
                                break;
                        }
                    })
                    .show();
            return;
        }

        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                confCheckCT3();
                break;
            case "TAB-A05-BD":
                confCheckCTX();
                break;
            case "TAB-A05-BA1":
                confCheckCTZ();
                break;
            default:
                confCheckCT2();
                break;
        }
    }

    /* Pad2起動設定チェック */
    private void confCheckCT2() {
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        } else {
            WarningDialog();
        }
    }

    /* Pad3起動設定チェック */
    private void confCheckCT3() {
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT3);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* NEO起動設定チェック */
    private void confCheckCTX() {
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTX);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* NEXT起動設定チェック */
    private void confCheckCTZ() {
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTZ);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* 初回起動お知らせ */
    public void WarningDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (isPermissionCheck()) {
                        Preferences.save(this, Constants.KEY_FLAG_SETTINGS, true);
                        startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        overridePendingTransition(0, 0);
                        finish();
                    }
                })
                .show();
    }

    /* システム設定変更権限か付与されているか確認 */
    private boolean isPermissionCheck() {
        if (isWriteSystemPermissionCheck()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_grant_permission)
                    .setMessage(R.string.dialog_error_start_permission)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_settings, (dialog, which) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), Constants.REQUEST_PERMISSION);
                        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canWrite = Settings.System.canWrite(this);
        }
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

    /* DchaServiceへバインド */
    public boolean tryBindDchaService() {
        return bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_UPDATE:
                if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                    if (supportModelCheck()) {
                        checkDchaService();
                    } else {
                        supportModelError();
                    }
                } else {
                    new WelcomeHelper(this, WelAppActivity.class).forceShow();
                }
                break;
            case WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
            case Constants.REQUEST_PERMISSION:
                if (supportModelCheck()) {
                    checkDchaService();
                } else {
                    supportModelError();
                }
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
}