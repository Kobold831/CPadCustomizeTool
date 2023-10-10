package com.saradabar.cpadcustomizetool.data.service;

import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

import com.aurora.store.data.service.IInstallResult;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DeviceOwnerService extends Service {

    protected IDeviceOwnerService.Stub mDeviceOwnerServiceStub = new IDeviceOwnerService.Stub() {

        @Override
        public boolean isDeviceOwnerApp() {
            DevicePolicyManager dPM = (DevicePolicyManager) getBaseContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
            try {
                return dPM.isDeviceOwnerApp(getPackageName());
            } catch (SecurityException ignored) {
                return false;
            }
        }

        @Override
        public void setUninstallBlocked(String str, boolean bl) {
            DevicePolicyManager dPM = (DevicePolicyManager) getBaseContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
            dPM.setUninstallBlocked(new ComponentName(getApplicationContext(), AdministratorReceiver.class), str, bl);
        }

        @Override
        public boolean isUninstallBlocked(String str) {
            DevicePolicyManager dPM = (DevicePolicyManager) getBaseContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dPM.isUninstallBlocked(new ComponentName(getApplicationContext(), AdministratorReceiver.class), str);
        }

        @Override
        public boolean installPackages(String str, List<Uri> uriList) {
            int sessionId;
            try {
                sessionId = createSession(getPackageManager().getPackageInstaller());
                if (sessionId < 0) {
                    getPackageManager().getPackageInstaller().abandonSession(sessionId);
                    return false;
                }
            } catch (IOException ignored) {
                return false;
            }
            for (Uri uri : uriList) {
                try {
                    if (!writeSession(getPackageManager().getPackageInstaller(), sessionId, new File(Environment.getExternalStorageDirectory() + uri.getPath().replace("/external_files", "")))) {
                        getPackageManager().getPackageInstaller().abandonSession(sessionId);
                        return false;
                    }
                } catch (IOException ignored) {
                    getPackageManager().getPackageInstaller().abandonSession(sessionId);
                    return false;
                }
            }
            try {
                if (commitSession(getPackageManager().getPackageInstaller(), sessionId, getBaseContext())) {
                    return true;
                } else {
                    getPackageManager().getPackageInstaller().abandonSession(sessionId);
                    return false;
                }
            } catch (IOException ignored) {
                getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mDeviceOwnerServiceStub;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) postStatus(intent.getIntExtra("REQUEST_SESSION", -1), intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1), intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME), intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        return super.onStartCommand(intent, flags, startId);
    }

    private void postStatus(int sessionId, int status, String packageName, String extra) {
        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).close();
                } catch (Exception ignored) {
                }
                bindInstallResult(0, packageName, null, null);
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).abandon();
                } catch (Exception ignored) {
                }
                bindInstallResult(1, packageName, getErrorMessage(this, status), null);
                break;
            default:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).abandon();
                } catch (Exception ignored) {
                }
                bindInstallResult(2, packageName, getErrorMessage(this, status), extra);
                break;
        }
    }

    private void bindInstallResult(int flag, String packageName, String errorString, String extra) {
        bindService(Constants.AURORA_SERVICE, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IInstallResult mIInstallResult = IInstallResult.Stub.asInterface(iBinder);
                try {
                    switch (flag) {
                        case 0:
                            mIInstallResult.InstallSuccess(packageName);
                            unbindService(this);
                            break;
                        case 1:
                            mIInstallResult.InstallFailure(packageName, errorString);
                            unbindService(this);
                            break;
                        case 2:
                            mIInstallResult.InstallError(packageName, errorString, extra);
                            unbindService(this);
                            break;
                    }
                } catch (RemoteException ignored) {
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                unbindService(this);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private String getErrorMessage(Context context, int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return context.getString(R.string.installer_status_user_action);
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return context.getString(R.string.installer_status_failure_blocked);
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return context.getString(R.string.installer_status_failure_conflict);
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return context.getString(R.string.installer_status_failure_incompatible);
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return context.getString(R.string.installer_status_failure_invalid);
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return context.getString(R.string.installer_status_failure_storage);
            default:
                return context.getString(R.string.installer_status_failure);
        }
    }

    public static int createSession(PackageInstaller packageInstaller) throws IOException {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY);
        return packageInstaller.createSession(params);
    }

    public static boolean writeSession(PackageInstaller packageInstaller, int sessionId, File apkFile) throws IOException {
        long sizeBytes = -1;
        String apkPath = apkFile.getAbsolutePath();

        File file = new File(apkPath);
        if (file.isFile()) {
            sizeBytes = file.length();
        }

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            session = packageInstaller.openSession(sessionId);
            in = new FileInputStream(apkPath);
            out = session.openWrite(getRandomString(), 0, sizeBytes);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    public static boolean commitSession(PackageInstaller packageInstaller, int sessionId, Context context) throws IOException {
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);
            Intent intent = new Intent(context, DeviceOwnerService.class).putExtra("REQUEST_SESSION", sessionId);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );
            session.commit(pendingIntent.getIntentSender());
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }

    public static String getRandomString() {
        String theAlphaNumericS;
        StringBuilder builder;
        theAlphaNumericS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        builder = new StringBuilder(5);
        for (int m = 0; m < 5; m++) {
            int myindex = (int) (theAlphaNumericS.length() * Math.random());
            builder.append(theAlphaNumericS.charAt(myindex));
        }
        return builder.toString();
    }
}