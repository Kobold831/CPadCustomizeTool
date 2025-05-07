package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.RemoteException;

import com.saradabar.cpadcustomizetool.data.task.IDchaUtilTask;

/** @noinspection unused*/
public class DchaUtilServiceUtil {

    Context mContext;

    public DchaUtilServiceUtil(Context context) {
        mContext = context;
    }

    public interface Listener {
        void onResult(Object object);
    }

    public void clearDefaultPreferredApp(String packageName, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaUtilService.clearDefaultPreferredApp(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void copyFile(String srcFilePath, String dstFilePath, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.copyFile(srcFilePath, dstFilePath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void copyDirectory(String srcFilePath, String dstFilePath, boolean makeTopDir, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.copyDirectory(srcFilePath, dstFilePath, makeTopDir));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void deleteFile(String path, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.deleteFile(path));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void existsFile(String path, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.existsFile(path));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getCanonicalExternalPath(String linkPath, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.getCanonicalExternalPath(linkPath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getDisplaySize(Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.getDisplaySize());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getLcdSize(Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.getLcdSize());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getUserCount(Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.getUserCount());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void hideNavigationBar(boolean hide, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaUtilService.hideNavigationBar(hide);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void listFiles(String path, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.listFiles(path));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void makeDir(String path, String dirname, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.makeDir(path, dirname));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void sdUnmount(Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaUtilService.sdUnmount();
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setForcedDisplaySize(int width, int height, Listener listener) {
        new IDchaUtilTask().execute(mContext, iDchaUtilService -> {
            if (iDchaUtilService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaUtilService.setForcedDisplaySize(width, height));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }
}
