package com.saradabar.cpadcustomizetool.view.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.UpdateEventListener;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;

import java.io.File;

public class UpdateActivity extends Activity implements UpdateEventListener {

    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        showLoadingDialog();
        new Updater(this, Constants.URL_UPDATE_CHECK, 0).updateCheck();
    }

    @Override
    public void onUpdateAvailable(String string) {
        cancelLoadingDialog();
        new Handler().post(() -> showUpdateDialog(string));
    }

    @Override
    public void onUpdateUnavailable() {
        cancelLoadingDialog();
        new Handler().post(this::showNoUpdateDialog);
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(() -> new Updater(this, Constants.URL_UPDATE_CHECK, 0).installApk(this));
    }

    @Override
    public void onSupportAvailable() {

    }

    @Override
    public void onSupportUnavailable() {

    }

    @Override
    public void onUpdateAvailable1(String d) {

    }

    @Override
    public void onUpdateUnavailable1() {

    }

    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_connection)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finish())
                .show();
    }

    private void showUpdateDialog(String mString) {
        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView mTextView = view.findViewById(R.id.update_information);
        mTextView.setText(mString);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE_INFO)));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });
        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = initFileLoader();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_title_update);
                    progressDialog.setMessage("アップデートファイルをサーバーからダウンロード中・・・");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "キャンセル", (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        finish();
                    });
                    progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> finish())
                .show();
    }

    private void showNoUpdateDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_info_no_update)
                .setPositiveButton(R.string.dialog_common_ok,
                        (dialog, which) -> finish())
                .show();
    }

    private AsyncFileDownload initFileLoader() {
        AsyncFileDownload asyncfiledownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()));
        asyncfiledownload.execute();
        return asyncfiledownload;
    }

    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_update_check), true);
    }

    private void cancelLoadingDialog() {
        if (loadingDialog != null) loadingDialog.cancel();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_UPDATE) {
            finish();
        }
    }
}