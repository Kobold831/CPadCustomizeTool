package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;

import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkMCopyTask {

    public static ApkMCopyTask apkMCopyTask;
    long totalByte = 0;

    public void execute(Context context, Listener listener, String[] splitInstallData) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Object result = doInBackground(context, listener, splitInstallData);
                handler.post(() -> onPostExecute(context, listener, result));
            }).start();
        });
    }

    void onPreExecute(Listener listener) {
        apkMCopyTask = this;
        listener.onShow();
    }

    void onPostExecute(Context context, Listener listener, Object result) {
        if (result == null) {
            totalByte = -1;
            listener.onError(context.getString(R.string.installer_status_unknown_error));
            return;
        }

        if (result.equals(true)) {
            totalByte = -1;
            listener.onSuccess();
            return;
        }

        if (result.equals(false)) {
            totalByte = -1;
            listener.onFailure();
            return;
        }

        totalByte = -1;
        listener.onError(result.toString());
    }

    public void onProgressUpdate(Listener listener, String message) {
        listener.onProgressUpdate(message);
    }

    protected Object doInBackground(Context context, Listener listener, String[] splitInstallData) {
        String zipFile = new File(splitInstallData[0]).getParent() + File.separator + new File(splitInstallData[0]).getName().replaceFirst("\\..*", ".zip");

        /* 拡張子.apkmを.zipに変更 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_rename));

        if (!new File(splitInstallData[0]).renameTo(new File(zipFile))) {
            return "拡張子の変更に失敗しました";
        }

        File tmpFile = new File(Common.getTemporaryPath(context));

        /* zipを展開して外部ディレクトリに一時保存 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_unpack));

        totalByte = new File(zipFile).length();

        try {
            ZipUtil.unpack(new File(zipFile), tmpFile);
        } catch (ZipException e) {
            return "圧縮ファイルが無効のため展開できません\n" + e.getMessage();
        } catch (Exception e) {
            return context.getString(R.string.installer_status_no_allocatable_space) + e.getMessage();
        }

        /* 拡張子.zipを.apkmに変更 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_rename));

        if (!new File(zipFile).renameTo(new File(new File(zipFile).getParent() + File.separator + new File(zipFile).getName().replaceFirst("\\..*", ".apkm")))) {
            return "拡張子の変更に失敗しました";
        }

        File[] zipListFiles = tmpFile.listFiles();

        if (zipListFiles != null) {
            int c = 0;

            /* ディレクトリのなかのファイルを取得 */
            for (int i = 0; i < zipListFiles.length; i++) {
                if (zipListFiles[i].isDirectory()) {
                    c++;
                } else {
                    onProgressUpdate(listener, context.getString(R.string.progress_state_check_file));
                    zipFile = zipListFiles[i].getName();

                    /* apkファイルならパスをインストールデータへ */
                    if (zipFile.substring(zipFile.lastIndexOf(".")).equalsIgnoreCase(".apk")) {
                        splitInstallData[i - c] = zipListFiles[i].getPath();
                    } else {
                        /* apkファイルでなかったときのリストの順番を修正 */
                        c++;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public interface Listener {

        void onShow();

        void onSuccess();

        void onFailure();

        void onError(String message);

        void onProgressUpdate(String message);
    }

    public int getLoadedBytePercent(Context context) {
        if (totalByte <= 0) {
            return 0;
        }

        return (int) Math.floor(((double) getLoadedCurrentByte(context) / getLoadedTotalByte()) * 100);
    }

    public int getLoadedTotalByte() {
        return (int) totalByte / (1024 * 1024);
    }

    public int getLoadedCurrentByte(Context context) {
        if (totalByte <= 0) {
            return 0;
        }

        return (int) Common.getFileSize(new File(Common.getTemporaryPath(context))) / (1024 * 1024);
    }

    public boolean isFinish() {
        return totalByte == -1;
    }
}
