package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.task.IDchaTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.benesse.dcha.dchaservice.IDchaService;

/** @noinspection unused*/
public class DchaServiceUtil {

    final Object objLock = new Object();

    Context mContext;
    IDchaService mDchaService;

    public DchaServiceUtil(Context context) {
        mContext = context;
    }

    public void cancelSetup() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.cancelSetup();
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public boolean checkPadRooted() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.checkPadRooted();
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean clearDefaultPreferredApp(String packageName) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                mDchaService.clearDefaultPreferredApp(packageName);
                return true;
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean copyFile(String srcFilePath, String dstFilePath) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.copyFile(srcFilePath, dstFilePath);
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean copyUpdateImage(String srcFilePath, String dstFilePath) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.copyFile(srcFilePath, dstFilePath.startsWith("/cache") ? dstFilePath : "/cache/../" + dstFilePath);
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean deleteFile(String path) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.deleteFile(path);
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void disableADB() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.disableADB();
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
            Settings.Secure.putInt(mContext.getContentResolver(), "adb_enabled", 0);
        }
    }

    public String getCanonicalExternalPath(String linkPath) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.getCanonicalExternalPath(linkPath);
            }).get();
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getForegroundPackageName() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.getForegroundPackageName();
            }).get();
        } catch (Exception ignored) {
            return null;
        }
    }

    public int getSetupStatus() {
        if (Common.isBenesseExtensionExist()) {
            return BenesseExtension.getDchaState();
        } else {
            try {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                return executorService.submit(() -> {
                    new IDchaTask().execute(mContext, iDchaService -> {
                        mDchaService = iDchaService;
                        synchronized (objLock) {
                            objLock.notify();
                        }
                    });
                    synchronized (objLock) {
                        objLock.wait();
                    }
                    return mDchaService.getSetupStatus();
                }).get();
            } catch (Exception ignored) {
                return -1;
            }
        }
    }

    public int getUserCount() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.getUserCount();
            }).get();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public void hideNavigationBar(boolean hide) {
        try {
            if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(() -> {
                    new IDchaTask().execute(mContext, iDchaService -> {
                        mDchaService = iDchaService;
                        synchronized (objLock) {
                            objLock.notify();
                        }
                    });
                    synchronized (objLock) {
                        try {
                            objLock.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                    try {
                        mDchaService.hideNavigationBar(hide);
                    } catch (RemoteException ignored) {
                    }
                });
            } else {
                Settings.System.putInt(mContext.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, hide ? 1 : 0);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean installApp(String path, int installFlag) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.installApp(path, installFlag);
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isDeviceEncryptionEnabled() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.isDeviceEncryptionEnabled();
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean rebootPad(int rebootMode, String srcFile) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                mDchaService.rebootPad(rebootMode, srcFile);
                return true;
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void removeTask(String packageName) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.removeTask(packageName);
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public void sdUnmount() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.sdUnmount();
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public boolean setDefaultParam() {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                mDchaService.setDefaultParam();
                return true;
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean setDefaultPreferredHomeApp(String packageName) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                mDchaService.setDefaultPreferredHomeApp(packageName);
                return true;
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void setPermissionEnforced(boolean enforced) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.setPermissionEnforced(enforced);
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public void setSetupStatus(int status) {
        try {
            // BenesseExtensionが存在してかつCT3ではないか
            if (Common.isBenesseExtensionExist() &&
                    Preferences.load(mContext, Constants.KEY_INT_MODEL_NUMBER, Constants.DEF_INT) != Constants.MODEL_CT3) {
                BenesseExtension.setDchaState(status);
            } else {
                // BenesseExtensionが存在しないまたはCT3
                // Dchaを使うかどうか
                if (Preferences.load(mContext, Constants.KEY_FLAG_APP_SETTING_DCHA, false)) {
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.submit(() -> {
                        new IDchaTask().execute(mContext, iDchaService -> {
                            mDchaService = iDchaService;
                            synchronized (objLock) {
                                objLock.notify();
                            }
                        });
                        synchronized (objLock) {
                            try {
                                objLock.wait();
                            } catch (InterruptedException ignored) {
                            }
                        }
                        try {
                            mDchaService.setSetupStatus(status);
                        } catch (RemoteException ignored) {
                        }
                    });
                } else {
                    Settings.System.putInt(mContext.getContentResolver(), Constants.DCHA_STATE, status);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void setSystemTime(String time, String timeFormat) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    try {
                        objLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    mDchaService.setSystemTime(time, timeFormat);
                } catch (RemoteException ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public boolean uninstallApp(String packageName, int uninstallFlag) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.uninstallApp(packageName, uninstallFlag);
            }).get();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean verifyUpdateImage(String updateFile) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(() -> {
                new IDchaTask().execute(mContext, iDchaService -> {
                    mDchaService = iDchaService;
                    synchronized (objLock) {
                        objLock.notify();
                    }
                });
                synchronized (objLock) {
                    objLock.wait();
                }
                return mDchaService.verifyUpdateImage(updateFile);
            }).get();
        } catch (Exception ignored) {
            return true;
        }
    }
}
