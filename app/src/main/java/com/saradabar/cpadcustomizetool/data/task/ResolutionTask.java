package com.saradabar.cpadcustomizetool.data.task;

import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.Display;
import android.view.IWindowManager;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

public class ResolutionTask {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void execute(Listener listener, int width, int height) {
        AppExecutors.io(() -> doInBackground(listener, width, height));
    }

    private void onPostExecute(Listener listener, Object result) {
        MAIN.postDelayed(() -> {
            if (result.equals(true)) {
                listener.onSuccess();
                return;
            }

            if (result.equals(false)) {
                listener.onFailure();
                return;
            }
            listener.onError(result.toString());
        }, Constants.RESOLUTION_APPLY_DELAY_MS);
    }

    private void doInBackground(Listener listener, int width, int height) {
        if (Common.isBenesseExtensionExist("putInt")) {
            if (width == Constants.XGA_WIDTH && height == Constants.XGA_HEIGHT ||
                    width == Constants.WXGA_WIDTH && height == Constants.WXGA_HEIGHT ||
                    width == Constants.WUXGA_WIDTH && height == Constants.WUXGA_HEIGHT) {
                onPostExecute(listener,
                        BenesseExtension.putInt(Constants.BC_COMPATSCREEN, width == Constants.XGA_WIDTH ? Constants.XGA : width == Constants.WXGA_WIDTH ? Constants.WXGA : Constants.WUXGA));
            } else {
                try {
                    IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setForcedDisplaySize(Display.DEFAULT_DISPLAY, width, height);
                    onPostExecute(listener, true);
                } catch (RemoteException e) {
                    onPostExecute(listener, e.getMessage());
                }
            }
        } else {
            try {
                onPostExecute(listener,
                    BenesseExtension.setForcedDisplaySize(width, height));
            } catch (SecurityException e) {
                onPostExecute(listener, e.getMessage());
            }
        }
    }

    public interface Listener {
        void onSuccess();

        void onFailure();

        void onError(String message);
    }
}
