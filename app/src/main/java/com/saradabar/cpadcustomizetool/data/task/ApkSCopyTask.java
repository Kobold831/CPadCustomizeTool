package com.saradabar.cpadcustomizetool.data.task;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class ApkSCopyTask {

    long totalByte = 0;

    public void execute(Context context, Listener listener, ArrayList<String> splitInstallData) {
        onPreExecute(listener);
        Executors.newSingleThreadExecutor().submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Object result = doInBackground(context, listener, splitInstallData);
                handler.post(() -> onPostExecute(context, listener, result));
            }).start();
        });
    }

    void onPreExecute(Listener listener) {
        listener.onShow();
    }

    void onPostExecute(Context context, Listener listener, Object result) {
        if (result == null) {
            totalByte = -1;
            listener.onError(context.getString(R.string.installer_status_unknown_error));
            return;
        }

        if (result.getClass() == ArrayList.class) {
            ArrayList<String> stringArrayList = new ArrayList<>();

            for (Object o : (Iterable<?>) result) {
                stringArrayList.add((String) o);
            }

            totalByte = -1;
            listener.onSuccess(stringArrayList);
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

    protected Object doInBackground(Context context, Listener listener, ArrayList<String> splitInstallData) {
        //noinspection SequencedCollectionMethodCanBeUsed
        String zipFile = splitInstallData.get(0);
        // .apks を展開した splits の中が .apk 群
        File tmpFile = new File(Common.getTemporaryPath(context) + "/splits");
        totalByte = new File(zipFile).length();

        // apksのファイルパスを削除
        //noinspection SequencedCollectionMethodCanBeUsed
        splitInstallData.remove(0);

        /* zipを展開して外部ディレクトリに一時保存 */
        onProgressUpdate(listener, context.getString(R.string.progress_state_unpack));

        try {
            ZipUtil.unpack(new File(zipFile), tmpFile);
        } catch (Exception e) {
            return e.getMessage();
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
                    // TODO: master_2.apk は無視
                    if (zipFile.substring(zipFile.lastIndexOf(".")).equalsIgnoreCase(".apk")
                            && !zipFile.endsWith("master_2.apk")) {
                        splitInstallData.add(i - c, zipListFiles[i].getPath());
                    } else {
                        /* apkファイルでなかったときのリストの順番を修正 */
                        c++;
                    }
                }
            }
            return splitInstallData;
        } else {
            return false;
        }
    }

    public interface Listener {
        void onShow();

        void onSuccess(ArrayList<String> splitInstallData);

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

    @TargetApi(Build.VERSION_CODES.O)
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
