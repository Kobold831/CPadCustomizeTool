package com.saradabar.cpadcustomizetool.data.task;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Path;

import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XApkCopyTask {

    public static XApkCopyTask xApkCopyTask;
    long totalByte = 0;
    String obbPath1, obbPath2;

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
        xApkCopyTask = this;
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

        /* 拡張子.xapkを.zipに変更 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_rename));

        if (!new File(splitInstallData[0]).renameTo(new File(zipFile))) {
            return "拡張子の変更に失敗しました";
        }

        File tmpFile = new File(Path.getTemporaryPath(context));

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

        /* 拡張子.zipを.xapkに変更 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_rename));

        if (!new File(zipFile).renameTo(new File(new File(zipFile).getParent() + File.separator + new File(zipFile).getName().replaceFirst("\\..*", ".xapk")))) {
            return "拡張子の変更に失敗しました";
        }

        File[] zipListFiles = tmpFile.listFiles();

        if (zipListFiles != null) {
            int c = 0;

            /* ディレクトリのなかのファイルを取得 */
            for (int i = 0; i < zipListFiles.length; i++) {
                /* obbデータを取得 */
                if (zipListFiles[i].isDirectory()) {
                    c++;

                    try {
                        /* obbデータをコピー */
                        onProgressUpdate(listener, context.getString(R.string.progress_state_copy_file));
                        File[] obbName = new File(zipListFiles[i].getPath() + "/obb").listFiles();
                        File[] obbFile = obbName != null ? obbName[0].listFiles() : new File[0];
                        totalByte = obbFile != null ? obbFile[0].length() : 0;
                        obbPath1 = obbName[0].getName();
                        obbPath2 = obbFile != null ? obbFile[0].getName() : null;
                        FileUtils.copyDirectory(new File(zipListFiles[i].getPath() + "/obb/"), new File(Environment.getExternalStorageDirectory() + "/Android/obb"));
                    } catch (Exception e) {
                        return context.getString(R.string.installer_status_no_allocatable_space) + e.getMessage();
                    }
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

    @TargetApi(Build.VERSION_CODES.O)
    public int getLoadedBytePercent(Context context) {
        if (totalByte <= 0) return 0;
        return (int) Math.floor(((double) getLoadedCurrentByte(context) / getLoadedTotalByte()) * 100);
    }

    public int getLoadedTotalByte() {
        return (int) totalByte / (1024 * 1024);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public int getLoadedCurrentByte(Context context) {
        if (totalByte <= 0) return 0;

        if (obbPath1 == null) {
            return (int) Common.getDirectorySize(new File(Path.getTemporaryPath(context))) / (1024 * 1024);
        } else {
            try {
                return (int) Files.size(Paths.get(Environment.getExternalStorageDirectory() + "/Android/obb/" + obbPath1 + "/" + obbPath2)) / (1024 * 1024);
            } catch (IOException ignored) {
                return 0;
            }
        }
    }

    public boolean isFinish() {
        return totalByte == -1;
    }
}
