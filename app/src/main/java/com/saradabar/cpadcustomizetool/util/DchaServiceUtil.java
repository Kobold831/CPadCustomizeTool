package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.task.IDchaTask;

/** @noinspection unused*/
public class DchaServiceUtil {

    Context mContext;

    public DchaServiceUtil(Context context) {
        mContext = context;
    }

    public interface Listener {
        void onResult(Object object);
    }

    public void cancelSetup(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.cancelSetup();
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void checkPadRooted(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.checkPadRooted());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void clearDefaultPreferredApp(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.clearDefaultPreferredApp(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void copyFile(String srcFilePath, String dstFilePath, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.copyFile(srcFilePath, dstFilePath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void copyUpdateImage(String srcFilePath, String dstFilePath, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.copyUpdateImage(srcFilePath, dstFilePath.startsWith("/cache") ? dstFilePath : "/cache/../" + dstFilePath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void deleteFile(String path, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.deleteFile(path));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void disableADB(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.disableADB();
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getCanonicalExternalPath(String linkPath, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.getCanonicalExternalPath(linkPath));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getForegroundPackageName(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.getForegroundPackageName());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void getSetupStatus(Listener listener) {
        if (Common.isBenesseExtensionExist("getDchaState")) {
            listener.onResult(BenesseExtension.getDchaState());
        } else {
            new IDchaTask().execute(mContext, iDchaService -> {
                if (iDchaService == null) {
                    listener.onResult(false);
                    return;
                }

                try {
                    listener.onResult(iDchaService.getSetupStatus());
                } catch (RemoteException e) {
                    listener.onResult(e.getMessage());
                }
            });
        }
    }

    public void getUserCount(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.getUserCount());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void hideNavigationBar(boolean hide, Listener listener) {
        if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
            new IDchaTask().execute(mContext, iDchaService -> {
                if (iDchaService == null) {
                    listener.onResult(false);
                    return;
                }

                try {
                    iDchaService.hideNavigationBar(hide);
                    listener.onResult(true);
                } catch (RemoteException e) {
                    listener.onResult(e.getMessage());
                }
            });
        } else {
            Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, hide ? 1 : 0);
        }
    }

    public void installApp(String path, int installFlag, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.installApp(path, installFlag));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void isDeviceEncryptionEnabled(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.isDeviceEncryptionEnabled());
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void rebootPad(int rebootMode, String srcFile, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.rebootPad(rebootMode, srcFile);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void removeTask(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.removeTask(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void sdUnmount(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.sdUnmount();
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setDefaultParam(Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.setDefaultParam();
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setDefaultPreferredHomeApp(String packageName, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.setDefaultPreferredHomeApp(packageName);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setPermissionEnforced(boolean enforced, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.setPermissionEnforced(enforced);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setSetupStatus(int status, Listener listener) {
        // BenesseExtensionが存在してかつCT3ではないか
        if (Common.isBenesseExtensionExist("setDchaState") && !Common.isCT3()) {
            BenesseExtension.setDchaState(status);
            listener.onResult(true);
            return;
        }

        // BenesseExtensionが存在しないまたはCT3
        // Dchaを使うかどうか
        if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
            new IDchaTask().execute(mContext, iDchaService -> {
                if (iDchaService == null) {
                    listener.onResult(false);
                    return;
                }

                try {
                    iDchaService.setSetupStatus(status);
                    listener.onResult(true);
                } catch (RemoteException e) {
                    listener.onResult(e.getMessage());
                }
            });
        } else {
            Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, status);
            listener.onResult(true);
        }
    }

    public void setSystemTime(String time, String timeFormat, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                iDchaService.setSystemTime(time, timeFormat);
                listener.onResult(true);
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void uninstallApp(String packageName, int uninstallFlag, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.uninstallApp(packageName, uninstallFlag));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void verifyUpdateImage(String updateFile, Listener listener) {
        new IDchaTask().execute(mContext, iDchaService -> {
            if (iDchaService == null) {
                listener.onResult(false);
                return;
            }

            try {
                listener.onResult(iDchaService.verifyUpdateImage(updateFile));
            } catch (RemoteException e) {
                listener.onResult(e.getMessage());
            }
        });
    }

    public void setPreferredHomeApp(String s, String s1, Listener listener) {
        clearDefaultPreferredApp(s, object -> {
            if (object.equals(true)) {
                setDefaultPreferredHomeApp(s1, object1 -> {
                    if (object1.equals(true)) {
                        listener.onResult(true);
                    } else {
                        listener.onResult(false);
                    }
                });
            } else {
                listener.onResult(false);
            }
        });
    }

    public void execSystemUpdate(String s, int i, Listener listener) {
        String updateFile = "/cache/update.zip";

        copyUpdateImage(s, updateFile, object -> {
            if (object.equals(true)) {
                rebootPad(i, updateFile, object1 -> {
                    if (object1.equals(true)) {
                        listener.onResult(true);
                    } else {
                        listener.onResult(false);
                    }
                });
            } else {
                listener.onResult(false);
            }
        });
    }
}
