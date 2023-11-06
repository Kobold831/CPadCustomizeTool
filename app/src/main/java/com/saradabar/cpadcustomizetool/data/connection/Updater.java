package com.saradabar.cpadcustomizetool.data.connection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListenerList;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.installer.SplitInstaller;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class Updater implements InstallEventListener {

    int reqCode, currentVersionCode, latestVersionCode;
    String description, url;
    DownloadEventListenerList downloadEventListenerList;
    Activity activity;
    IDchaService mDchaService;

    @SuppressLint("StaticFieldLeak")
    static Updater instance = null;

    public static Updater getInstance() {
        return instance;
    }

    public Updater(Activity activity, String str, int i) {
        instance = this;
        url = str;
        this.activity = activity;
        reqCode = i;
        downloadEventListenerList = new DownloadEventListenerList();

        downloadEventListenerList.addEventListener((DownloadEventListener) this.activity);
    }

    private int updateAvailableCheck() {
        try {
            getCurrentVersionInfo();
            getLatestVersionInfo();

            if (latestVersionCode == -99) return -1;
            if (currentVersionCode < latestVersionCode) return 1;
            else return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void getCurrentVersionInfo() throws Exception {
        currentVersionCode = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_META_DATA).versionCode;
    }

    private void getLatestVersionInfo() {
        HashMap<String, String> map = parseUpdateXml(url);

        if (map != null) {
            latestVersionCode = Integer.parseInt(Objects.requireNonNull(map.get("versionCode")));
            Variables.DOWNLOAD_FILE_URL = map.get("url");
            description = map.get("description");
        } else latestVersionCode = -99;
    }

    public void updateCheck() {
        new updateCheckTask().execute();
    }

    @Override
    public void onInstallSuccess() {

    }

    /* 失敗 */
    @Override
    public void onInstallFailure(String str) {
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    @Override
    public void onInstallError(String str) {
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.dialog_error) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    @SuppressLint("StaticFieldLeak")
    private class updateCheckTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... arg0) {
            return updateAvailableCheck();
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case -1:
                    downloadEventListenerList.connectionErrorNotify();
                    break;
                case 0:
                    if (reqCode == 0) downloadEventListenerList.updateUnavailableNotify();
                    if (reqCode == 1) downloadEventListenerList.updateUnavailableNotify1();
                    break;
                case 1:
                    if (reqCode == 0) downloadEventListenerList.updateAvailableNotify(description);
                    if (reqCode == 1) downloadEventListenerList.updateAvailableNotify1(description);
                    break;
            }
        }
    }

    public void installApk(Context context) {
        switch (Preferences.GET_UPDATE_MODE(activity)) {
            case 0:
                Uri uri = Uri.fromFile(new File(new File(context.getExternalCacheDir(), "update.apk").getPath()));

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivityForResult(intent, Constants.REQUEST_UPDATE);
                break;
            case 1:
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_update)
                        .setMessage(R.string.dialog_info_update_caution)
                        .setPositiveButton(R.string.dialog_common_yes, (dialog2, which2) -> {
                            try {
                                activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE)), Constants.REQUEST_UPDATE);
                            } catch (ActivityNotFoundException ignored) {
                                Toast.toast(activity, R.string.toast_unknown_activity);
                                activity.finish();
                            }
                        })
                        .show();
                break;
            case 2:
                ProgressDialog progressDialog = new ProgressDialog(activity);
                progressDialog.setTitle("");
                progressDialog.setMessage("インストール中・・・");
                progressDialog.setCancelable(false);
                progressDialog.show();

                if (isBindDchaService()) {
                    Runnable runnable = () -> {
                        if (!isInstallPackage()) {
                            progressDialog.dismiss();

                            new AlertDialog.Builder(activity)
                                    .setCancelable(false)
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                    .show();
                        }
                    };
                    new Handler().postDelayed(runnable, 10);
                } else {
                    progressDialog.dismiss();

                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_error)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
            case 3:
                if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
                    if (!trySessionInstall()) {
                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setMessage(R.string.dialog_error)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                .show();
                    }
                } else {
                    if (Preferences.GET_MODEL_ID(activity) == 2) {
                        Preferences.SET_UPDATE_MODE(activity, 1);
                    } else Preferences.SET_UPDATE_MODE(activity, 0);
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(activity.getString(R.string.dialog_error_reset_update_mode))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
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

    public boolean isBindDchaService() {
        return activity.bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean isInstallPackage() {
        if (mDchaService != null) {
            try {
                boolean bl = mDchaService.installApp(new File(activity.getExternalCacheDir(), "update.apk").getPath(), 1);

                activity.unbindService(mDchaServiceConnection);

                return bl;
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }

    private boolean trySessionInstall() {
        SplitInstaller splitInstaller = new SplitInstaller();
        int sessionId;

        try {
            sessionId = splitInstaller.splitCreateSession(activity).i;
            if (sessionId < 0) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            if (!splitInstaller.splitWriteSession(activity, new File(activity.getExternalCacheDir(), "update.apk"), sessionId).bl) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            return splitInstaller.splitCommitSession(activity, sessionId, 1).bl;
        } catch (Exception ignored) {
            return false;
        }
    }

    private HashMap<String, String> parseUpdateXml(String url) {
        HashMap<String, String> map = new HashMap<>();
        HttpURLConnection httpURLConnection;

        try {
            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setConnectTimeout(5000);
            InputStream is = httpURLConnection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(bis);
            Element root = document.getDocumentElement();

            if (root.getTagName().equals("update")) {
                NodeList nodelist = root.getChildNodes();

                for (int j = 0; j < nodelist.getLength(); j++) {
                    Node node = nodelist.item(j);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String tagName = element.getTagName();
                        String textContent = element.getTextContent().trim();

                        map.put(tagName, textContent);
                    }
                }
            }

            return map;
        } catch (IOException | SAXException | ParserConfigurationException ignored) {
            return null;
        }
    }
}