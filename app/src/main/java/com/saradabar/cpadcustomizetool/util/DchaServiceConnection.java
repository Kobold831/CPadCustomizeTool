/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.data.task.AppExecutors;

import java.util.ArrayDeque;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public final class DchaServiceConnection {

    public interface Callback {
        void onService(@Nullable IDchaService service);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final ArrayDeque<Callback> PENDING = new ArrayDeque<>();

    private static final long CONNECT_TIMEOUT_MS = 500;

    private static volatile IDchaService sService;
    private static volatile boolean sBinding;
    private static volatile boolean sBindingFailed;
    private static Context sContext;

    private DchaServiceConnection() {
    }

    public static boolean ensureBound(@NonNull Context context) {
        if (sService != null) {
            return true;
        }

        synchronized (LOCK) {
            if (sService != null) {
                return true;
            }

            if (sContext == null) {
                sContext = context.getApplicationContext();
            }

            if (sBinding) {
                return true;
            }

            sBinding = true;
            sBindingFailed = false;

            boolean bound;

            try {
                bound = sContext.bindService(new Intent(Constants.ACTION_DCHA_SERVICE), CONNECTION, Context.BIND_AUTO_CREATE);
            } catch (RuntimeException ignored) {
                bound = false;
            }

            if (!bound) {
                sBinding = false;
                sBindingFailed = true;
                MAIN.post(DchaServiceConnection::flushFailure);
            }
            return bound;
        }
    }

    public static void execute(@NonNull Context context, @NonNull Callback callback) {
        IDchaService service = sService;

        if (service != null) {
            AppExecutors.io(() -> callback.onService(service));
            return;
        }

        ensureBound(context);
        MAIN.post(() -> dispatch(callback));
    }

    public static void invalidate() {
        synchronized (LOCK) {
            sService = null;
            releaseBinding();
        }
        MAIN.post(DchaServiceConnection::flushFailure);
    }

    private static void dispatch(@NonNull Callback callback) {
        IDchaService service = sService;

        if (service != null) {
            AppExecutors.io(() -> callback.onService(service));
        } else if (sBindingFailed) {
            AppExecutors.io(() -> callback.onService(null));
        } else {
            PENDING.add(callback);
            MAIN.postDelayed(() -> {
                if (PENDING.remove(callback)) {
                    if (sService == null) {
                        invalidate();
                    }
                    AppExecutors.io(() -> callback.onService(null));
                }
            }, CONNECT_TIMEOUT_MS);
        }
    }

    private static void flushFailure() {
        while (!PENDING.isEmpty()) {
            Callback callback = PENDING.poll();

            if (callback != null) {
                AppExecutors.io(() -> callback.onService(null));
            }
        }
    }

    private static void releaseBinding() {
        sBinding = false;
        sBindingFailed = true;
        Context context = sContext;

        if (context != null) {
            try {
                context.unbindService(CONNECTION);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static final ServiceConnection CONNECTION = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!sBinding) {
                return;
            }

            IDchaService service = IDchaService.Stub.asInterface(binder);
            sService = service;
            sBinding = true;
            sBindingFailed = false;

            while (!PENDING.isEmpty()) {
                Callback callback = PENDING.poll();

                if (callback != null) {
                    AppExecutors.io(() -> callback.onService(service));
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sService = null;
            releaseBinding();
            flushFailure();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            sService = null;
            releaseBinding();
            flushFailure();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            sService = null;
            releaseBinding();
            flushFailure();
        }
    };
}
