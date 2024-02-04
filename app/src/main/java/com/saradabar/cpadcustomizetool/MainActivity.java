package com.saradabar.cpadcustomizetool;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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

public class MainActivity extends AppCompatActivity implements DownloadEventListener {

    IDchaService mDchaService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        setContentView(R.layout.layout_progress);

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
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_error_crash, getApplicationInfo().loadLabel(getPackageManager())))
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                    Preferences.save(this, Constants.KEY_FLAG_CRASH_LOG, false);
                    firstCheck();
                })
                .setNeutralButton(R.string.dialog_common_check, (dialog, which) -> {
                    startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    finish();
                })
                .show();
    }

    private void firstCheck() {
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
            /* 初回起動はアップデート確認後ウォークスルー起動 */
            /* アップデートチェック */
            updateCheck();
        }
    }

    /* アップデートチェック */
    private void updateCheck() {
        showLoadingDialog();
        new AsyncFileDownload(this, Constants.URL_CHECK, new File(new File(getExternalCacheDir(), "Check.json").getPath()), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK).execute();
    }

    /* json読み取り */
    public JSONObject parseJson() throws JSONException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(getExternalCacheDir(), "Check.json").getPath()));
        JSONObject json;
        StringBuilder data = new StringBuilder();
        String str = bufferedReader.readLine();

        while (str != null) {
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
                        new WelcomeHelper(this, WelAppActivity.class).forceShow();
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
    public void onDownloadError(int reqCode) {
        cancelLoadingDialog();
        new WelcomeHelper(this, WelAppActivity.class).forceShow();
    }


    /* サーバー接続エラー */
    @Override
    public void onConnectionError(int reqCode) {
        cancelLoadingDialog();
        new WelcomeHelper(this, WelAppActivity.class).forceShow();
    }

    /* アップデートダイアログ */
    private void showUpdateDialog(String str) {
        /* モデルIDをセット */
        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT3);
                break;
            case "TAB-A05-BD":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTX);
                break;
            case "TAB-A05-BA1":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTZ);
                break;
            default:
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2);
                break;
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

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    LinearProgressIndicator linearProgressIndicator = findViewById(R.id.layout_progress_main);
                    linearProgressIndicator.show();
                    AsyncFileDownload asyncFileDownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()), Constants.REQUEST_DOWNLOAD_APK);
                    asyncFileDownload.execute();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.linearProgressIndicator = linearProgressIndicator;
                    progressHandler.textView = findViewById(R.id.layout_text_progress);
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                    new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    /* ローディングダイアログを表示する */
    private void showLoadingDialog() {
        TextView textView = findViewById(R.id.layout_text_progress);
        textView.setText("サーバーと通信しています...");
        LinearProgressIndicator linearProgressIndicator = findViewById(R.id.layout_progress_main);
        linearProgressIndicator.show();
    }

    /* ローディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        TextView textView = findViewById(R.id.layout_text_progress);
        textView.setText("");
        try {
            LinearProgressIndicator linearProgressIndicator = findViewById(R.id.layout_progress_main);
            if (linearProgressIndicator.isShown()) {
                linearProgressIndicator.hide();
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
        new MaterialAlertDialogBuilder(this)
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
        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            if (!tryBindDchaService()) {
                new MaterialAlertDialogBuilder(this)
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
        new MaterialAlertDialogBuilder(this)
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
            new MaterialAlertDialogBuilder(this)
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